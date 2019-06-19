package uk.org.sown.sownbot.module.events;

public class Room extends Location {

	protected String building;
	protected String room; 
	
	public Room(String name, String building, String room) {
		super(name);
		this.building = building;
		this.room = room;
	}

	public String getBuilding() {
		return building;
	}

	public void setBuilding(String building) {
		this.building = building;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}
	
	@Override
	public String toString() {
		if (this.name == null || this.name.equals(""))
			return toAbbr();
		else
			return super.toString();
	}
	
	@Override
	public String toAbbr() {
		return building + "/" + room;
	}

	@Override
	public int compareTo(Location o) {
		if (o instanceof Room)
			return this.compareTo((Room) o);
			
		return -1;
	}
	
	public int compareTo(Room o) {
		return building.compareTo(o.building)*2 + room.compareTo(o.room);
	}
}
