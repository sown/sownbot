package uk.org.sown.sownbot.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.org.sown.sownbot.SownBot;

public class SendMessageAction extends SownBotAction {
	
	String message;
	String target;
	boolean censored;
	
	public SendMessageAction(String target, String message) {
		this(target, message, false);
	}
	
	public SendMessageAction(String target, String message, boolean censored) {
		this.target = target;
		this.message = message;
		this.censored = censored;
	}
	
	@Override
	public void execute(SownBot sownBot) {
		if (! censored)
			sownBot.sendLoggedMessage(target, message);
		else
			sownBot.sendCensoredMessage(target, message);
	}
	
	@Override
	public String toString() {
		return "SendMessageAction[\"" + message + " - to " + target + "\"]";
	}
	
	public static List<SendMessageAction> createMessages(String target, String[] msgs)
	{
		ArrayList<SendMessageAction> actions = new ArrayList<SendMessageAction>();
		for (String msg : msgs) {
			boolean censored = false;
			if (msg.startsWith("//")){
				censored = true;
				msg = msg.substring(2);
			}
			actions.add(new SendMessageAction(target, msg, censored));
		}
		return actions;
	}
	
	public static List<SendMessageAction> createMessages(String target, String[] msgs, boolean censored)
	{
		ArrayList<SendMessageAction> actions = new ArrayList<SendMessageAction>();
		for (String msg : msgs) {
			actions.add(new SendMessageAction(target, msg, censored));
		}
		return actions;
	}

	public static List<? extends SownBotAction> createSingleMessage(
			String target, String message) {
		return Arrays.asList(new SownBotAction[] { new SendMessageAction(target, message)});
	}

	public static List<? extends SownBotAction> createSingleMessage(
			String target, String message, boolean logged) {
		return Arrays.asList(new SownBotAction[] { new SendMessageAction(target, message, logged)});
	}

}
