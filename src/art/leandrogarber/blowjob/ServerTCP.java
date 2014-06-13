
package art.leandrogarber.blowjob;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import android.os.Handler;
import android.util.Log;

public class ServerTCP implements Runnable {
	private final int port;
	private Handler handler;
	private ServerSocket serverSocket;
	private ArrayList<CommunicationThread> arrComunications;	
	
	ServerTCP(Handler handler, int port)
	{
		this.handler = handler;
		this.port = port;
		
		arrComunications = new ArrayList<CommunicationThread>();
		
	}
	
	private void createServerSocket()
	{
		try	{
			serverSocket = new ServerSocket(this.port);
			
			Log.d("NETWORK", "TCP LISTENING");
		} catch (IOException e) {
			Log.e("NETWORK", "TCP NOT LISTENING :(" + " - " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void destroyMe()
	{
		if ( serverSocket != null )
		{
			Log.e("NETWORK", "DESTROYING SERVER");
			
			try {
				serverSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e("NETWORK", "Couldn't destroy :(" + " - " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public void sendMessageToClients(String message)
	{
		Log.d("NETWORK", "SENDING " + message + " TO " + arrComunications.size() + " CONNECTIONS");
		
		for ( CommunicationThread comm : arrComunications )
		{
			comm.sendMessage(message);
		}
	}
	
	public void sendMessage(String message, CommunicationThread who)
	{
		for ( CommunicationThread comm : arrComunications )
		{
			if ( comm == who )
				comm.sendMessage(message);
		}
	}
	
	public void run() {
		createServerSocket();
		
		while (!Thread.currentThread().isInterrupted()) {
			try {				
				if ( serverSocket == null )
					break;
				
				Socket socket = serverSocket.accept();
				Log.d("NETWORK", "New client - " + socket.getInetAddress().toString());
				
				CommunicationThread commThread = new CommunicationThread(socket, handler);
				new Thread(commThread).start();
				
				arrComunications.add(commThread);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Log.d("NETWORK", "TCPServer is going bye bye");
	}
}
