package uk.org.sown.sownbot.module;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jrdf.MemoryJRDFFactory;
import org.jrdf.graph.AnyObjectNode;
import org.jrdf.graph.AnyPredicateNode;
import org.jrdf.graph.AnySubjectNode;
import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.GraphElementFactoryException;
import org.jrdf.graph.Literal;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.Resource;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;
import org.jrdf.graph.local.URIReferenceImpl;
import org.jrdf.parser.RdfReader;
import org.jrdf.util.ClosableIterable;

import uk.org.sown.sownbot.InteractiveSownBotModule;
import uk.org.sown.sownbot.Message;
import uk.org.sown.sownbot.SownBot;
import uk.org.sown.sownbot.SownBotModuleException;
import uk.org.sown.sownbot.action.LogMessageAction;
import uk.org.sown.sownbot.action.SendMessageAction;
import uk.org.sown.sownbot.action.SownBotAction;
import uk.org.sown.sownbot.module.events.Event;
import uk.org.sown.sownbot.module.events.EventMonitor;
import uk.org.sown.sownbot.module.events.EventsException;
import uk.org.sown.sownbot.module.events.Location;
import uk.org.sown.sownbot.module.events.Room;
import uk.org.sown.sownbot.module.events.Event.EventType;


public class EventsModule extends InteractiveSownBotModule {

	enum EventCommand {
		list,
		update
	};
	
	private EventMonitor evm;
	private static final String moduleName = "Events";

	private static final String keyw_events = "events";
	private static final String cmd_list = "list";
	private static final String cmd_update = "update";
	
	
	private static final String[] usage_cmd_list = new String[] { "Usage: !events list" };
	private static final String[] usage_cmd_update = new String[] { "Usage: !events update" };
	
	private static final String[] keywords = new String[] { keyw_events };
	private static final String[] events_usage = new String[] {"!events list | update"};
	
	private static final String[][][] keywordUsage = new String[][][] {
		{events_usage, events_usage }};
	
	private static final String[][] commands = new String[][] { { cmd_list, cmd_update} };
	private static final String[][][][] commandUsage = new String[][][][] { {
		{usage_cmd_list, usage_cmd_list },
		{usage_cmd_update, usage_cmd_update }} };

	protected static String uri_prefix = "";
	protected static String wiki_prefix = "";
	protected static String resolver_prefix = "";
	protected static String export_prefix = "";
	protected static String index_prefix = "";
	protected static URI category_workshop_type_uri;
	protected static URI category_meeting_type_uri;
	protected static URI workshop_type_uri;
	protected static URI meeting_type_uri;
	protected static URI location_type_uri;
	protected static URI room_type_uri;
	protected static URI has_location_uri;
	protected static URI has_date_uri;
	protected static URI title_uri;
	
	protected static final URI rdf_type_uri      = URI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	protected static final URI rdfs_label_uri    = URI.create("http://www.w3.org/2000/01/rdf-schema#label");
	
	protected static final  SimpleDateFormat rdf_date_format = new SimpleDateFormat("yyyy'-'MM'-'dd'T'hh':'mm':'ss");
	
	public EventsModule(SownBot bot, String eventsUri)
	{
		super(bot, moduleName, keywords, keywordUsage, commands, commandUsage);
		uri_prefix = eventsUri;
		wiki_prefix = uri_prefix + "/wiki/";
		resolver_prefix = wiki_prefix + "Special:URIResolver/";
		export_prefix = wiki_prefix + "Special:ExportRDF/";
		index_prefix = uri_prefix + "/w/index.php";
		category_workshop_type_uri = URI.create(resolver_prefix + "Category-3AWorkshops");
		category_meeting_type_uri  = URI.create(resolver_prefix + "Category-3AMeetings");
		workshop_type_uri = URI.create(resolver_prefix + "Workshops");
		meeting_type_uri  = URI.create(resolver_prefix + "Meetings");
		location_type_uri = URI.create(resolver_prefix + "Category-3ALocation");
		room_type_uri     = URI.create(resolver_prefix + "Category-3ARoom");
		has_location_uri  = URI.create(resolver_prefix + "Property-3AHas_location");
		has_date_uri      = URI.create(resolver_prefix + "Property-3AHas_date");
		title_uri         = URI.create(resolver_prefix + "Property-3ATitle");
		evm=new EventMonitor(bot);
	}

	public List<? extends SownBotAction> processMessage(Message msg,
			String keyword, String command, String remainder)
			throws SownBotModuleException {

		
		
		ArrayList<SownBotAction> actions = new ArrayList<SownBotAction>();
		actions.add(new LogMessageAction(msg));
		
		EventCommand eventCommand = null;
		try {
			eventCommand = EventCommand.valueOf(command);
		} catch (IllegalArgumentException e) {}
		
		if(eventCommand != null)
		{
			switch(eventCommand)
			{
				case list:
					Collection<Event> events;
					try
					{
						events = getCurrentEvents();
					} catch (EventsException e)
					{
						System.err.println(new SimpleDateFormat().format(new Date()));
						System.err.println("Error retrieving meeting data.");
						e.printStackTrace();
						actions.addAll(SendMessageAction.createMessages(msg.getReplyTarget(), new String[] { "Error performing command", e.getMessage(), e.getCause().getMessage()}));
						break;
					}
					
					actions.addAll(SendMessageAction.createSingleMessage(msg.getReplyTarget(),"Forthcoming events in SOWN"));
					for(Event e: events)
						actions.addAll(SendMessageAction.createSingleMessage(msg.getReplyTarget(),e.toString()));
					break;
				case update:
					evm.interrupt();
					break;
			}
		}
		else
		{
			actions.addAll(sendUsage(msg, keyword));
		}
		
		return actions;
	}

	public static List<Event> getCurrentEvents() throws EventsException {
		return getEvents(getCurrentEventURIs());
	}
	
	public static ArrayList<URI> getCurrentEventURIs() throws EventsException
	{
		ArrayList<URI> out = new ArrayList<URI>();
		
		DateFormat df = new SimpleDateFormat("MMM+dd+yyyy");
		String now = df.format(new Date());

		String url = index_prefix + "?title=Special:Ask&q=[[Category:Events]]+[[Has+date::>" +
			now +
			"]]&po=?Has+date=date&eq=yes&p[format]=rdf&sort_num=&order_num=ASC&p[limit]=5&p[offset]=&p[link]=all&p[sort]=Has+date&p[order][asc]=1&p[headers]=show&p[mainlabel]=&p[intro]=&p[outro]=&p[searchlabel]=RDF&p[default]=&p[syntax]=rdfxml&eq=yes";
		URL meetingsURL = null;
		try {
			meetingsURL = new URL(url);
		} catch (MalformedURLException e) {
			// wont happen
		}

		RdfReader reader = new RdfReader();
		Graph meetingsGraph = null;
		try {
			meetingsGraph = reader.parseRdfXml(meetingsURL.openStream());
		} catch (IOException e) {
			throw new EventsException("Error retrieving RDF from " + meetingsURL,e);
		}

		GraphElementFactory elementFactory = meetingsGraph.getElementFactory();

		ObjectNode itemNode = elementFactory.createURIReference(URI
				.create("http://semantic-mediawiki.org/swivt/1.0#Subject"));
		ClosableIterable<Triple> itemTriples = meetingsGraph.find(
				AnySubjectNode.ANY_SUBJECT_NODE,
				AnyPredicateNode.ANY_PREDICATE_NODE, itemNode);

		try {
			for (Triple triple : itemTriples) {
				URIReferenceImpl uri = (URIReferenceImpl) triple.getSubject();
				out.add(
					URI.create(/*
						resolver_prefix +*/
						uri.getURI().toString()/*
						.replace(wiki_prefix, "")
						.replace(":", "-3A")*/));
			}
		} finally {
			itemTriples.iterator().close();
		}
		
		return out;
	}
	
	protected static Graph loadWikiGraph(Collection<URI> uris) throws EventsException
	{
		if (uris.size() == 0)
			return MemoryJRDFFactory.getFactory().getGraph();
		
		String identifiers = "";
		for (URI uri : uris)
			identifiers += uri.toString()
				.replace(resolver_prefix, "%0A")
				.replace("-3A", ":");
		
		String url = export_prefix + "?pages=" + identifiers.replaceAll("^%0A","");

		try {
			URL dataURL = new URL(url);
			return (new RdfReader()).parseRdfXml(dataURL.openStream());
		} catch (Exception e) {
			throw new EventsException("Error loading wiki graph from URL: " + url, e);
		}
	}
	
	protected static List<Event> getEvents(Collection<URI> uris) throws EventsException {
		
		HashMap<URI,Event> events = new HashMap<URI, Event>();
		HashSet<URI> locations = new HashSet<URI>();

		if (uris.size() == 0)
			return new ArrayList<Event>();
		
		Graph graph = loadWikiGraph(uris);

		GraphElementFactory elementFactory = graph.getElementFactory();
		Resource rdf_type;
		Resource has_location;
		Resource has_date;
		
		try {
			rdf_type = elementFactory.createResource(rdf_type_uri);
			has_location = elementFactory.createResource(has_location_uri);
			has_date = elementFactory.createResource(has_date_uri);
		} catch (GraphElementFactoryException e) {
			throw new RuntimeException("Failed to initialise URI constants", e);
		}
		
		for (URI uri : uris) {

			Resource uri_res = elementFactory.createResource(uri);

							
			for (Triple t: graph.find(uri_res, has_location, AnyObjectNode.ANY_OBJECT_NODE))
				locations.add(((URIReference) t.getObject()).getURI());

			for (Triple t: graph.find(uri_res, rdf_type, AnyObjectNode.ANY_OBJECT_NODE)) {
				URI type = ((URIReference) t.getObject()).getURI();
				
				if (type.equals(meeting_type_uri) || type.equals(workshop_type_uri) ||
					type.equals(category_meeting_type_uri) || type.equals(category_workshop_type_uri))
				{
					Event event = new Event();
					
					String date = findFirstLiteral(graph, uri_res, has_date);
					try {
						event.setDate(rdf_date_format.parse(date));
					} catch (ParseException e) {
						// Don't store this event, it has not date/time
						continue;
					}
					
					if (type.equals(meeting_type_uri) || type.equals(category_meeting_type_uri))
						event.setType(EventType.MEETING);
					else if (type.equals(workshop_type_uri) || type.equals(category_workshop_type_uri))
						event.setType(EventType.WORKSHOP);
				
					events.put(uri, event);
					continue;
				}
			}
		}
		
		Map<URI, Location> location_map = getLocationMetadata(locations);
		
		for (URI uri : events.keySet()) {
			Resource uri_res = elementFactory.createResource(uri);
			Event event = events.get(uri);
			
			for (Triple t: graph.find(uri_res, has_location, AnyObjectNode.ANY_OBJECT_NODE))
			{
				event.setLocation(location_map.get(((URIReference) t.getObject()).getURI()));
				break;
			}
		}
		
		ArrayList<Event> out = new ArrayList<Event>(events.values());
		Collections.sort(out);
		return out;
	}
	
	protected static Map<URI, Location> getLocationMetadata(Collection<URI> uris) throws EventsException {
		
		HashMap<URI, Location> output = new HashMap<URI, Location>();
		
		Graph graph = loadWikiGraph(uris);
		GraphElementFactory eFactory = graph.getElementFactory();
		
		Resource rdf_type;
		Resource rdfs_label;
		Resource title;
		try {
			rdf_type = eFactory.createResource(rdf_type_uri);
			title = eFactory.createResource(title_uri);
			rdfs_label = eFactory.createResource(rdfs_label_uri);
		} catch (GraphElementFactoryException e) {
			throw new RuntimeException("Failed to initialise URI constants", e);
		}
		
		for (URI uri : uris) {
			Resource uri_res = eFactory.createResource(uri);
			
			Class<? extends Location> location_class = null;
			for (Triple t: graph.find(uri_res, rdf_type, AnyObjectNode.ANY_OBJECT_NODE)) {
				URI type = ((URIReference) t.getObject()).getURI();
				
				if (type.equals(location_type_uri) && location_class == null)
				{
					location_class = Location.class;
				}
				else if (type.equals(room_type_uri))
				{
					location_class = Room.class;
					continue;
				}
			}
			
			if (location_class == Location.class) {
				String name = findFirstLiteral(graph, uri_res, title);
				output.put(uri, new Location(name));
			} else if (location_class == Room.class) {
				String name = findFirstLiteral(graph, uri_res, title);
				String label = findFirstLiteral(graph, uri_res, rdfs_label);
				String building = "", room = "";
				
				if (label != null)
				{
					String[] parts = label.split("/");
					building = parts[0];
					room = parts[1];
				}
				
				output.put(uri, new Room(name, building, room));
			}
		}
		
		return output;
	}
	
	protected static String findFirstLiteral(Graph graph, SubjectNode subj, PredicateNode pred)
	{
		for (Triple t : graph.find(subj, pred, AnyObjectNode.ANY_OBJECT_NODE)) {
			if (t.getObject() instanceof Literal)
				return ((Literal) t.getObject()).getLexicalForm(); 
		}
		return null;
	}
	
	
	public void start()
	{
		evm.start();
	}

	public static void main(String[] args) throws EventsException {
		ArrayList<URI> eventURIs = getCurrentEventURIs();
		Collection<Event> events = getEvents(eventURIs);
		
		System.out.println(events.size() + " Events.");
		for (Event event : events) {
			System.out.println(event);
		}
	}
}
