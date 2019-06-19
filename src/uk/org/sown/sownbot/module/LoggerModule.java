package uk.org.sown.sownbot.module;

import java.text.SimpleDateFormat;
import java.util.Date;

import uk.org.sown.sownbot.AbstractSownBotModule;
import uk.org.sown.sownbot.DatabaseConnection;
import uk.org.sown.sownbot.Message;
import uk.org.sown.sownbot.SownBot;

public class LoggerModule extends AbstractSownBotModule {
	
	private static final String moduleName = "Logger";
	
	protected DatabaseConnection dbc;
	private static final boolean LOGGING_ENABLED = true;
	private boolean logging_working = true;

	public LoggerModule(SownBot bot, String host, String database, String username, String password)
	{
		super(bot, moduleName);
		dbc = new DatabaseConnection(host, database, username, password);
	}

	public void logMessage(Message msg)
	{
		logMessage(msg, false);
	}
	
	private void alertFailure(Exception e, String msg, String target, String sender)
	{
		System.err.println("Following exception at " + new SimpleDateFormat().format(new Date()));
		e.printStackTrace();

		if (sender == bot.getNick())
			return;
		
		if (target == null ||
			target.equals(""))
			target = "#sown";
		
		if (logging_working)
			bot.sendMessage(target, "Unable to log " + msg + ". Logging is DOWN until further notice.");
		
		logging_working = false;
	}
	
	private void loggingWorking(String target)
	{
		if (! logging_working)
			bot.sendMessage(target, "Logging is working again! :)");
		
		logging_working = true;
	}
	
	public void logMessage(Message msg, boolean censored)
	{
		if (!LOGGING_ENABLED)
			return;

		try
		{
			if (censored)
			{
				String[] params = new String[] {msg.target, msg.sender, msg.userLogin, msg.userHostname, msg.message, "sownbot"};
				dbc.ExecuteUpdate("INSERT INTO log (channel, sender, login, hostname, message, datetime, type, censored) VALUES (?, ?, ?, ?, ?, NOW(), 'message', ?)", params);	
			} else
			{
				String[] params = new String[] {msg.target, msg.sender, msg.userLogin, msg.userHostname, msg.message};
				dbc.ExecuteUpdate("INSERT INTO log (channel, sender, login, hostname, message, datetime, type) VALUES (?, ?, ?, ?, ?, NOW(), 'message')", params);
			}
			loggingWorking(msg.target);
		}
		catch (Exception e)
		{
			alertFailure(e, "message", msg.target, msg.sender);
		}
	}

	public void logAction(String channel, String sender, String login,
			String hostname, String action)
	{
		if (!LOGGING_ENABLED)
			return;
		
		try
		{
			String[] params = new String[] {channel, sender, login, hostname, action};
			dbc.ExecuteUpdate("INSERT INTO log (channel, sender, login, hostname, message, datetime, type) VALUES (?, ?, ?, ?, ?, NOW(), 'action')", params);
			loggingWorking(channel);
		}
		catch (Exception e)
		{
			alertFailure(e, "action", channel, sender);
		}

	}

	public void logTopic(String channel, String sender, String login,
			String hostname, String topic)
	{
		if (!LOGGING_ENABLED)
			return;
	
		try
		{
			String[] params = new String[] {channel, sender, login, hostname, topic};
			dbc.ExecuteUpdate("INSERT INTO log (channel, sender, login, hostname, message, datetime, type) VALUES (?, ?, ?, ?, ?, NOW(), 'topic')", params);
			loggingWorking(channel);
		}
		catch (Exception e)
		{
			alertFailure(e, "topic", channel, sender);
		}	
	}
	
	public void logMember(String channel, String sender, String login,
			String hostname, String action)
	{
		if (!LOGGING_ENABLED)
			return;
		
		try
		{
			String[] params = new String[] {channel, sender, login, hostname, action};
			dbc.ExecuteUpdate("INSERT INTO log (channel, sender, login, hostname, message, datetime, type) VALUES (?, ?, ?, ?, ?, NOW(), 'member')", params);
			loggingWorking(channel);
		}
		catch (Exception e)
		{
			String[] parts = action.split(" ");
			String msg;
			if (parts[0].equals("MODE") && parts.length > 1)
				msg = parts[0] + ' ' + parts[1];
			else
				msg = parts[0];
			
			alertFailure(e, msg, channel, sender);
		}
	}

	public void logCensorAction(String channel, String sender, String login,
			String hostname, String action) 
	{
		if (!LOGGING_ENABLED)
			return;
		
		try
		{
			String[] params = new String[] {channel, sender, login, hostname, action, (sender + "!" + login + "@" + hostname)};
			dbc.ExecuteUpdate("INSERT INTO log (channel, sender, login, hostname, message, datetime, type, censored) VALUES (?, ?, ?, ?, ?, NOW(), 'action', ?)", params);
			loggingWorking(channel);
		}
		catch (Exception e)
		{
			
		}
	}
}
