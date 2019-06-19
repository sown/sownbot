package uk.org.sown.sownbot.module.comms;

import uk.org.sown.sownbot.AbstractSownBotModule;
import uk.org.sown.sownbot.SownBot;

public class CommunicationModule extends AbstractSownBotModule {

	private static final String moduleName = "Communication";
	protected ListenerThread lt;
	
	public CommunicationModule(SownBot bot, int port)
	{
		super(bot, moduleName);
		lt = new ListenerThread(bot, port);
	}

	public void start()
	{
		lt.start();
	}
	
	public void mainChannel(String channel)
	{
		lt.mainChannel(channel);
	}
}
