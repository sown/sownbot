package uk.org.sown.sownbot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jibble.pircbot.User;

import uk.org.sown.sownbot.action.LogMessageAction;
import uk.org.sown.sownbot.action.SendMessageAction;
import uk.org.sown.sownbot.action.SownBotAction;
import uk.org.sown.sownbot.module.AutoOpModule;
import uk.org.sown.sownbot.module.CensorModule;
import uk.org.sown.sownbot.module.GitStatusModule;
import uk.org.sown.sownbot.module.LoggerModule;
import uk.org.sown.sownbot.module.SVNStatusModule;
import uk.org.sown.sownbot.module.comms.CommunicationModule;
import uk.org.sown.sownbot.module.icinga.IcingaModule;
import uk.org.sown.sownbot.module.EventsModule;

public class SownBot extends UserInfoTrackingPircBot {

	static final int RETRY_CONNECT_LIMIT = 10;
	static final int RETRY_INTERVAL_SLEEP_MULTIPLE = 1000;
	
	private LoggerModule logger;
	private AutoOpModule autoOp;

	private String[] pIrcHostList;
	private String pBotName;
	private String pIrcChan;
	private String pBotNickPass;
	
	private Map<String, InteractiveSownBotModule> modulesByKeywords = new HashMap<String, InteractiveSownBotModule>();
	private Map<Class<AbstractSownBotModule>, AbstractSownBotModule> modulesByClass = new HashMap<Class<AbstractSownBotModule>, AbstractSownBotModule>();
	
	public String topic;
	
	public SownBot()
	{
		String filename = "sownbot.conf";
		Properties p = new Properties();
		try {
			p.loadFromXML(new FileInputStream(filename));
		} catch (InvalidPropertiesFormatException e) {
			System.out.println("Invalid formatting in " + filename);
			e.printStackTrace();
			System.exit(1);
		} catch (FileNotFoundException e) {
			System.out.println(filename + " not found");
			try {
				p = new Properties();
				p.put("bot-name", "SOWN-Bot-UNNAMED");
				p.put("bot-nick-pass", "CHANGEME");
                        p.put("irc-host", "irc.example.org");
                        p.put("irc-chan", "#sown");
				p.put("listen-port", "4444");
				p.put("log-db-host", "127.0.0.1");
				p.put("log-db-user", "sownbot");
				p.put("log-db-pass", "CHANGEME");
				p.put("log-db-name", "sownbot");
				p.put("autoop-db-host", "127.0.0.1");
				p.put("autoop-db-user", "sownbot");
				p.put("autoop-db-pass", "CHANGEME");
				p.put("autoop-db-name", "sownbot");
				p.storeToXML(new FileOutputStream(filename), "Default configuration");
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Unable to read " + filename);
			e.printStackTrace();
			System.exit(1);
		}
		ArrayList<String> errors = new ArrayList<String>();
		pBotName = getProperty(p, errors, "bot-name");
		pBotNickPass = getProperty(p, errors, "bot-nick-pass");
		String pLogDbHost = getProperty(p, errors, "log-db-host");
		String pLogDbName = getProperty(p, errors, "log-db-name");
		String pLogDbUser = getProperty(p, errors, "log-db-user");
		String pLogDbPass = getProperty(p, errors, "log-db-pass");
		String pAutoopDbHost = getProperty(p, errors, "autoop-db-host");
		String pAutoopDbName = getProperty(p, errors, "autoop-db-name");
		String pAutoopDbUser = getProperty(p, errors, "autoop-db-user");
		String pAutoopDbPass = getProperty(p, errors, "autoop-db-pass");
		String pSVNDbHost = getProperty(p, errors, "svn-db-host");
		String pSVNDbName = getProperty(p, errors, "svn-db-name");
		String pSVNDbUser = getProperty(p, errors, "svn-db-user");
		String pSVNDbPass = getProperty(p, errors, "svn-db-pass");
		String pGitDbHost = getProperty(p, errors, "git-db-host");
            String pGitDbName = getProperty(p, errors, "git-db-name");
            String pGitDbUser = getProperty(p, errors, "git-db-user");
            String pGitDbPass = getProperty(p, errors, "git-db-pass");
		String pEventsUri = getProperty(p, errors, "events-uri");
		String pIcingaDbHost = getProperty(p, errors, "icinga-db-host");
            String pIcingaDbName = getProperty(p, errors, "icinga-db-name");
            String pIcingaDbUser = getProperty(p, errors, "icinga-db-user");
            String pIcingaDbPass = getProperty(p, errors, "icinga-db-pass");
            String pIcingaApiUri = getProperty(p, errors, "icinga-api-uri");
		
		String pListenPort = getProperty(p, errors, "listen-port");
		
		String hostList = getProperty(p, errors, "irc-host");
		if (hostList != null)
		pIrcHostList = getProperty(p, errors, "irc-host").split(",");
		
		pIrcChan = getProperty(p, errors, "irc-chan");
		int pListenPortInt = 0;
		try
		{
			pListenPortInt = Integer.parseInt(pListenPort);
		}
		catch(NumberFormatException ex)
		{
			errors.add("listen-port");
		}
		
		if(errors.size() > 0)
		{
			System.out.println(errors.size() + " missing or invalid properties in " + filename);
			for(int i=0; i<errors.size(); i++)
			{
				System.out.println(errors.get(i));
			}
			System.exit(1);
		}
			
		this.setName(pBotName);
		this.setLogin(pBotName);
		this.setAutoNickChange(true);
		try {
			this.setEncoding("UTF8");
		} catch (UnsupportedEncodingException e){
			e.printStackTrace();
		}

		logger = new LoggerModule(this, pLogDbHost, pLogDbName, pLogDbUser, pLogDbPass);
		autoOp = new AutoOpModule(this, pAutoopDbHost, pAutoopDbName, pAutoopDbUser, pAutoopDbPass);
		
		installModule(logger);
		installModule(new IcingaModule(this, pIcingaDbHost, pIcingaDbName, pIcingaDbUser, pIcingaDbPass, pIcingaApiUri));
		installModule(new CommunicationModule(this, pListenPortInt));
		installModule(autoOp);
		installModule(new CensorModule(this, pLogDbHost, pLogDbName, pLogDbUser, pLogDbPass));
		installModule(new SVNStatusModule(this, pSVNDbHost, pSVNDbName, pSVNDbUser, pSVNDbPass));
		installModule(new GitStatusModule(this, pGitDbHost, pGitDbName, pGitDbUser, pGitDbPass));
		installModule(new EventsModule(this, pEventsUri));		
		getModuleByClass(CommunicationModule.class).start();
		getModuleByClass(EventsModule.class).start();
		
		// Enable debugging output. prints timestamp + msg to stdout and exceptions if it catches them
		setVerbose(true);
		
		// Connect to the IRC server.
		this.doConnect();
	}
	
	@SuppressWarnings("unchecked")
	private void installModule(AbstractSownBotModule module) {
		Class<AbstractSownBotModule> clazz = (Class<AbstractSownBotModule>) module.getClass();
		
		if (modulesByClass.containsKey(clazz))
			throw new RuntimeException("Cannot install 2 modules of the same type! ("
					+ clazz.getName() + ").");
		
		modulesByClass.put(clazz, module);

		if (InteractiveSownBotModule.class.isAssignableFrom(clazz))
		{
			InteractiveSownBotModule iMod = (InteractiveSownBotModule) module;
			
			String[] keywords = iMod.getModuleKeywords();
			for (String keyword : keywords)
			{
				AbstractSownBotModule prev = modulesByKeywords.put(keyword, iMod);
				if (prev != null)
					System.err.println("Warning, 2 modules trying to use the same keyword (" + keyword + ") - "
							+ module.getClass().getName() + " and " + prev.getClass().getName());
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getModuleByClass(Class<T> clazz)
	{
		return (T) modulesByClass.get(clazz);
	}
	
	private String getProperty(Properties p, ArrayList<String> errors,
			String key) {
		String propertyString = p.getProperty(key);
		if(propertyString == null)
			errors.add(key);
		return propertyString;
	}

	public boolean isOp(String channel, String sender)
	{
		for (User user : this.getUsers(channel)) {
			if (user.getNick().equals(sender))
				return user.isOp();
		}
		return false;
	}
	
	public void onAction(String sender, String login, String hostname, String target, String action)
	{
		super.onAction(sender, login, hostname, target, action);
		
		logger.logAction(target, sender, login, hostname, action);
	}
	
	public void onJoin(String channel, String sender, String login, String hostname)
	{
		super.onJoin(channel, sender, login, hostname);
		
		if(autoOp.shouldBeOp(channel, sender, login, hostname) && this.isOp(channel, this.getNick()))
			this.op(channel, sender);

		logger.logMember(channel, sender, login, hostname, "JOIN");
	}
	
	public void onPart(String channel, String sender, String login, String hostname)
	{
		super.onPart(channel, sender, login, hostname);

		logger.logMember(channel, sender, login, hostname, "PART");
	}
	
	public void onQuit(String sender, String login, String hostname, String reason)
	{
		super.onQuit(sender, login, hostname, reason);
		
		logger.logMember("", sender, login, hostname, "QUIT " + reason);
	}
	
	public void onKick(String channel, String sender, String login, String hostname, String recipient, String reason)
	{
		super.onKick(channel, sender, login, hostname, recipient, reason);

		logger.logMember(channel, sender, login, hostname, "KICK " + recipient + ": " + reason);
	}
	
	public void onNickChange(String oldNick, String login, String hostname, String newNick)
	{
		super.onNickChange(oldNick, login, hostname, newNick);

		logger.logMember("", oldNick, login, hostname, "NICK " + newNick);
		
		for (String channel : this.getChannels()) {
			for (User user : this.getUsers(channel)) {
				if (user.getNick() == newNick){
					if(autoOp.shouldBeOp(channel, newNick, login, hostname))
						this.op(channel, newNick);			
				}
			}
		}
	}
	
	public void onTopic(String channel, String topic, String sender, long date, boolean changed)
	{
		if(changed)
		{
			logger.logTopic(channel, sender, "", "", topic);
		}
		this.topic=topic;
	}
	
	public void onOp(String channel, String sender, String login, String hostname, String recipient)
	{
		logger.logMember(channel, sender, login, hostname, "MODE +o " + recipient);
		
		// Op everyone who we think should be opped.
		if (recipient.equals(this.getNick()))
			autoOp.opAll(channel);
	}
	
	public void onDeop(String channel, String sender, String login, String hostname, String recipient)
	{
		logger.logMember(channel, sender, login, hostname, "MODE -o " + recipient);
	}
	
	public void onVoice(String channel, String sender, String login, String hostname, String recipient)
	{
		logger.logMember(channel, sender, login, hostname, "MODE +v " + recipient);
	}
	
	public void onDeVoice(String channel, String sender, String login, String hostname, String recipient)
	{
		logger.logMember(channel, sender, login, hostname, "MODE -v " + recipient);
	}
	
	public void onUserMode(String who, String sender, String login, String hostname, String mode)
	{
		logger.logMember("", sender, login, hostname, "MODE " + mode + " " + who);
	}
	
	public void onPrivateMessage(String sender, String login, String hostname, String message)
	{
		Message msg = new Message(sender, this.getNick(), login, hostname, message);
		
		onGenericMessage(msg);
	}
	
	public void onMessage(String channel, String sender, String login, String hostname, String message)
	{
		Message msg = new Message(sender, channel, login, hostname, message);
		
		onGenericMessage(msg);
	}
	
	public void sendLoggedMessage(String target, String message)
	{
		sendMessage(target, message);

		logger.logMessage(new Message(this.getNick(), target, this.getLogin(), "", message));
	}
	
	public void sendCensoredMessage(String target, String message)
	{
		sendMessage(target, message);

		logger.logMessage(new Message(this.getNick(), target, this.getLogin(), "", message), true);
	}
	
	private void onGenericMessage(Message msg)
	{	
		ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
		
		if(msg.message.charAt(0) != '!')
		{
			actions.add(new LogMessageAction(msg));
		}
		else if(msg.sender.equals(this.getNick()))
		{
			actions.add(new LogMessageAction(msg));
		}
		else
		{
			String[] parts = msg.message.substring(1).split(" ", 2);
			String keyword = parts[0].toLowerCase();
			String rest = parts.length == 2 ? parts[1] : null;
			
			if (keyword.equals("")) {
				actions.add(new LogMessageAction(msg));
			} else if (keyword.equals("help")) {
				try {
					actions.addAll(onHelpMessage(msg, rest));
				} catch (SownBotModuleException e) {
					sendMessage(msg.getReplyTarget(), "Error: " + e.getMessage());
					e.printStackTrace();
				}
			} else {
				InteractiveSownBotModule module = modulesByKeywords.get(keyword);
				
				if (module != null) {
					try
					{
						Collection<? extends SownBotAction> act = module.processMessage(msg, keyword, rest);
						
						if (act != null)
							actions.addAll(act);
					}
					catch (SownBotModuleException e)
					{
						sendMessage(msg.getReplyTarget(), "Error: " + e.getMessage());
						System.err.println(new SimpleDateFormat().format(new Date()));
						e.printStackTrace();
					} 
					catch (Exception e)
					{
						System.err.println(new SimpleDateFormat().format(new Date()));
						e.printStackTrace();
					}
				} else {
					actions.add(new LogMessageAction(msg));
					actions.add(new SendMessageAction(msg.target, "'" + keyword + "' not a registered command. Respond '!help' for available commands."));
				}
			}
		}
		
		try {
			for (SownBotAction action : actions)
				action.execute(this);
		} catch (Exception e)
		{
			sendMessage(msg.getReplyTarget(), "Error processing actions: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void logMessage(Message msg, boolean censored) {
		try {
			logger.logMessage(msg, censored);
		} catch (Exception e) {
			e.printStackTrace();
			sendMessage(msg.getReplyTarget(), "Unable to log message!");
		}
	}
	
	private List<? extends SownBotAction> onHelpMessage(Message msg, String rest)
			throws SownBotModuleException
	{
		ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
		
		if (rest == null || rest.trim().equals("")) {
			actions.add(new LogMessageAction(msg));
			
			String body = "Available keywords:";
			for (String key : this.modulesByKeywords.keySet())
				body += ' ' + key + ',';
			body += " help";
			actions.add(new SendMessageAction(msg.getReplyTarget(), body + '.'));
			
			body = "Installed modules:";
			for (AbstractSownBotModule mod : this.modulesByClass.values()) {
				body += " " + mod.getModuleName();
			}
			actions.add(new SendMessageAction(msg.getReplyTarget(), body + '.'));
		} else {
			String[] parts = rest.split(" ");
			String keyword = parts[0];
			String remainder = parts.length == 2 ? parts[1] : null;
			
			InteractiveSownBotModule mod = modulesByKeywords.get(keyword);
			if (mod != null)
				actions.addAll(mod.processHelpMessage(msg, keyword, remainder));
			else
				actions.add(new SendMessageAction(msg.getReplyTarget(), "No usage information for that keyword."));
		}
		return actions;
	}
	
	/**
	 * Set the main channel of the bot, which the bot should contact by default
	 * @param channel The main channel of the bot
	 */
	public void mainChannel(String channel)
	{
		CommunicationModule mod = getModuleByClass(CommunicationModule.class);
		if (mod != null)
			mod.mainChannel(channel);
	}
	
	@Override
	protected void onDisconnect() {
		super.onDisconnect();
		
		this.doConnect();
	}
	
	private void doConnect() {
		while(! this.isConnected()) {
			
			for (int i = 0; i < this.pIrcHostList.length; i++) {
				this.setName(this.pBotName);
				String host = this.pIrcHostList[i];
				
				for (int j = 0; j < RETRY_CONNECT_LIMIT; j++) {
					// This is the future! (alternate the future)
					System.setProperty("java.net.preferIPv6Addresses", new Boolean(j % 2 == 0).toString());
					System.out.println("Connecting to host '" + host + "'. Attempt " + (j +1));
					
					try {
						connect(host);
						break;
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println("Connect failed. Sleeping for " + (RETRY_INTERVAL_SLEEP_MULTIPLE * (j+1)) + "ms.");
					try
					{
						Thread.sleep(RETRY_INTERVAL_SLEEP_MULTIPLE * (j+1));
					} catch (InterruptedException e)
					{}
				}
				
				if (this.isConnected()) {
					// Identify with NickServ once connected
					this.identify(this.pBotNickPass);
					break;
				}
			}
			
		}
		
		joinChannel(this.pIrcChan);
		mainChannel(this.pIrcChan);
	}
}
