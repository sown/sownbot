package uk.org.sown.sownbot.module.icinga;

public class IcingaException extends Exception {

	private static final long serialVersionUID = 2620137061608583318L;
	private String message;
	
	public IcingaException(String message)
	{
		this.message = message;
	}
	
	public String getMessage()
	{
		return message;
	}
}
