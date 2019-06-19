package uk.org.sown.sownbot.action;

import uk.org.sown.sownbot.Message;
import uk.org.sown.sownbot.SownBot;

public class LogMessageAction extends SownBotAction {

	Message message;
	boolean censored;
	
	public LogMessageAction(Message msg) {
		this(msg, false);
	}

	public LogMessageAction(Message msg, boolean censored) {
		this.message = msg;
		this.censored = censored;
	}
	
	@Override
	public void execute(SownBot sownBot) {
		sownBot.logMessage(this.message, this.censored);
	}
	
	@Override
	public String toString() {
		return "LogMessageAction[\"" + message.toString() + "\"]";
	}
}
