package uk.org.sown.sownbot.module.events;

import uk.org.sown.sownbot.module.events.Event;
import uk.org.sown.sownbot.module.EventsModule;
import uk.org.sown.sownbot.SownBot;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
public class EventMonitor extends Thread {

	private SownBot bot;
	private GregorianCalendar c;
	private int expbackoff = 1;

	public EventMonitor(SownBot bot) {
		this.bot = bot;
	}

	public void run() {
		while(true) {
			try {
				while(!bot.isConnected())
				{
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e2) {
						//just in case
					}
				}
				
				List<Event> events = EventsModule.getCurrentEvents();
				
				String topic = "";
				
				if (events.size() > 0)
				{
					ArrayList<Event> upcoming = new ArrayList<Event>();
		
					Event meet = null;
					Event shop = null;
					
					for (Event e : events) {
						switch (e.getType()) {
						case MEETING:
							if (meet == null)
							{
								meet = e;
								upcoming.add(e);
							}
							break;
						case WORKSHOP:
							if (shop == null)
							{
								shop = e;
								upcoming.add(e);
							}
							break;
						}
						if (shop != null && meet != null)
							break;
					}
				
					
					DateFormat df = new SimpleDateFormat("yyyy-MM-DD");
					String today = df.format(new Date());
					
					for (Event event : upcoming) {
						Date event_date = event.getDate();
						Calendar event_calendar = GregorianCalendar.getInstance();
						event_calendar.setTime(event_date);
						event_calendar.get(Calendar.MINUTE);
						String hourFormat = (event_calendar.get(Calendar.MINUTE) == 0) ? "ha" : "h:mma"; 
		
						if (! topic.equals(""))
							topic += ", ";
						
						String time = new SimpleDateFormat(hourFormat).format(event_date);
						
						if (time.equals("12AM"))
							time = "12 Noon";
						else if (time.equals("12PM"))
							time = "12 Midnight";
						
						topic += "Next " + event.getType() + ": "
							+ (today.equals(df.format(event_date)) ?
								"Today @ " + time
								: new SimpleDateFormat("d MMM '@' ").format(event_date) + time)
							+ " " + event.location.toAbbr();
					}
					
					topic = topic.replaceAll(", $","");
					if (topic.length() == 0)
					{
						topic = "No events scheduled";
					}
				}
				else
				{
					topic = "No events scheduled";
				}
				
				if (topic.length() > 0)
				{
					ArrayList<String> newtopic = new ArrayList<String>();
					newtopic.add(topic);
					if (bot.topic != null && bot.topic.length() > 0)
					{
						String[] topicparts = bot.topic.split("\\|");
                                    for(String topicpart : topicparts)
						{
							topicpart = topicpart.trim();
                                          if(!topicpart.matches("(Next Meeting:|Next Workshop:|No events scheduled).*"))
							{
								newtopic.add(topicpart);
							}
						}
					}
					String newtopicstring = new String();
					for (String  newtopicbit : newtopic) 
					{
						if (! newtopicstring.equals("")) 
                                          newtopicstring += " | ";
						newtopicstring += newtopicbit;	
					}
					

					//if (bot.topic != null && bot.topic.length() > 0 && !newtopic.equals(bot.topic) && !bot.topic.matches("(Next Meeting:|Next Workshop:|No events scheduled).*"))
					//	newtopic = newtopic + " | " + bot.topic.replaceAll("[^|]*\\| *(.*)", "$1");
					
					if(!newtopicstring.equals(bot.topic))
						bot.setTopic("#sown",newtopicstring);
				}
				try {
					Thread.sleep(300000);
				} catch (InterruptedException e1) {
				}
				expbackoff = 1;
			}
			catch (Exception e)
			{
				System.err.println(new SimpleDateFormat().format(new Date()));
				e.printStackTrace();
				try {
					Thread.sleep(expbackoff*60000);
				}
				catch (InterruptedException ie) {}
				if (expbackoff < 16)
                              expbackoff = expbackoff * expbackoff;
			}
		}
	}
}
