package art.leandrogarber.blowjob;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

class CommunicationThread implements Runnable {

	private Socket clientSocket;
	private BufferedReader input;
	private BufferedWriter output;
	private Handler handler;
	private String message;
	
	private final String MESSAGE_PING = "Ping?";
	private final String MESSAGE_PONG = "Pong!";
	
	private final boolean PING_PONG_ENABLED = false;
	private final boolean INPUT_ENABLED = false;
	
	public CommunicationThread(Socket clientSocket, Handler handler) {
		this.clientSocket = clientSocket;
		this.handler = handler;
		
		try {
			if ( INPUT_ENABLED )
				this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream(), Charset.forName("UTF-8")));
			
			this.output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), Charset.forName("UTF-8")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendMessage(String message)
	{
		this.message = message;
	}
	
	private void handleInput()
	{
		try {
			if ( INPUT_ENABLED ) {
				if ( input.ready() )
				{
					String read = input.readLine();// + System.getProperty("line.separator");
					
					Log.d("NETWORK", "READ: " + read);
					
					if ( PING_PONG_ENABLED && read.equals(MESSAGE_PING) )
						sendMessage(MESSAGE_PONG);
					else
					{
						Message msg = new Message();
						CommunicationMessage commMsg = new CommunicationMessage(read, this);
						msg.obj = commMsg;
						
						handler.sendMessage(msg);
					}
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
				Log.d("NETWORK", "Sending " + message);				
				output.write(message + System.getProperty("line.separator"));
				output.flush();
				Log.d("NETWORK", "SENT: " + message + " TO IP:" + clientSocket.getInetAddress().toString());
				message = null;
			} catch (SocketException e) {
				Log.e("NETWORK", "SENT ERROR" + " TO IP:" + clientSocket.getInetAddress().toString() + " - " + e.getMessage() + "(" + e.getClass() +  ")" );
				e.printStackTrace();
				return false;
			} catch (Exception e) {
				Log.e("NETWORK", "SENT ERROR" + " TO IP:" + clientSocket.getInetAddress().toString() + " - " + e.getMessage() + "(" + e.getClass() +  ")" );
				e.printStackTrace();
			}
		}
		
		return true;
	}
	
	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			handleInput();
			if ( !handleOutput() )
			{
				Log.d("NETWORK", "Connection down, closing thread...  ");
				break;
			}
		}
	}
	
}
