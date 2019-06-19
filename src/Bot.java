import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import uk.org.sown.sownbot.SownBot;

public class Bot {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Starting sownbot ("+ new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(GregorianCalendar.getInstance().getTime()) + ")");
		new SownBot();
	}
}
