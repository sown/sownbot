package uk.org.sown.sownbot;

public class Message {

	// Code is cleaner if these are public.
	// please don't do nasty things with them. 
	public String sender;
	public String target;
	public String userLogin;
	public String userHostname;
	public String message;

	public Message(String sender, String target, String userLogin, String userHostname, String message) {
		this.sender = sender;
		this.target = target;
		this.userLogin = userLogin;
		this.userHostname = userHostname;
		this.message = message;
	}

	public boolean isChannelMessage()
	{
		return target.charAt(0) == '#';
	}
	
	public String getReplyTarget()
	{
		if (isChannelMessage())
			return target;
		else
			return sender;
	}
	
	@Override
	public String toString() {
		return "Message[" + target + " " + sender + " \"" + message + "\" " + userLogin + "@" + userHostname + "]"; 
	}
}
