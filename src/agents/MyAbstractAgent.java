package agents;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;

import message.ColeagueInformation;
import message.MyMessage;
import message.RetainedInformation;
import message.Serializer;
import message.TokenInformation;

import rescuecore2.Constants;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.misc.geometry.GeometryTools2D;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.kernel.comms.StandardCommunicationModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;


import sample.SampleSearch;
/**
Abstract base class for MyAgent.
@param <E> The subclass of StandardEntity this agent wants to control.
*/
public abstract class MyAbstractAgent<E extends StandardEntity> extends StandardAgent<E> {
	
	private static final int RANDOM_WALK_LENGTH = 50;

    private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class.getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class.getName();
    protected static final Double THRESHOLD = 0.5;
    private int communicationChannel;
    /**
       The search algorithm.
       Default from Sample module
       take all the world and makes a bread first search
    */
    protected SampleSearch search;

    /**
       Whether to use AKSpeak messages or not.
    */
    protected boolean useSpeak;

    /**
       Cache of building IDs.
    */
    protected List<EntityID> buildingIDs;

    /**
       Cache of road IDs.
    */
    protected List<EntityID> roadIDs;

    /**
       Cache of refuge IDs.
    */
    protected List<EntityID> refugeIDs;

    private Map<EntityID, Set<EntityID>> neighbours;
    protected boolean	channelComm;
    
    private ArrayList<Integer> coleagues = new ArrayList<Integer>();
    private ArrayList<EntityID> otherJobs = new ArrayList<EntityID>();
    private List<TokenInformation> value = new ArrayList<TokenInformation>();
    private ArrayList<TokenInformation> potentialValue = new ArrayList<TokenInformation>();
    private ArrayList<MyMessage> receivedMessages = new ArrayList<MyMessage>();
    private ArrayList<RetainedInformation> retained = new ArrayList<RetainedInformation>();
    protected ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> map;
    private ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> kruskalMap;
    /*
     * Constructor of MyAbstractAgent
     */
    protected MyAbstractAgent(){}
    
    /**
     * Connects the agent to the simulation
     */
    @Override
    protected void postConnect() {
        super.postConnect();
        //creates arrays list for buildings, roads and refuges of the world model
        
        buildingIDs = new ArrayList<EntityID>();
        //roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        /*
        //assign values to buildings, roads and refuges according to model
        for (StandardEntity next : model) {
            if (next instanceof Building) {
                buildingIDs.add(next.getID());
            }
            if (next instanceof Road) {
                roadIDs.add(next.getID());
            }
            if (next instanceof Refuge) {
                refugeIDs.add(next.getID());
            }
        }*/
        
        this.map = this.worldGraph();
        this.kruskalMap = this.minimalSpanningTree(this.map);
        
        /**
         * sets communication via radio
         */
        boolean speakComm = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(ChannelCommunicationModel.class.getName());

        int numChannels = this.config.getIntValue("comms.channels.count");
        
        if((speakComm) && (numChannels > 1)){
        	this.channelComm = true;
        }else{
        	this.channelComm = false;
        }
        
        /*
         *  Instantiate a new SampleSearch
         *  Sample Search creates a graph for the world model
         *  and implements a bread first search for use as well.
         */
        search = new SampleSearch(model);
        // assign graph of world model made by SampleSearch to neighbours
        neighbours = search.getGraph();
        
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Modelo de Comunicação: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Usando modelo SPEAK" : "Usando modelo SAY");
    }
    
    /**
    Construct a random walk starting from this agent's current location to a random building.
    @return A random walk.
     */
    protected List<EntityID> randomWalk() {
    	//creates a array list of Entity as larger as RANDOM_WALK_LENGTH value
    	List<EntityID> result = new ArrayList<EntityID>(RANDOM_WALK_LENGTH);
    	//array list of Entities which was visited
    	Set<EntityID> seen = new HashSet<EntityID>();
    	//current position (EntityID) of the agent
    	EntityID current = ((Human)me()).getPosition();
    	for (int i = 0; i < RANDOM_WALK_LENGTH; ++i) {
    		//add the current position to result and seen array list
    		result.add(current);
    		seen.add(current);
    		//any EntityID from the current position of agent
    		List<EntityID> possible = new ArrayList<EntityID>(neighbours.get(current));
    		//shuffle Entities ID's order in possible array list
    		Collections.shuffle(possible, random);
    		boolean found = false;
    		//tries to get some possible EntityID
    		for (EntityID next : possible) {
    			//if the EntityID was seen then keep searching
    			if (seen.contains(next)) {
    				continue;
    			}
    			//otherwise
    			// breaks the loop and return result array list
    			current = next;
    			found = true;
    			break;
    		}
    		//if found is false, it means that there's no possible EntityID
    		//and we reached a dead-end.
    		if (!found) {
    			break;
    		}
    	}
    	return result;
    }
    
    protected ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> worldGraph()
    {
    	ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> g = new ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge>(DefaultWeightedEdge.class);
    	for(StandardEntity next : model)
    	{
    		if(next instanceof Road)
    		{
    			Road b = (Road)next;
    			Double size = GeometryTools2D.computeArea(GeometryTools2D.vertexArrayToPoints(b.getApexList()));
    			if(g.containsVertex(b.getID()) == false && (b.getNeighbours().size() >= 1)){
    				g.addVertex(b.getID());
    				List<EntityID> neighbours = b.getNeighbours();
    				for(EntityID n : neighbours)
    				{
    					if(g.containsVertex(n)){
    						Area tmp = (Area)model.getEntity(n);
    						Double sizeTmp = GeometryTools2D.computeArea(GeometryTools2D.vertexArrayToPoints(tmp.getApexList()));
    						g.addEdge(n, b.getID());
    						g.setEdgeWeight(g.getEdge(n, b.getID()), (sizeTmp * size));
    					}
    				}
    			}
    		}
    		if(next instanceof Building)
    		{
    			Building b = (Building)next;
    			buildingIDs.add(b.getID());
    			Double size = GeometryTools2D.computeArea(GeometryTools2D.vertexArrayToPoints(b.getApexList()));
    			if(g.containsVertex(b.getID()) == false && (b.getNeighbours().size() >= 1)){
    				g.addVertex(b.getID());
    				List<EntityID> neighbours = b.getNeighbours();
    				for(EntityID n : neighbours)
    				{
    					if(g.containsVertex(n)){
    						Area tmp = (Area)model.getEntity(n);
    						Double sizeTmp = GeometryTools2D.computeArea(GeometryTools2D.vertexArrayToPoints(tmp.getApexList()));
    						g.addEdge(n, b.getID());
    						g.setEdgeWeight(g.getEdge(n, b.getID()), (sizeTmp * size));
    					}
    				}
    			}
    		}
    		if(next instanceof Refuge)
    		{
    			Refuge b = (Refuge)next;
    			refugeIDs.add(b.getID());
    			Double size = GeometryTools2D.computeArea(GeometryTools2D.vertexArrayToPoints(b.getApexList()));
    			if(g.containsVertex(b.getID()) == false && (b.getNeighbours().size() >= 1)){
    				g.addVertex(b.getID());
    				List<EntityID> neighbours = b.getNeighbours();
    				for(EntityID n : neighbours)
    				{
    					if(g.containsVertex(n)){
    						Area tmp = (Area)model.getEntity(n);
    						Double sizeTmp = GeometryTools2D.computeArea(GeometryTools2D.vertexArrayToPoints(tmp.getApexList()));
    						g.addEdge(n, b.getID());
    						g.setEdgeWeight(g.getEdge(n, b.getID()), (sizeTmp * size));
    					}
    				}
    			}
    			
    		}
    		
    	}
    	return g;
    	
    }
    
    /**
     * 
     * @param targetPosition EntityID do alvo
     * @return retorna objetos em um raio de 400??? do alvo mencionado
     */
    protected Collection<StandardEntity> tasksInRange(EntityID targetPosition)
    {
    	Collection<StandardEntity> targets = model.getObjectsInRange(targetPosition, 400);
    	return targets;
    }
    
    /**
     * 
     * @param targetPosition EntityID do alvo
     * @param range distancia maxima
     * @return retorna objetos dentro de um raio informado
     */
    protected Collection<StandardEntity> tasksInRange(EntityID targetPosition, int range)
    {
    	Collection<StandardEntity> targets = model.getObjectsInRange(targetPosition, range);
    	return targets;
    }
    
    /**
     * 
     * @param sourcePosition - location of the agent
     * @param targetPosition - location of the target 
     * @param range
     * @return
     */
    protected List<EntityID> planPathToTask(EntityID sourcePosition, EntityID targetPosition, int range) {
        
        Collection<StandardEntity> targets = model.getObjectsInRange(targetPosition, range);
        if (targets.isEmpty()) {
            return null;
        }
        List<EntityID> shortestPath = new ArrayList<EntityID>();
        for(EntityID t : objectsToIDs(targets))
        {
        	List<EntityID> path = this.getDijkstraPath(sourcePosition, t);
        	if(shortestPath.isEmpty())
        	{
        		shortestPath = path;
        	}else{
        		if(path.size() < shortestPath.size())
        		{
        			shortestPath = path;
        		}
        	}
        }
        return shortestPath;
    }
    
    /**
     * 
     * @param target - EntityID of a StandardEntity of the world model
     * @return
     */
    public Double computeCapability(EntityID target){
		Double distanceWorldModel = 0.0;
		Integer distanceOfTarget = 0;
		Integer x,y = 0;
		Double cap = 0.0;
		x = model.getWorldBounds().second().first() - model.getWorldBounds().first().first();
		y = model.getWorldBounds().second().second() - model.getWorldBounds().first().second();
		distanceWorldModel = Math.sqrt((Double)Math.pow(x, 2) + (Double)Math.pow(y, 2));
		distanceOfTarget = model.getDistance(me().getID(), target);
		cap = 1 - (distanceOfTarget.doubleValue() / distanceWorldModel);
		return cap;
	}
    
    
    public ArrayList<Integer> getColeagues() {
		return coleagues;
	}

	public void setColeagues(ArrayList<Integer> coleagues) {
		this.coleagues = coleagues;
	}
	
	protected void sendmessage(int time, int channel, ArrayList<TokenInformation> message)
	{
		byte[] speak = null;
        try {
            speak = Serializer.serialize(message);
            sendSpeak(time, channel, speak);
//            System.out.println("Mensagem de enviada com sucesso");
            Logger.debug("Mensagem enviado com sucesso");
        } catch (IOException e) {
        	e.printStackTrace();
            Logger.error("IoException ao gerar mensagem de " + e.getMessage());
//            System.out.println("Erro ao enviar a mensagem   ");
        }
	}
	
	protected void sendMessage(int time, int channel, MyMessage buildingFire) {
        byte[] speak = null;
        try {
            speak = Serializer.serialize(buildingFire);
            sendSpeak(time, channel, speak);
//            System.out.println("Mensagem de enviada com sucesso");
            Logger.debug("Mensagem enviado com sucesso");
        } catch (IOException e) {
        	e.printStackTrace();
            Logger.error("IoException ao gerar mensagem de " + e.getMessage());
//            System.out.println("Erro ao enviar a mensagem   ");
        }
    }

    protected void heardMessage(Collection<Command> heard) {
    	this.getReceivedMessage().clear();
        for (Command next : heard) {
            if (next instanceof AKSpeak) {
                byte[] msg = ((AKSpeak) next).getContent();
                try {
                    Object object = Serializer.deserialize(msg);
                    if (object instanceof TokenInformation) {
                    	TokenInformation tmp = (TokenInformation)object;
                    	this.getReceivedMessage().add(tmp);
                    } else if (object instanceof ColeagueInformation) {
                    	ColeagueInformation tmp = (ColeagueInformation)object;
                    	this.getReceivedMessage().add(tmp);
                    }else if (object instanceof ArrayList) {
                    	for(TokenInformation t : (ArrayList<TokenInformation>)object)
                    	{
                    		this.getReceivedMessage().add(t);
                    	}
                    	  //this.getReceivedMessage().add((MyMessage)object);
                    	//System.out.println("Não entrou em nada");
                    }else{
                    	
                    }
                } catch (IOException e) {
                    Logger.error("Não entendi a mensagem!" + e.getMessage());
                } catch (ClassNotFoundException e) {
                    Logger.error("Mensagem veio com classe que não conheço.");
                }
            }
        }
    }
    
  //retorna o caminho mais curto (algoritmo de Dijkstra) do local atual do agente ate um destino
    public List<EntityID> getDijkstraPath(EntityID position, EntityID destiny)
    {
    	List<EntityID> returnedPath = new ArrayList<EntityID>();
    	DijkstraShortestPath<EntityID, DefaultWeightedEdge> shortestPath = 
    			new DijkstraShortestPath<EntityID, DefaultWeightedEdge>(this.kruskalMap, position, destiny);
    	returnedPath.add(shortestPath.getPath().getStartVertex());
    	for(DefaultWeightedEdge e : shortestPath.getPathEdgeList())
    	{
    		EntityID sourceV = this.kruskalMap.getEdgeSource(e);
    		EntityID targetV = this.kruskalMap.getEdgeTarget(e);
    		if(returnedPath.contains(sourceV) == false && shortestPath.getPath().getEndVertex() != sourceV)
    		{
    			returnedPath.add(sourceV);
    		}
    		if(returnedPath.contains(targetV) == false && shortestPath.getPath().getEndVertex() != targetV)
    		{
    			returnedPath.add(targetV);
    		}
    	}
    	returnedPath.add(shortestPath.getPath().getEndVertex());
    	return returnedPath;
    }
    
    //monta a minimal spanning tree do mapa
    protected ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> minimalSpanningTree(Graph<EntityID, DefaultWeightedEdge> graph)
    {
    	ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> h = new ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		
    	KruskalMinimumSpanningTree kruskalMST = new KruskalMinimumSpanningTree(graph);
		Iterator itr = kruskalMST.getEdgeSet().iterator();
		while(itr.hasNext())
		{
			DefaultWeightedEdge edge = (DefaultWeightedEdge) itr.next();
			if(h.containsVertex(graph.getEdgeSource(edge)) == false)
			{
				h.addVertex(graph.getEdgeSource(edge));
			}
			if(h.containsVertex(graph.getEdgeTarget(edge)) == false)
			{
				h.addVertex(graph.getEdgeTarget(edge));
			}
			h.addEdge(graph.getEdgeSource(edge), graph.getEdgeTarget(edge));
		}
		return h;
    }

	public ArrayList<MyMessage> getReceivedMessage() {
		return receivedMessages;
	}

	public void setReceivedMessage(ArrayList<MyMessage> receivedMessage) {
		this.receivedMessages = receivedMessage;
	}

	public List<TokenInformation> getValue() {
		return value;
	}

	public void setValue(List<TokenInformation> value) {
		this.value = value;
	}

	public ArrayList<TokenInformation> getPotentialValue() {
		return potentialValue;
	}

	public void setPotentialValue(ArrayList<TokenInformation> potentialValue) {
		this.potentialValue = potentialValue;
	}

	public int getCommunicationChannel() {
		return communicationChannel;
	}

	public void setCommunicationChannel(int communicationChannel) {
		this.communicationChannel = communicationChannel;
	}

	public ArrayList<RetainedInformation> getRetained() {
		return retained;
	}

	public void setRetained(ArrayList<RetainedInformation> retained) {
		this.retained = retained;
	}

	public ArrayList<EntityID> getOtherJobs() {
		return otherJobs;
	}

	public void setOtherJobs(ArrayList<EntityID> otherJobs) {
		this.otherJobs = otherJobs;
	}


}
