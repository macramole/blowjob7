package art.leandrogarber.blowjob;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ClientTCP implements Runnable {
	private String ip;
	private int port;
	
	private Socket socket;
	private String message;
	private Handler handler;
	
	private BufferedReader input;
	private BufferedWriter output;
	
	private Timer keepAliveTimer;
	private final String MESSAGE_PING = "Ping?";
	private final String MESSAGE_PONG = "Pong!";
	private final boolean PING_PONG_ENABLED = true;
	private final long PING_PONG_TIME = 10000;
	
	public boolean isConnected;
	
	
	
	ClientTCP(String ip, int port, Handler handler)
	{
		this.ip = ip;
		this.port = port;
		this.handler = handler;
		
		isConnected = false;
	}
	
	public void sendMessage( String message )
	{
		this.message = message;  
		
	}
	
	private void createSocket()
	{
		do
		{
			try {
				InetAddress serverAddr = InetAddress.getByName(ip);
				socket = new Socket(serverAddr, port);
				
				Log.d("NETWORK", "SOCKET CREATED SUCCESSFULLY" + " FOR IP:" + ip);
				isConnected = true;
				
			} catch (ConnectException e) {
				Log.e("NETWORK", "CREATE SOCKET ERROR" + " WITH IP:" + ip + " - " + e.getMessage() + "(" + e.getClass() +  ")" );
				e.printStackTrace();
				
				Log.d("NETWORK", "RETRY SOCKET CREATION IN 2 SECONDS" + " WITH IP:" + ip);
				
				synchronized (this) {
					try {
						wait(2000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				
			} catch (Exception e) {
				Log.e("NETWORK", "CREATE SOCKET ERROR" + " WITH IP:" + ip + " - " + e.getMessage() + "(" + e.getClass() +  ")" );
				e.printStackTrace();
			}
			
			try {
				if ( isConnected )
				{
					this.input = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8") ));
					this.output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8"))); // , Charset.forName("UTF-8") US-ASCII
					
					if ( PING_PONG_ENABLED && keepAliveTimer == null )
					{
						final ClientTCP $this = this;
						
						keepAliveTimer = new Timer();
						keepAliveTimer.scheduleAtFixedRate(new TimerTask() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								if ( $this.isConnected )
									$this.sendMessage(MESSAGE_PING);
							}
						}, PING_PONG_TIME, PING_PONG_TIME);
					}
				}
			} catch (Exception e) {
				Log.e("NETWORK", "ERROR CONFIGURING SOCKET" + " WITH IP:" + ip + " - " + e.getMessage() + "(" + e.getClass() +  ")");
				e.printStackTrace();
			}
		} while ( socket == null );
		
	}
	
	private void handleInput()
	{
		try {
			if ( input.ready() )
			{
				String read = input.readLine();// + System.getProperty("line.separator");
				
				Log.d("NETWORK", "READ: " + read);
				
				if ( !read.equals(MESSAGE_PONG) )
				{
					Message msg = new Message();
					msg.obj = read;
					
					handler.sendMessage(msg);
				}
			}
			
		} catch (Exception e) {
			Log.e("NETWORK", "READ ERROR" + " - " + e.getMessage() + "(" + e.getClass() +  ")" );
		}
	}
	
	private boolean handleOutput()
	{
		if ( message != null )
		{
			try
			{
				output.write( (message + "\r\n" ) );
				output.flush();
				Log.d("NETWORK", "SENT: " + message + " TO IP:" + socket.getInetAddress().toString());
				message = null;
			} catch (SocketException e) {
				Log.e("NETWORK", "SENT ERROR" + " TO IP:" + socket.getInetAddress().toString() + " - " + e.getMessage() + "(" + e.getClass() +  ")" );
				e.printStackTrace();
				return false;
			} catch (Exception e) {
				Log.e("NETWORK", "SENT ERROR" + " TO IP:" + socket.getInetAddress().toString() + " - " + e.getMessage() + "(" + e.getClass() +  ")" );
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	@Override
	public void run() {
		
		createSocket();
		
		while (!Thread.currentThread().isInterrupted()) {
			
			handleInput();
			
			if ( !handleOutput() || socket.isClosed() )
			{
				Log.d("NETWORK", "Connection down, reconnecting...");
				isConnected = false;
				createSocket();
			}
		}
	}
}
