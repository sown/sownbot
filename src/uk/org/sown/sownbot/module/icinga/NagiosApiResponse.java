package uk.org.sown.sownbot.module.icinga;

public class NagiosApiResponse {

	protected boolean success;
      protected String response;

      public NagiosApiResponse (boolean success, String response)
      {
            this.success=success;
		this.response=response;
      }

	public boolean getSuccess() {
            return success;
      }

      public String getResponse() {
            return response;
      }

}
