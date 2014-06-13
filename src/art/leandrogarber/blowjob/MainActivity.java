package art.leandrogarber.blowjob;

import java.util.Timer;
import java.util.TimerTask;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

public class MainActivity extends Activity {
	
	MediaPlayer oMediaPlayer;
	MediaPlayer oAudioPlayer;
	private final long VIDEO_RESET_TIME = 1000 * 60 * 40; //40 minutos
	
	VideoView oVideo;
	View backgroundView;
	View overlayView;
	
	private final String TCP_IP = "192.168.2.150";
	private final int TCP_PORT = 3399;
	private final String TCP_MESSAGE_OFF = "0";
	private final String TCP_MESSAGE_ON = "1";
	private final String TCP_MESSAGE_STATUS = "2";
	
	private ClientTCP clientTCP;
	private Handler clientTCPHandler;
	
	private boolean magicIsOn;
	private boolean magicIsGoingOff;
	private WakeLock wakeLock;
	
	private ObjectAnimator animatorFadeIn;
	private ObjectAnimator animatorFadeOut;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		hideUI();
		setContentView(R.layout.activity_main);
		
		setupNetwork();
		setupMedia();
		disablePowerButton();
	}
	
	private void setupNetwork()
	{
		Log.d("NETWORK", "My IP is: " + Utils.getIPAddress(true));
		final MainActivity $this = this;
		
		clientTCPHandler = new Handler() {
			
			@Override 
			public void handleMessage(Message msg) {
				String text = (String)msg.obj;
				
				if ( text.equals(TCP_MESSAGE_STATUS) )
					sendStatusMessage();
			}
		};
		
		clientTCP = new ClientTCP(TCP_IP, TCP_PORT, clientTCPHandler);
		new Thread(clientTCP).start();
		
	}
	
	private void sendStatusMessage() {
		Intent batteryStatus = registerReceiver(null,  new IntentFilter(Intent.ACTION_BATTERY_CHANGED) );
		int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		
		boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
							 status == BatteryManager.BATTERY_STATUS_FULL;
		
		
		int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		
		int batteryLevel = (int) Math.floor( (level / (float)scale) * 10 );
		
		clientTCP.sendMessage("Soy: " + Utils.getMacAddress(this) + " " + String.valueOf(isCharging) + " | " + batteryLevel );
	}
	
	/**
	 * Prepara el video. Lo resetea cada VIDEO_RESET_TIME milisegundos para que no se tilde el loop
	 * 
	 */
	private void setupVideo()
	{
		final Activity activity = this;
		oVideo = (VideoView)findViewById(R.id.videoView1);
		oVideo.setOnPreparedListener(new OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				Log.d("BLOWJOB", "Video prepared...");
				mp.setLooping(true);
				oMediaPlayer = mp;
			} 
		});
		
		Timer timerResetVideo = new Timer();
		timerResetVideo.schedule(new TimerTask() {
			
			@Override
			public void run() {
				
				activity.runOnUiThread(new Runnable() {				
					@Override
					public void run() {
						if ( oMediaPlayer != null )
							oMediaPlayer.reset();
						
						Log.d("BLOWJOB", "Reseting video...");
						oVideo.setVideoURI( Uri.parse("android.resource://art.leandrogarber.blowjob/" + R.raw.loop) );
						oVideo.start();
					}
				});
			}
		}, 0, VIDEO_RESET_TIME);
	}
	
	private void setupMedia()
	{
		setupVideo();
		
		try {
			oAudioPlayer = new MediaPlayer();
			oAudioPlayer.setDataSource(this, Uri.parse("android.resource://art.leandrogarber.blowjob/" + R.raw.audio));
			oAudioPlayer.setLooping(true);
			oAudioPlayer.prepare();
		} catch (Exception e) {
			//Log.e("AUDIO", "Audio error " + e.getMessage() ) ;
		}
		

		backgroundView = (View)findViewById(R.id.background);
		overlayView = (View)findViewById(R.id.overlay);
		
		backgroundView.setOnTouchListener( new OnTouchListener() {		
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
				if ( event.getAction() == MotionEvent.ACTION_UP )
					stopTheMagic();
				else if ( event.getAction() == MotionEvent.ACTION_MOVE )
					startTheMagic();
				
				return true;
			}
		});
		
		overlayView.setVisibility(View.VISIBLE);
		
		animatorFadeIn = new ObjectAnimator();
		animatorFadeOut = new ObjectAnimator();
		
		animatorFadeIn.setTarget( overlayView );
		animatorFadeIn.setPropertyName( "alpha" );
		animatorFadeIn.setDuration(4000);
		animatorFadeIn.setFloatValues(1);
		
		animatorFadeIn.addListener(new AnimatorListener() {
			
			@Override
			public void onAnimationStart(Animator arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationRepeat(Animator arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd(Animator arg0) {
				// TODO Auto-generated method stub
				Log.d("BLOWJOB", "FadeIn End");
				if ( magicIsGoingOff )
					reallyStopTheMagic();
			}
			
			@Override
			public void onAnimationCancel(Animator arg0) {
				Log.d("BLOWJOB", "FadeIn Cancel");
				// TODO Auto-generated method stub
				
			}
		});
		
		
		animatorFadeOut.setTarget( overlayView );
		animatorFadeOut.setPropertyName( "alpha" );
		animatorFadeOut.setDuration(400);
		animatorFadeOut.setFloatValues(0);
	}
	
	private void startTheMagic()
	{
		if ( !magicIsOn || magicIsGoingOff )
		{
			magicIsGoingOff = false;
			animatorFadeIn.cancel();
			animatorFadeOut.setFloatValues(overlayView.getAlpha(),  0);
			animatorFadeOut.start();
			
			
			if ( !magicIsOn ) 
			{
				sendTCPToAll( String.valueOf(TCP_MESSAGE_ON) );
				oAudioPlayer.seekTo(0);
				oAudioPlayer.start();
				
				magicIsOn = true;
			}
		}
	}
	
	private void stopTheMagic()
	{
		magicIsGoingOff = true;
		animatorFadeIn.start();
	}
	
	private void reallyStopTheMagic()
	{
		sendTCPToAll( String.valueOf(TCP_MESSAGE_OFF) );
		oAudioPlayer.pause();
		
		magicIsGoingOff = false;
		magicIsOn = false;
	}
	
	private void sendTCPToAll( String message )
	{
		clientTCP.sendMessage(message);
	}
	
	private void disablePowerButton()
	{
		PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "BLOWJOB");
        
        KeyguardManager mKeyGuardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE); 
        KeyguardLock mLock = mKeyGuardManager.newKeyguardLock("MainActivity"); 
        mLock.disableKeyguard();
	}
	
	private void hideUI()
	{
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        try{
            //REQUIRES ROOT
            String ProcID = "79"; //HONEYCOMB AND OLDER

            Process proc = Runtime.getRuntime().exec(new String[]{"su","-c","service call activity "+ ProcID +" s16 com.android.systemui"}); //WAS 79
            proc.waitFor();

        }catch(Exception ex){
            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
	}
	
	private void showUI()
	{
		try{
            //REQUIRES ROOT
            String ProcID = "79"; //HONEYCOMB AND OLDER
            
            Process proc = Runtime.getRuntime().exec(new String[]{"am","startservice","-n","com.android.systemui/.SystemUIService"});
            proc.waitFor();

        }catch(Exception ex){
            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            return true;
        }
        else if ((keyCode == KeyEvent.KEYCODE_CALL)) {
            return true;
        }
        else if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            return true;
        }
        else if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
	
	@Override
	protected void onResume ()
	{
		Log.d("BLOWJOB", "Resuming app...");
		wakeLock.acquire();
		super.onResume();
	}
	
	@Override
	protected void onPause ()
	{
		Log.d("BLOWJOB", "Pausing app...");
		wakeLock.release();
		super.onPause();
	}
	
	@Override
	protected void onStop ()
	{
		Log.d("BLOWJOB", "Stoping app...");
		//showUI();
		super.onStop();
	}
	
	@Override
	protected void onDestroy()
	{
		Log.d("BLOWJOB", "Destroying app...");
		//showUI();
		super.onDestroy();
	}
	
	//Asi no se puede apagar jamás
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	    super.onWindowFocusChanged(hasFocus);
	    if(!hasFocus) {
	       Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
	        sendBroadcast(closeDialog);
	    }
	}
}
