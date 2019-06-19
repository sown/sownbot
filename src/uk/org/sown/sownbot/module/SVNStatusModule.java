package uk.org.sown.sownbot.module;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jibble.pircbot.Colors;

import uk.org.sown.sownbot.DatabaseConnection;
import uk.org.sown.sownbot.InteractiveSownBotModule;
import uk.org.sown.sownbot.Message;
import uk.org.sown.sownbot.SownBot;
import uk.org.sown.sownbot.SownBotModuleException;
import uk.org.sown.sownbot.action.LogMessageAction;
import uk.org.sown.sownbot.action.SendMessageAction;
import uk.org.sown.sownbot.action.SownBotAction;

public class SVNStatusModule extends InteractiveSownBotModule {
	enum SVNCommand { list, ack, blame };
	
	private static final String keyw_svn = "svn";
	private static final String cmd_list = "list";
	private static final String cmd_ack = "ack";
	private static final String cmd_blame = "blame";
	private static final String[] usage_keyw_svn  = new String[] {"Usage: !svn (list <args> | ack <args> | blame <args)"};
	private static final String[] usage_cmd_list  = new String[] {"Usage: !svn list [host] [repo path]"};
	private static final String[] usage_cmd_ack   = new String[] {"Usage: !svn ack <file path> in <repo path> on <host>"};
	private static final String[] usage_cmd_blame = new String[] {"Usage: !svn blame <user> <file path> in <repo path> on <host>"};
	
	
	private static final String moduleName = "SVNStatus";
	private static final String[] keywords = new String[] {
		keyw_svn
	};
	
	private static final String[][][] keywordUsage = new String[][][] {
		{ usage_keyw_svn, usage_keyw_svn }
	};
	private static final String[][] commands = new String[][] {
		{ cmd_list, cmd_ack, cmd_blame }
	};
	private static final String[][][][] commandUsage = new String[][][][] {
		{
			{usage_cmd_list,  usage_cmd_list},
			{usage_cmd_ack,   usage_cmd_ack},
			{usage_cmd_blame, usage_cmd_blame}
		}
	};
	
	private DatabaseConnection dbc;

	public SVNStatusModule(SownBot bot, String host, String database, String username, String password) {
		super(bot, moduleName, keywords, keywordUsage, commands, commandUsage);
		dbc = new DatabaseConnection(host, database, username, password);
	}
	
	@Override
	public List<? extends SownBotAction> processMessage(
			Message msg, String keyword, String command, String remainder)
			throws SownBotModuleException
	{
		ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
		actions.add(new LogMessageAction(msg));
		
		SVNCommand svnCommand = null;
		try {
			svnCommand = SVNCommand.valueOf(command);
		} catch (IllegalArgumentException e) {}
		
		if(svnCommand != null)
		{
			switch (svnCommand) {
			case list:
				actions.addAll(processListRequest(this.bot, msg, remainder));
				break;
			case ack:
				actions.addAll(processAckRequest(this.bot, msg, remainder));
				break;
			case blame:
				actions.addAll(processBlameRequest(this.bot, msg, remainder));			
				break;
			}
		} 
		else
		{
			actions.addAll(sendUsage(msg, keyword, command));
		}
			
		return actions;
	}


	private Collection<? extends SownBotAction> processListRequest(SownBot sownBot, Message msg, String args) {
		String[] parts = args == null ? new String[0] : args.split(" ");
		ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();

		String host = (parts.length > 0 && ! parts[0].equals("")) ? parts[0] : null;
		String repo = (parts.length > 1 && ! parts[1].equals("")) ? parts[1] : null;
		
		ArrayList<String> params = new ArrayList<String>();
		String queryString = 
			"SELECT host_name, repo_path, file_path, blamed_user FROM svn_status";
		
		if (host != null) {
			queryString = queryString + " WHERE host_name = ?";
			params.add(host);
		}
		if (repo != null) {
			queryString = queryString + " AND repo_path = ?";
			params.add(repo);
		}
		
		try {
			ResultSet rs = dbc.ExecuteQuery(queryString, params.toArray(new String[0]));
			int rowCount = 0;
			while (rs.next()) {
				rowCount ++;
				String blamed = rs.getString("blamed_user");
				if (blamed == null)
					blamed = "";
				else
					blamed = " (" + Colors.BLUE + blamed + Colors.NORMAL + ")";
					
				String message =
					"Changes to " + Colors.RED + rs.getString("file_path") + Colors.NORMAL +
					" in " + Colors.RED + rs.getString("repo_path") + Colors.NORMAL +
					" on " + Colors.RED + rs.getString("host_name") + Colors.NORMAL +
					blamed;
				
				actions.add(new SendMessageAction(msg.getReplyTarget(), message));
			}
			
			// If we are still on row 0 then there were no entries 
			if (rowCount == 0)
				actions.add(new SendMessageAction(msg.getReplyTarget(), "No uncommitted changes in the database."));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return actions;
	}
	
	private List<? extends SownBotAction> processAckRequest(SownBot sownBot, Message msg, String args)
			throws SownBotModuleException
	{
		String[] parts = args == null ? new String[0] : args.split(" ");

		ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
		String host, repo, filename;
		String queryString = null;
		String[] queryArgs = null;
		
		if (parts.length == 0)
			return sendUsage(msg, keyw_svn, cmd_ack);
		
		if (parts.length == 3) {
			queryString = "UPDATE svn_status SET last_acknowledge_time = TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP) WHERE file_path LIKE ? AND repo_path LIKE ? AND host_name LIKE ?;";
			queryArgs = parts;
		} else if (parts.length > 3) {
			// <file path> in <repo path> on <host>
			int inIndex = args.indexOf(" in ");
			int onIndex = args.indexOf(" on ");
			
			if (inIndex != -1 && onIndex != -1) {
				filename = args.substring(0, inIndex).trim();
				repo = args.substring(inIndex + 4, onIndex).trim();
				host = args.substring(onIndex + 4).trim();
				
				queryString = "UPDATE svn_status SET last_acknowledge_time = TIMESTAMPADD(DAY, 1, CURRENT_TIMESTAMP) WHERE file_path LIKE ? AND repo_path LIKE ? AND host_name LIKE ?;";
				queryArgs = new String[] {filename, repo, host};
			} else {
				actions.add(new SendMessageAction(msg.getReplyTarget(), "Could not parse file details."));
			}
		} else {
			actions.add(new SendMessageAction(msg.getReplyTarget(), "Invalid number of arguments."));
		}

		if (queryString != null) {
			try {
				int rs = dbc.ExecuteUpdate(queryString, queryArgs);
				if (rs > 1)
					actions.add(new SendMessageAction(msg.getReplyTarget(), rs + " files acknowledged."));
				else if (rs == 1)
					actions.add(new SendMessageAction(msg.getReplyTarget(), "File '" + queryArgs[0] + "' acknowledged."));
				else
					actions.add(new SendMessageAction(msg.getReplyTarget(), "File '" + queryArgs[0] + "' not found."));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return actions;
	}

	private Collection<? extends SownBotAction> processBlameRequest(SownBot sownBot, Message msg, String args) {
		ArrayList<String> parts = new ArrayList<String>(Arrays.asList(args.split(" ")));
		ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
		args = args.trim();
		String host, repo, filename, blamed;
		String queryString = null;
		String[] queryArgs = null;
		
		if (parts.size() == 4) {
			queryString = "UPDATE svn_status SET blamed_user = ? WHERE file_path LIKE ? AND repo_path LIKE ? AND host_name LIKE ?;";
			queryArgs = parts.toArray(new String[0]);
		} else if (parts.size() > 4) {
			// <file path> in <repo path> on <host>
			int blameSpaceIndex = args.indexOf(" ");
			int inIndex = args.indexOf(" in ");
			int onIndex = args.indexOf(" on ");
			
			if (inIndex != -1 && onIndex != -1) {
				blamed = args.substring(0, blameSpaceIndex);
				filename = args.substring(blameSpaceIndex + 1, inIndex);
				repo = args.substring(inIndex + 4, onIndex);
				host = args.substring(onIndex + 4);
				
				queryString = "UPDATE svn_status SET blamed_user = ? WHERE file_path LIKE ? AND repo_path LIKE ? AND host_name LIKE ?;";
				queryArgs = new String[] {blamed, filename, repo, host};
			}
		} else {
			actions.add(new SendMessageAction(msg.getReplyTarget(), "Invalid number of arguments."));
		}		
		
		if (queryString != null) {
			try {
				int rs = dbc.ExecuteUpdate(queryString, queryArgs);
				if (rs > 1)
					actions.add(new SendMessageAction(msg.getReplyTarget(), rs + " files blamed on " + queryArgs[0] + "."));
				else if (rs == 1)
					actions.add(new SendMessageAction(msg.getReplyTarget(), rs + " file blamed on " + queryArgs[0] + "."));
				else
					actions.add(new SendMessageAction(msg.getReplyTarget(), "File '" + parts.get(1) + "' not found."));
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			actions.add(new SendMessageAction(msg.getReplyTarget(), "Command not understood."));
		}
		return actions;
	}
}
