package uk.org.sown.sownbot.module.icinga;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jibble.pircbot.Colors;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import uk.org.sown.sownbot.DatabaseConnection;
import uk.org.sown.sownbot.InteractiveSownBotModule;
import uk.org.sown.sownbot.Message;
import uk.org.sown.sownbot.SownBot;
import uk.org.sown.sownbot.SownBotModuleException;
import uk.org.sown.sownbot.action.LogMessageAction;
import uk.org.sown.sownbot.action.SendMessageAction;
import uk.org.sown.sownbot.action.SownBotAction;
import uk.org.sown.sownbot.module.AutoOpModule;


public class IcingaModule extends InteractiveSownBotModule {
	enum IcingaCommand { help, list, servers, campus, core, nodes, allnodes, ack, check, downtime };
	
	private static final String moduleName = "Icinga";
	private static final String keyw_todo = "icinga";
	private static final String[] keywords = new String[] {
		keyw_todo
	};
	private static final String usage = "Usage: !icinga (<nodename> | <nodenumber> | servers | campus | core | list | nodes | allnodes | ack \"<service>\" <reason> | check \"<service>\" downtime \"<service>\" <hours> | help)";

	private static final String[][][] keywordUsage = new String[][][] {
		{
			{usage},
			{usage}
		}
	};
	
	private DatabaseConnection dbc;

	protected static String apiUri = "";

	static final String query = 
		"SELECT DISTINCT icinga_hoststatus.last_state_change, " +
		"icinga_hoststatus.current_state AS host_status, " +
		"icinga_hosts.alias AS host_name " +
		"FROM icinga_hoststatus " +
		"JOIN icinga_hosts ON icinga_hoststatus.host_object_id = icinga_hosts.host_object_id " +
		"JOIN icinga_hostgroup_members ON icinga_hosts.host_object_id = icinga_hostgroup_members.host_object_id " +
		"JOIN icinga_hostgroups ON icinga_hostgroups.hostgroup_id = icinga_hostgroup_members.hostgroup_id";
	
	public IcingaModule(SownBot bot, String host, String database, String username, String password, String api_uri)
	{
		super(bot, moduleName, keywords, keywordUsage);
		apiUri = api_uri;
		dbc = new DatabaseConnection(host, database, username, password);
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
		
		if(messageParts.size() == 0)
		{
			actions.add(new SendMessageAction(msg.getReplyTarget(), "Icinga module is active but doesn't know what you want"));
			return actions;
		}
		
		IcingaCommand icingaCommand = null;
		try {
			icingaCommand = IcingaCommand.valueOf(messageParts.get(0).toLowerCase());
		} catch (IllegalArgumentException e) {}
		
		if(icingaCommand != null)
		{
			switch(icingaCommand)
			{
			case list:
				actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), listKnownHosts()));
				break;
			case servers:
				actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), serverStatuses()));
				break;
			case campus:
				actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), campusStatuses()));
				break;
			case core:
				actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), coreStatuses()));
				break;
			case nodes:
				actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), nodeStatuses()));
				break;
			case allnodes:
				actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), allNodeStatuses()));
				break;
			case ack:
				actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), acknowledgeService(messageParts, msg)));
				break;
			case check:
				actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), checkService(messageParts, msg)));
                        break;
			case downtime:
                        actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), downtimeService(messageParts, msg)));
                        break;
			case help:
				actions.addAll(sendUsage(msg, keyword));
		      }	
			
			return actions;
		}
		else
		{
			String al = "";
			for(int i=0; i<messageParts.size(); i++)
			{
				al += messageParts.get(i) + " ";
			}
			actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), specificStatus(al.replaceAll("[^0-9A-Za-z ]", "").trim())));
			return actions;
		}
	}

	/**
	  * Processes the message and sets an Acknowledgement
	  * @return the response to the request
	  */
	private String[] acknowledgeService(ArrayList<String> messageParts, Message msg) {
		String al = new String();
		String fullservice = new String();
		
		boolean found_service = false;
            for(int i=1; i<messageParts.size(); i++)
		{
			String word = messageParts.get(i);
			if(found_service) {
			      al += word + " ";
			}
			else
			{			
				found_service=word.endsWith("\"");
				fullservice += word + " ";
			}
		}

		al = al.replaceAll("\"", "\\\\\\\"").trim();
		if(!found_service||al.equals(""))
			return new String[] {"Usage: !icinga ack \"<service>\" <reason>"};
		
            String username = getSownUsernameFromMessage("#sown", msg);
            if (username.equals("UNKNOWN")) {
                  return new String[] {"Sorry you cannot acknowledge host/service: Unable to tie your nick to a SOWN username."};
            }

            String[] hostservjson = getHostServiceAndJson(fullservice);

		String json = hostservjson[2] + ", \"comment\": \""+al+"\", \"author\": \""+username+"\"}";

		NagiosApiResponse nar = makeHttpPost(apiUri + "/acknowledge_problem", json);

            if (nar.getSuccess() == true) {
			return new String[] {};
		}
		return new String[] {"Could not acknowledge "+(hostservjson[1].length()>0?"'"+hostservjson[1]+"' on ":"")+"'"+hostservjson[0]+"'. "+nar.getResponse()};
	}

	 /**
        * Processes the message and reschedules check for a service
        * @return the response to the request
        */
      private String[] checkService(ArrayList<String> messageParts, Message msg) {
            String fullservice = new String();

		for(int i=1; i<messageParts.size(); i++)
            {
			fullservice += messageParts.get(i) + " ";
            }
		
		if(fullservice.length() == 0)
                  return new String[] {"Usage: !icinga check \"<service>\""};

		fullservice = fullservice.trim();

		String username = getSownUsernameFromMessage("#sown", msg);
            if (username.equals("UNKNOWN")) {
                  return new String[] {"Sorry you cannot acknowledge host/service: Unable to tie your nick to a SOWN username."};
            }

		String[] hostservjson = getHostServiceAndJson(fullservice);
	
            String json = hostservjson[2] + "}";

		NagiosApiResponse nar = makeHttpPost(apiUri + "/schedule_check", json);

            if (nar.getSuccess() == true) {
                  return new String[] {"Rescheduled an immediate check for "+(hostservjson[1].length()>0?"'"+hostservjson[1]+"' on ":"")+"'"+hostservjson[0]+"'. The check's hard state may take a while to change."};
            }
            return new String[] {"Could not reschedule an immediate check for "+(hostservjson[1].length()>0?"'"+hostservjson[1]+"' on ":"")+"'"+hostservjson[0]+"'. "+nar.getResponse()};
      }

	/**
        * Processes the message and schedules downtime for a service
        * @return the response to the request
        */
      private String[] downtimeService(ArrayList<String> messageParts, Message msg) {
		String hours = new String();
            String fullservice = new String();

            boolean found_service = false;
            for(int i=1; i<messageParts.size(); i++)
            {
                  String word = messageParts.get(i);
                  if(found_service) {
                        hours += word;
				break;
                  }
                  else
                  {
                        found_service=word.endsWith("\"");
                        fullservice += word + " ";
                  }
            }
		
		hours = hours.replaceAll("\"", "\\\\\\\"").trim();
            if(!found_service||hours.equals(""))
                  return new String[] {"Usage: !icinga downtime \"<service>\" <hours>"};

            String username = getSownUsernameFromMessage("#sown", msg);
            if (username.equals("UNKNOWN")) {
                  return new String[] {"Sorry you cannot schedule downtime for host/service: Unable to tie your nick to a SOWN username."};
            }

            String[] hostservjson = getHostServiceAndJson(fullservice);
		String json = new String();

		try{
			int duration = Integer.parseInt(hours) * 3600;
			json = hostservjson[2] + ", \"duration\": \""+duration+"\" }";
		}
		catch (NumberFormatException e) {
			return new String[] {"Could not schedule downtime for "+(hostservjson[1].length()>0?"'"+hostservjson[1]+"' on ":"")+"'"+hostservjson[0]+"'. Hours specified was not a whole number."};	
		}
		NagiosApiResponse nar = makeHttpPost(apiUri + "/schedule_downtime", json);
		
            if (nar.getSuccess() == true) {
                  return new String[] {"Scheduled downtime for "+(hostservjson[1].length()>0?"'"+hostservjson[1]+"' on ":"")+"'"+hostservjson[0]+"'."};
            }
            return new String[] {"Could not schedule downtime for "+(hostservjson[1].length()>0?"'"+hostservjson[1]+"' on ":"")+"'"+hostservjson[0]+"'. "+nar.getResponse()};
      }

	/**
	 * Extracts the host and service from the full service string 
       * and generates the first part of the JSON request object.
	 * @return String[] containing the host name, service name and the partial json object.
       */
	private String[] getHostServiceAndJson(String fullservice){
		String host = new String();
		String service = new String();
		String json = new String();
		int firstslash;
		fullservice = fullservice.replaceAll("[^0-9A-Za-z\\-/\\(\\)# ]","").trim();
            if((firstslash = fullservice.indexOf("/"))!=-1) {
                  String hostalias = fullservice.substring(0,firstslash);
			host = dealiasHost(hostalias);	
                  service = fullservice.replace(hostalias+"/","");
			json = "{\"host\": \""+host+"\", \"service\": \""+service+"\"";
            }
            else
            {
                  host=dealiasHost(fullservice+""); //don't trust the object cloning - it should do it as its a string, but java can be flakey...
                  service="";
			json =  "{\"host\": \""+host+"\"";
            }
		return new String[] {host, service, json};
	}

	/**
	 * If an alias is provided for the host change for the display_name of the host
	 * @return String the dealiased host name
	 */
	private String dealiasHost(String hostalias){
		try{
                  String aliasQuery = "SELECT display_name FROM icinga_hosts WHERE alias = '"+hostalias+"'";
                  ResultSet rs = dbc.ExecuteQuery(aliasQuery);
                  if (rs.next()){
                        if (hostalias != rs.getString("display_name")){
					return rs.getString("display_name");
                        }
                  }
            }
            catch (Exception e){
            }
		return hostalias;
	}	

	/**
	 * Works out the SOWN username from the IRC nick, username 
	 * and host from the sender of the request.
	 * @return String the SOWN username of the requester.
	 */
      private String getSownUsernameFromMessage(String channel, Message msg) {
      	String username = "";
            try{
                  AutoOpModule autoOp = bot.getModuleByClass(AutoOpModule.class);
                  if (autoOp == null)
                        throw new SownBotModuleException("Unable to retrieve AutoOp Module for authentication.");

                  username = autoOp.getUsername(channel, msg.sender, msg.userLogin, msg.userHostname);

                  if (username == null || username.equals("")) {
				return new String("UNKNOWN");
                  }
            }
            catch(SownBotModuleException e) {
			return new String("UNKNOWN");
            }
		return username;
	}

	/**
       * Create an HTTP post with a JSON object and send.  
       * Process response for success/failure.
       * @return boolean true on success, false on failure.
       */
	private NagiosApiResponse makeHttpPost(String urlstr, String json) {
		try {
			// Send data
			URL url = new URL(urlstr);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/json");
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(json);
			wr.flush();
			System.out.println("Nagios API Request - URL: "+urlstr+" | JSON: "+json);
			
			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			String response = new String();
			while ((line = rd.readLine()) != null) {
				response += line;
			}
			System.out.println("Nagios API Response - "+response);
			wr.close();
                  rd.close();
			JSONObject json_obj = (JSONObject) JSONSerializer.toJSON( response );
			return new NagiosApiResponse(json_obj.getBoolean("success"), json_obj.getString("content"));
		} catch (Exception e) {
			e.printStackTrace();
			return new NagiosApiResponse(false, e.getMessage());
		}
	}

	/**
	 * Process a 'All Node Status' request
	 * @return The response to the request
	 */
	private String[] allNodeStatuses() {
		return shortStatuses("nodes", "icinga_hostgroups.alias LIKE '%Nodes'");
	}

	/**
	 * Process a 'Node Status' request
	 * @return The response to the request
	 */
	private String[] nodeStatuses() {
		return shortStatuses("nodes", "icinga_hostgroups.alias IN ('Campus Nodes', 'Home Nodes')");
	}

	/**
	 * Process a 'Server Status' request
	 * @return The response to the request
	 */
	private String[] serverStatuses() {
		return shortStatuses("servers", "icinga_hostgroups.alias IN ('Core Servers', 'Dev Servers')");
	}

	/**
	 * Process a 'Campus Status' request
	 * @return The response to the request
	 */
	private String[] campusStatuses() {
		return shortStatuses("campus nodes", "icinga_hostgroups.alias = 'Campus Nodes'");
	}

	/**
	 * Process a 'Core Status' request
	 * @return The response to the request
	 */
	private String[] coreStatuses() {
		return shortStatuses("core nodes and servers", "icinga_hostgroups.alias IN ('Campus Nodes', 'Core Servers')");
	}

	/**
	 * Process a status request for a specified host
	 * @param matchString The string to use in matching the host's alias
	 * @return The response to the request
	 */
	private String[] specificStatus(String matchString) {
		String[] names;
		try
		{
			names = getStatuses(query + " WHERE icinga_hosts.alias LIKE '%" + matchString + "%'");
		}
		catch (IcingaException e)
		{
			return new String[] {e.getMessage()};
		}
		return names;
	}
	
	/**
	 * Get the statuses of the devices returned by executing a query
	 * @param queryString The query string to execute
	 * @return The response to the query, indicating name, status and last status change time
	 * @throws IcingaException
	 */
	public String[] getStatuses(String queryString) throws IcingaException
	{
		ResultSet rs;
		try
		{
			rs = dbc.ExecuteQuery(queryString);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new IcingaException("Unable to connect to database: Query error occurred");
		}
		ArrayList<String> results = new ArrayList<String>();
			
		try
		{
			while(rs.next())
			{
				String status = "unknown";
				String color = null;
				String normal = Colors.NORMAL;
				switch(rs.getInt("host_status"))
				{
				case 0:
					status = "UP";
					color = Colors.GREEN;
					break;
				case 1:
					status = "DOWN";
					color = Colors.RED;
					break;
				case 2:
					color = Colors.YELLOW;
					status = "UNREACHABLE";
					break;
				}
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

				results.add(rs.getString("host_name") + " has been " + color + status + normal + " since " +
						formatter.format(rs.getTimestamp("last_state_change")));
			}
		}
		catch (SQLException e)
		{
			throw new IcingaException("Unable to connect to database: Error processing query results");
		}
		String[] a = new String[0];
		return results.toArray(a);
	}
	
	/**
	 * Get the names of the devices returned by executing a query
	 * @param queryString
	 * @return The response to the query, indicating only the names of the devices
	 * @throws IcingaException
	 */
	public String[] getNames(String queryString) throws IcingaException
	{
		ResultSet rs;
		try
		{
			rs = dbc.ExecuteQuery(queryString);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new IcingaException("Unable to connect to database: Query error occurred");
		}
		ArrayList<String> results = new ArrayList<String>();
		try
		{
			while(rs.next())
			{
				results.add(rs.getString("host_name"));
			}	
		}
		catch (SQLException ex)
		{
			throw new IcingaException("Unable to connect to database: Error processing query results");
		}
		String[] a = new String[0];
		return results.toArray(a);
	}

	/**
	 * List the names of all of the known devices
	 * @return The names of all of the known devices, on a single line
	 */
	private String[] listKnownHosts() {
		String[] names;
		try {
			names = getNames(query);
		} catch (IcingaException e) {
			return new String[] {e.getMessage()};
		}
		String nameList = "Icinga knows about the following hosts: ";
		for(int i=0; i<names.length; i++)
		{
			nameList += names[i] + ", ";
		}
		nameList = nameList.substring(0, nameList.length()-2);
		return new String[] {nameList};
	}
	
	/**
	 * List the devices that are down, that match a specified criteria
	 * @param typeDescription The type of devices to select
	 * @param whereClause The where clause to use in selecting the devices
	 * @return A single line listing all of the devices that are down
	 */
	private String[] shortStatuses(String typeDescription, String whereClause) {
		String[] names;
		try {
			names = getNames(query + " WHERE " + whereClause + " AND icinga_hoststatus.current_state > 0");
		} catch (IcingaException e) {
			return new String[] {e.getMessage()};
		}
		if(names.length == 0)
		{
			return new String[] {"Icinga is pleased to report that all " + typeDescription + " are just fine"};
		}
		if(names.length == 1)
		{
			return new String[] {"Oh dear, " + names[0] + " seems to be down"};
		}
		String nameList = "Oh dear, the following seem to be down: ";
		for(int i=0; i<names.length; i++)
		{
			nameList += names[i] + ", ";
		}
		nameList = nameList.substring(0, nameList.length()-2);
		return new String[] {nameList};
	}

	public String processIncoming(List<String> inputLineWords) {
		String outputLine = "";
		if(inputLineWords.size() == 6)
		{
			String alerttype1 = inputLineWords.get(0);
			String alerttype2 = inputLineWords.get(1);
			String service = inputLineWords.get(2);
			String state = inputLineWords.get(3);
			int duration = Integer.parseInt(inputLineWords.get(4)) - 1;
			int sinceInt = Integer.parseInt(inputLineWords.get(5));
			Date since = new Date(sinceInt*1000);
			String stateColor = Colors.RED;
			String normalColor = Colors.NORMAL;
			if(state.equals("UP") || state.equals("OK"))
			{
				stateColor = Colors.GREEN;
				duration = 0;
			}
			else if(alerttype2.equals("ACKNOWLEDGEMENT"))
			{
				stateColor = Colors.BLUE;
			}
			String stateColored = stateColor + state + normalColor;
			String alerttype2Colored = stateColor + alerttype2 + normalColor;
			
			if(alerttype2.equals("ACKNOWLEDGEMENT"))
			{
				outputLine = alerttype2Colored += " of problem on " + service;
			}
			else
			{
				if(alerttype1.equals("HOST"))
				{
					if(duration == 0)
					{
						outputLine = alerttype2Colored + " alert - " + service + " " + stateColored;
					}
					else
					{
						alerttype2 = alerttype2.toLowerCase();
						if(duration == 1)
						{
							outputLine = "REMINDER of " + alerttype2 + " - " + service + " STILL " + stateColored + " (1 reminder since " + since + ")";
						}
						else
						{
							outputLine = "REMINDER of " + alerttype2 + " - " + service + " STILL " + stateColored + " (" + duration + " reminders since " + since + ")";
						}
					}
				}
				else if(alerttype1.equals("SERVICE"))
				{
					if(duration == 0)
					{
						outputLine = alerttype2Colored + " alert - " + service + " " + stateColored;
					}
					else
					{
						alerttype2 = alerttype2.toLowerCase();
						if(duration == 1)
						{
							outputLine = "REMINDER of " + alerttype2 + " - " + service + " STILL " + stateColored + " (1 reminder since " + since + ")";
						}
						else
						{
							outputLine = "REMINDER of " + alerttype2 + " - " + service + " STILL " + stateColored + " (" + duration + " reminders since " + since + ")";
						}
					}
				}
			}
		}
		else if(inputLineWords.size() == 1 && inputLineWords.get(0).equals("CONFIG_ERROR"))
		{
			outputLine = "Error in icinga config.  Icinga config out of date.";
		}
		else
		{
			outputLine = "Icinga has encountered a problem, but the config is wrong so nothing useful is being displayed here.  Sorry";
		}
		return outputLine;
	}
}
