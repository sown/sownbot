package uk.org.sown.sownbot;


public abstract class AbstractSownBotModule {

	protected SownBot bot;
	protected String moduleName;

	public AbstractSownBotModule(SownBot bot, String moduleName)
	{
		this.bot = bot;
		this.moduleName = moduleName;
	}
	
	/**
	 * Get the descriptive name of this module.
	 * 
	 * @return the descriptive name of this module
	 */
	public final String getModuleName()
	{
		return this.moduleName;
	}
	
}
