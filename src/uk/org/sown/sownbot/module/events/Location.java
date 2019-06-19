package uk.org.sown.sownbot.module.events;

public class Location implements Comparable<Location>{
	
	protected String name;
	
	public Location (String name)
	{
		this.name=name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int compareTo(Location o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return name;
	}

	public String toAbbr() {
		return name;
	}
	
	
}
