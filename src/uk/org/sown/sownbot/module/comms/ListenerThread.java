package uk.org.sown.sownbot.module.comms;

import java.net.ServerSocket;
import java.net.Socket;

import uk.org.sown.sownbot.SownBot;

public class ListenerThread extends Thread {
	private SownBot bot;
	private String channel = "";
	private int port;
	
	public ListenerThread(SownBot bot, int port)
	{
		this.bot = bot;
		this.port = port;
	}
	
	/**
	 * Set the channel to respond to
	 * @param channel The channel to respond to
	 */
	public void mainChannel(String channel)
	{
		this.channel = channel;
	}
	
	public void run()
	{
		ServerSocket serverSocket = null;
		try
		{
			serverSocket = new ServerSocket(port);
			//serverSocket.bind(new InetSocketAddress(bindHost, port));
		}
		catch (Exception e)
		{
			System.err.println("Could not listen on port: " + port + ". " + e.getMessage());
			System.exit(-1);
		}
		while(true)
		{
	    	Socket clientSocket = null;
	    	try
	    	{
	    		clientSocket = serverSocket.accept();
		    	new ConnectionHandler(bot, clientSocket, channel).start();
		    }
	    	catch (Exception e)
	    	{
	    		System.err.println("Accept failed: " + port);
				e.printStackTrace();
	    		System.exit(-1);
	    	}
		}	 
	}
}
