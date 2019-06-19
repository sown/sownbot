package uk.org.sown.sownbot.module;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.org.sown.sownbot.DatabaseConnection;
import uk.org.sown.sownbot.InteractiveSownBotModule;
import uk.org.sown.sownbot.Message;
import uk.org.sown.sownbot.SownBot;
import uk.org.sown.sownbot.SownBotModuleException;
import uk.org.sown.sownbot.action.LogMessageAction;
import uk.org.sown.sownbot.action.SendMessageAction;
import uk.org.sown.sownbot.action.SownBotAction;

public class CensorModule extends InteractiveSownBotModule {
	private DatabaseConnection dbc;
	
	private static final String moduleName = "Censor";

	private static final String keyw_censor = "censor";
	private static final String keyw_hide = "hide";
	private static final String keyw_h = "h";
	private static final String[] keywords = new String[] {
		keyw_censor, keyw_hide, keyw_h
	};
	private static final String[] usage_keyw_hide = new String[] {
		"Usage: !hide [text] - Hides this line from the logs."
	}; 
	
	private static final String[][][] keywordUsage = new String[][][] {
		{
			{	"Usage: !censor (<censor search> | <message ID> <checksum>)",
				"Censor string accepts SQL wildcards (%,_ etc)."},
			{	"Usage: !censor (<#channel> <censor search> | <#channel> <message ID> <checksum>)",
				"Censor string accepts SQL wildcards (%,_ etc)."}
		},
		{
			usage_keyw_hide, usage_keyw_hide
		},
		{
			{"Alias for !hide"},{"Alias for !hide"}
		}
	};
	
	static final String findQuery = "SELECT * FROM log WHERE censored IS NULL AND message LIKE ? AND channel = ? ORDER BY datetime DESC LIMIT 1";
	static final String censorQuery = "UPDATE log SET censored = ? WHERE id = ?";
	private final String censor_secret = "dirty hack " + Math.random();

	public CensorModule(SownBot bot, String host, String database, String username, String password)
	{
		super(bot, moduleName, keywords, keywordUsage);
		dbc = new DatabaseConnection(host, database, username, password);
	}

	@Override
	public List<? extends SownBotAction> processMessage(
			Message msg, String keyword, String remainder)
			throws SownBotModuleException
	{
		ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
		actions.add(new LogMessageAction(msg, true));
		
		if (keyword.equals(keyw_hide) || keyword.equals(keyw_h)){
			if (remainder != null && remainder.trim().length() > 0)
				msg.message = remainder;
			return actions;
		} else if (msg.isChannelMessage())
		{
			actions.add(new SendMessageAction(msg.target, "Censor requests should now be sent as a private message.", true));
			return actions;
		}
		
		ArrayList<String> al = new ArrayList<String>(Arrays.asList(remainder.split(" ")));
		
		String channel;
		if (msg.isChannelMessage())
			channel = msg.target;
		else {
			channel = al.remove(0);
			
			if (channel.charAt(0) == '#') {
				// nothing to do here!
			} else {
				actions.add(new SendMessageAction(msg.getReplyTarget(), "You must specify a channel to censor a line.", true));
				return actions;
			}
		}
		
		if(! bot.isOp(channel, msg.sender)) {
			actions.add(new SendMessageAction(msg.getReplyTarget(), "Sorry, you need to be a channel operator to censor a line.", true));
			return actions;
		}
	
		int rowID = -1;
		if(al.size() <= 2)
		{
			try
			{
				rowID = Integer.parseInt(al.get(0));
			}
			catch(NumberFormatException ex) {}
		}
		
		if(rowID == -1)
		{
			if (al.size() == 0) {
				actions.addAll(sendUsage(msg, keyword));
				return actions;
			}
			
			String censorLine = "";
			for(int i=0; i<al.size(); i++)
			{
				censorLine += al.get(i) + " ";
			}
			censorLine = censorLine.trim();
			
			actions.addAll(processSearchRequest(msg, censorLine, channel));
		}
		else
		{	
			if (al.size() < 2)
			{
				String target = msg.isChannelMessage() ? msg.target : msg.sender;
				actions.add(new SendMessageAction(target, "Line hash missing. Request ignored.", true));
			}
			else
			{
				String hash = al.get(1);
				actions.addAll(processCensorResponse(msg, rowID, hash));
			}
		}
		return actions;
	}
	
	private List<? extends SownBotAction> processSearchRequest(Message msg, String search, String channel) {	
		String[] params = { search, channel };
		String target = msg.getReplyTarget();
		
		try {
			ResultSet rs = dbc.ExecuteQuery(findQuery, params);
			if (rs.next()) {
				int id = rs.getInt("id");
				String message = rs.getString("message");
				String messageSender = rs.getString("sender");
				Date datetime = rs.getDate("datetime");
				return SendMessageAction.createSingleMessage(
						target,
						"To censor the line: '" + message  + "' by '" + messageSender + "' at " + datetime + ", respond '!censor " + (msg.isChannelMessage() ? "" : channel + " ") + id + " " + getHashedID(id) + "'",
						true);
			} else {
				return SendMessageAction.createSingleMessage(
						target,
						"No lines like '" + search + "' found.",
						true);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return SendMessageAction.createSingleMessage(
					target,
					"An unhandled error occured!",
					true);
		}
	}
	
	private List<? extends SownBotAction> processCensorResponse(Message msg, int rowID, String hash) throws SownBotModuleException {
		String target = msg.getReplyTarget();

		if (hash.equals(getHashedID(rowID)))
		{
			String user = (msg.sender + "!" + msg.userLogin + "@" + msg.userHostname);
			try {
				int affected = dbc.ExecuteUpdate(censorQuery, user, rowID);
				if(affected > 0)
				{
					LoggerModule logger = bot.getModuleByClass(LoggerModule.class);
					if (logger == null)
						throw new SownBotModuleException("Unable to retrieve Logging Module to log successfull censor action.");
					
					// Not sure if we need to do this. The message was already logged.
					logger.logCensorAction(msg.target, msg.sender, msg.userLogin, msg.userHostname, msg.message);
					
					return SendMessageAction.createSingleMessage(
							target,
							"Line " + rowID + " successfully censored by '" + msg.sender + "'." ,
							true);
				} else {
					return SendMessageAction.createSingleMessage(
							target,
							"Failed to censor line " + rowID + ".",
							true);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return SendMessageAction.createSingleMessage(
						target, 
						"Failed to censor line " + rowID + ". (Exception)", 
						true);
			}
		}
		else
		{
			return SendMessageAction.createSingleMessage(
					target, 
					"Invalid hash for line " + rowID + ". Request ignored.", 
					true);
		}
	}
	
	private String getHashedID(int id) throws SownBotModuleException
	{
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			throw new SownBotModuleException("Could not load SHA1 hashing algorithm.", e);
		}

		md.update((id + this.censor_secret).getBytes());
		byte[] bytes = md.digest();
		
		StringBuffer sb = new StringBuffer(bytes.length *2);
		for (byte b: bytes)
			sb.append(String.format("%02x", b));

		// We don't need to to be so long.
		return sb.toString().substring(0, 6);
	}
	
	@Override
	protected List<? extends SownBotAction> sendUsage(
			Message msg, String keyword, int keyw_index)
			throws SownBotModuleException
	{
		if (keyw_index >= 0) {
			ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
			final int i = msg.isChannelMessage() ? 0 : 1;
			actions.add(new LogMessageAction(msg, true));
			actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), keywordUsage[keyw_index][i], true));
			return actions;
		} else {
			throw new SownBotModuleException("This should never happen.");
		}
	}
	
	@Override
	protected List<? extends SownBotAction> sendUsage(
			Message msg, String keyword, int keyw_index, String command, int cmd_index)
			throws SownBotModuleException
	{
		if (keyw_index >= 0 && cmd_index >= 0) {
			ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();

			actions.add(new LogMessageAction(msg, true));
			
			final int i = msg.isChannelMessage() ? 0 : 1;
			actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), commandUsage[keyw_index][cmd_index][i], true));

			return actions;
		} else {
			throw new SownBotModuleException("This should never happen.");
		}
	}
}
