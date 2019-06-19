package uk.org.sown.sownbot.module;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jibble.pircbot.User;

import uk.org.sown.sownbot.DatabaseConnection;
import uk.org.sown.sownbot.InteractiveSownBotModule;
import uk.org.sown.sownbot.Message;
import uk.org.sown.sownbot.SownBot;
import uk.org.sown.sownbot.SownBotModuleException;
import uk.org.sown.sownbot.action.LogMessageAction;
import uk.org.sown.sownbot.action.SownBotAction;
import uk.org.sown.sownbot.action.SendMessageAction;

public class AutoOpModule extends InteractiveSownBotModule {
	
	private static final String moduleName = "AutoOp";
	private static final String[] channelUsage = new String[] {"Usage: !op <channel (optional)> | !op all | !op help"};
	private static final String[] privUsage = new String[] {"Usage: !op <channel> | !op help"};
	
	private static final String[] keywords = new String[] {
		"op"
	};
	private static final String[][][] keywordUsage = new String[][][] {
		{channelUsage, privUsage}
	};
		
	private DatabaseConnection dbc;

	public AutoOpModule(SownBot bot, String host, String database, String username, String password)
	{
		super(bot, moduleName, keywords, keywordUsage);
		dbc = new DatabaseConnection(host, database, username, password);
	}

	public boolean shouldBeOp(String channel, String sender, String login,
			String hostname) {
		String[] params = {channel, sender, login, hostname};
		try
		{
			ResultSet rs = dbc.ExecuteQuery("SELECT COUNT(*) AS count FROM ops WHERE ? LIKE channel AND ? LIKE sender AND ? LIKE login AND ? LIKE hostname", params);
			rs.next();
			return rs.getInt("count") > 0;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public String getUsername(String channel, String sender, String login,
			String hostname) {
		String[] params = {channel, sender, login, hostname};
		String username = "";
		try
		{
			ResultSet rs = dbc.ExecuteQuery("SELECT DISTINCT(username) AS username FROM ops WHERE ? LIKE channel AND ? LIKE sender AND ? LIKE login AND ? LIKE hostname", params);
			rs.next();
			if(rs.isLast())
			{
				username = rs.getString("username");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return username;
	}
	
	public void opAll(String channel)
	{
		bot.requestChannelUsers(channel);
		for (User user : bot.getUsers(channel)) {
			String nick = user.getNick();
			if(shouldBeOp(channel, nick, bot.getUserLogin(nick), bot.getUserHostname(nick)) && ! bot.isOp(channel, nick))
				bot.op(channel, nick);
		}
	}
	
	@Override
	public List<? extends SownBotAction> processMessage(
			Message msg, String keyword, String remainder)
			throws SownBotModuleException
	{
		ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
		actions.add(new LogMessageAction(msg));
		
		ArrayList<String> messageParts;
		if (remainder == null)
			messageParts = new ArrayList<String>();
		else
			messageParts = new ArrayList<String>(Arrays.asList(remainder.split(" ")));
		
		String channel;
		
		if (msg.isChannelMessage()) {
			if (messageParts.size() > 0)
				channel = messageParts.remove(0);
			else
				channel = msg.target;
		} else {
			if (messageParts.size() == 0) {
				actions.addAll(sendUsage(msg, keyword));
				return actions;
			}
			channel = messageParts.remove(0);
		}

		if (messageParts.size() > 0) {
			actions.addAll(sendUsage(msg, keyword));
			return actions;
		}
		
		if (msg.isChannelMessage() && channel.equals("all"))
		{
			opAll(msg.target);
		}
		else if(shouldBeOp(channel, msg.sender, msg.userLogin, msg.userHostname))
		{
			bot.op(channel, msg.sender);
		}
		else if (channel.equals(msg.target))
		{
			actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), new String[] { "You are not in the ops list" }));	
		}
		else
            {
			actions = new ArrayList<SownBotAction>();
                  actions.addAll(sendUsage(msg, keyword));
            }
		return actions;
	}
}
