package uk.org.sown.sownbot.module.events;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Event implements Comparable<Event>{

	public enum EventType { 
		MEETING ("Meeting"), 
		WORKSHOP ("Workshop");
		private final String name;
		EventType(String name)
		{
			this.name=name;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	
	protected Date date;
	protected Location location;
	protected EventType type;
	protected URL link;
	
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public Location getLocation() {
		return location;
	}
	public void setLocation(Location location) {
		this.location = location;
	}
	public EventType getType() {
		return type;
	}
	public void setType(EventType type) {
		this.type = type;
	}
	public URL getLink() {
		return link;
	}
	public void setLink(URL link) {
		this.link = link;
	}
	
	public String toString()
	{
		String out = this.type + " to be held on " +
			(new SimpleDateFormat("dd MMM yy 'at' h:mma").format(date))+
			" in the ";
		
		if (location instanceof Room)
		{
			Room room = (Room) location;
			out += room + " (" + room.getBuilding() + "/" + room.getRoom() + ")";
		}
		else
		{
			out += this.location;
		}
		
		return out;
	}
	
	public int compareTo(Event o) {
		int ct = 0;
		if((ct=8*date.compareTo(o.date))==0)
			if((ct+=4*type.compareTo(o.type))==0)
				if((ct+=location.compareTo(o.location))==0)
					return 0;
		return ct;			
		
	}
	
	
}
