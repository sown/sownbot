package uk.org.sown.sownbot;

public class SownBotModuleException extends Exception {

	private static final long serialVersionUID = -801980145359614124L;

	public SownBotModuleException(String message)
	{
		super(message);
	}

	public SownBotModuleException(String message, Exception e)
	{
		super(message, e);
	}

}