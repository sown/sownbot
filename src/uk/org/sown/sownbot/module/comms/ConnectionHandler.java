package uk.org.sown.sownbot.module.comms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import uk.org.sown.sownbot.SownBot;
import uk.org.sown.sownbot.module.icinga.IcingaModule;

public class ConnectionHandler extends Thread {
	
	private SownBot bot;
	private Socket clientSocket;
	private String channel;

	public ConnectionHandler(SownBot bot, Socket clientSocket, String channel)
	{
		this.bot = bot;
		this.clientSocket = clientSocket;
		this.channel = channel;
	}

	public void run()
	{
	   	PrintWriter out;
	   	BufferedReader in;
	   	long time = System.currentTimeMillis();
	   	System.out.println("MSG (" + time + "): Connection from " + clientSocket.getRemoteSocketAddress());
	   	
		try {
			out = new PrintWriter(clientSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
			if (!clientSocket.isClosed())
				try {
					clientSocket.close();
				} catch (IOException e) {}
			return;
		}
	              
		String outputLine;
	
		outputLine = "Hello";
		out.println(outputLine);
		
		Thread killer = new Thread(){
			public void run() {
				try {
					Thread.sleep(10 * 1000);
					if (! clientSocket.isClosed())
						clientSocket.close();
				} catch (InterruptedException e) {
					// if we're interrupted the connection has already closed
				} catch (IOException e) {
				}
			};
		};
		killer.start();
		
		try {
			while (true)
			{
				String inputLine;
				
				try {
					inputLine = in.readLine();
				}
				catch (IOException e)
				{
					break;
				}
				
				// End of input reached.
				if (inputLine == null)
					break;
				
				System.out.println("MSG (" + time + ") IN:" + inputLine);
						
				if(channel != "")
				{
					List<String> inputLineWords = Arrays.asList(inputLine.split(" "));
					
					if(inputLineWords.get(0).equals("NAGIOS"))
					{
						IcingaModule icinga = bot.getModuleByClass(IcingaModule.class);
						if (icinga == null)
							bot.sendMessage(channel, "Unable to retrieve Icinga Module to render incoming text.");
						
						outputLine = icinga.processIncoming(inputLineWords.subList(1, inputLineWords.size()));
					}
					else
					{
		    			outputLine = inputLine;    					
					}
					if (outputLine.startsWith("!h"))
						bot.sendCensoredMessage(channel, outputLine);
					else
						bot.sendLoggedMessage(channel, outputLine);
					
					out.println(outputLine);
				}
				else
				{
					out.println("Sorry, your message has been ignored");
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("Unhandled Exception! Oh no!");
			e.printStackTrace();
		}
			
		if (! clientSocket.isClosed())
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		if (killer.isAlive())
			killer.interrupt();
	}
}
