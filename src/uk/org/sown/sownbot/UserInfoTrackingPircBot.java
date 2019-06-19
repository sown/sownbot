package uk.org.sown.sownbot;

import java.util.HashMap;
import java.util.Map;

import org.jibble.pircbot.PircBot;

public class UserInfoTrackingPircBot extends PircBot {

	private Map<String, String[]> _userDetailsMap = new HashMap<String, String[]>();
	
	public final String getUserLogin(String nick) {
		String[] data = _userDetailsMap.get(nick);
		return data == null ? null : data[0];
	}
	
	public final String getUserHostname(String nick) {
		String[] data = _userDetailsMap.get(nick);
		return data == null ? null : data[1];
	}
	
	// Call this to ask for the hostnames and logins of all users in a channel
    public final void requestChannelUsers(String channel) {
		this.sendRawLineViaQueue("WHO +c " + channel);
	}


    @Override
    protected void onServerResponse(int code, String response) {
    	if (code == RPL_WHOREPLY)
    	{
    		String[] parts = response.split(" ");
    		String nick = parts[5];
    		String login = parts[2];
    		String hostname = parts[3];

    		_userDetailsMap.put(nick, new String[] {login, hostname});
    	}
    }
    
    @Override
    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
    	_userDetailsMap.remove(oldNick);
		_userDetailsMap.put(newNick, new String[] {login, hostname});
    }
    
    @Override
    protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
    	_userDetailsMap.remove(sourceNick);
    }
    
    @Override
    protected void onJoin(String channel, String sender, String login, String hostname) {
		_userDetailsMap.put(sender, new String[] {login, hostname});
		if (sender == this.getNick())
			this.requestChannelUsers(channel);
    }
    
    @Override
    protected void onPart(String channel, String sender, String login, String hostname) {
    	_userDetailsMap.remove(sender);
    }
    
    @Override
    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
    	_userDetailsMap.remove(recipientNick);
    }
    
    @Override
    protected void onDisconnect() {
    	_userDetailsMap = new HashMap<String, String[]>();
    }
}
