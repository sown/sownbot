package uk.org.sown.sownbot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.org.sown.sownbot.action.LogMessageAction;
import uk.org.sown.sownbot.action.SendMessageAction;
import uk.org.sown.sownbot.action.SownBotAction;

public abstract class InteractiveSownBotModule extends AbstractSownBotModule {

	protected String[] keywords;
	protected String[][][] keywordUsage;
	protected String[][] commands;
	protected String[][][][] commandUsage;

	public InteractiveSownBotModule(SownBot bot, String moduleName,
			String[] keywords, String[][][] keywordUsage)
	{
		this(bot, moduleName, keywords, keywordUsage, new String[0][0], new String[0][0][0][0]);
	}
	
	/**
	 * Constructor
	 * @param bot The bot this module should be tied to.
	 * @param moduleName The name of this module
	 * @param keywords They main keywords this module responds to
	 * @param commands
	 */
	public InteractiveSownBotModule(
			SownBot bot, String moduleName,
			String[] keywords, String[][][] keywordUsage,
			String[][] commands, String[][][][] commandUsage)
	{
		super(bot, moduleName);
		assert(keywords.length == keywordUsage.length);
		assert(commands.length == commandUsage.length);
		
		this.keywords = keywords.clone();
		this.keywordUsage = new String[keywords.length][][];
		this.commands = new String[commands.length][];
		this.commandUsage = new String[commands.length][][][];

		// We want to sort the strings for binary searching
		Arrays.sort(this.keywords);
		
		for (int i = 0; i < keywords.length; i++)
		{
			// We need to preserve the index matching after sorting the keywords.
			final int keyw_index = Arrays.binarySearch(this.keywords, keywords[i]);
			
			this.keywordUsage[keyw_index] = keywordUsage[i].clone();
			
			// If no commands are defined don't do the following
			if (i + 1 > commands.length)
				continue;
				
			this.commands[keyw_index] = commands[i].clone(); 
			this.commandUsage[keyw_index] = new String[commands[i].length][][];
			
			// These need sorting as well
			Arrays.sort(this.commands[keyw_index]);
			
			for (int j = 0; j < commands[i].length; j++) {
				final int cmd_index = Arrays.binarySearch(this.commands[keyw_index], commands[i][j]);
				
				this.commandUsage[keyw_index][cmd_index] = commandUsage[i][j].clone();
			}
		}
	}

	/**
	 * Get the keywords this module responds to
	 * 
	 * @return the keywords understood by this module.
	 */
	public final String[] getModuleKeywords()
	{
		return this.keywords.clone();
	}

	/**
	 * Process an incoming message
	 * @param keyword the keyword this message matched
	 * @param remainder the remainder of the message string
	 * @param msg the original message
	 * @return Actions incurred by this message
	 * @throws SownBotModuleException Any exceptions not handled by the module
	 */
	public List<? extends SownBotAction> processMessage(
			Message msg, String keyword, String remainder)
			throws SownBotModuleException
	{
		if (commands.length == 0)
			return null;
		
		final int keyw_index = Arrays.binarySearch(keywords, keyword);
		
		if (commands[keyw_index].length == 0)
			return null;
		
		if (remainder == null)
			return sendUsage(msg, keyword);
			
		String[] parts = remainder.split(" ", 2);
		final String command = parts[0].toLowerCase();
		final String rest = parts.length == 2 ? parts[1] : null;
		
		final int cmd_index = Arrays.binarySearch(commands[keyw_index], command);
		if (cmd_index < 0)
		{
			ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
			actions.add(new LogMessageAction(msg));
			actions.add(new SendMessageAction(msg.getReplyTarget(), "Unknown command \"" + parts[0] + "\"."));
			actions.addAll(sendUsage(msg, keyword, keyw_index));
			
			return actions;
		}
		else
		{
			return processMessage(msg, keyword, command, rest);
		}
	}
	
	/**
	 * Process an incoming message
	 * @param keyword the keyword this message matched
	 * @param remainder the remainder of the message string
	 * @param msg the original message
	 * @return Actions incurred by this message
	 * @throws SownBotModuleException Any exceptions not handled by the module
	 */
	public List<? extends SownBotAction> processMessage(
			Message msg, String keyword, String command, String remainder)
			throws SownBotModuleException
	{
		return null;
	}
	
	
	public List<? extends SownBotAction> processHelpMessage(Message msg, String keyword, String remainder)
			throws SownBotModuleException
	{
		if (commands.length == 0 || remainder == null)
			return sendUsage(msg, keyword);
		
		final int keyw_index = Arrays.binarySearch(keywords, keyword);
		
		if (commands[keyw_index].length == 0)
			return sendUsage(msg, keyword, keyw_index);
		
		String[] parts = remainder.split(" ", 2);
		final String command = parts[0].toLowerCase();
//		final String rest = parts.length == 2 ? parts[1] : null;
		
		final int cmd_index = Arrays.binarySearch(commands[keyw_index], command);
		if (cmd_index < 0)
		{
			ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
			actions.add(new SendMessageAction(msg.getReplyTarget(), "Unknown command \"" + parts[0] + "\"."));
			actions.addAll(sendUsage(msg, keyword, keyw_index));
			
			return actions;
		}
		else
		{
			return sendUsage(msg, keyword, keyw_index, command, cmd_index);
		}
	}
	
	public List<? extends SownBotAction> sendUsage(
			Message msg, String keyword)
			throws SownBotModuleException
	{
		final int keyw_index = Arrays.binarySearch(keywords, keyword);
		return sendUsage(msg, keyword, keyw_index);
	}

	public List<? extends SownBotAction> sendUsage(
			Message msg, String keyword, String command)
			throws SownBotModuleException
	{
		final int keyw_index = Arrays.binarySearch(keywords, keyword);
		final int cmd_index = Arrays.binarySearch(commands[keyw_index], command);
		
		return sendUsage(msg, keyword, keyw_index, command, cmd_index);
	}
	
	protected List<? extends SownBotAction> sendUsage(
			Message msg, String keyword, int keyw_index)
			throws SownBotModuleException
	{
		if (keyw_index >= 0) {
			ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
			final int i = msg.isChannelMessage() ? 0 : 1;
			actions.add(new LogMessageAction(msg));
			actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), keywordUsage[keyw_index][i]));
			return actions;

		} else {
			throw new SownBotModuleException("This should never happen.");
		}
	}
	
	protected List<? extends SownBotAction> sendUsage(
			Message msg, String keyword, int keyw_index, String command, int cmd_index)
			throws SownBotModuleException
	{
		if (keyw_index >= 0 && cmd_index >= 0) {
			ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();

			actions.add(new LogMessageAction(msg));
			
			final int i = msg.isChannelMessage() ? 0 : 1;
			actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), commandUsage[keyw_index][cmd_index][i]));

			return actions;
		} else {
			throw new SownBotModuleException("This should never happen.");
		}
	}

}
