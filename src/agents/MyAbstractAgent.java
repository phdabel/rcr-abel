package agents;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;

import message.MessageType;
import message.MyMessage;
import message.Serializer;

import rescuecore2.Constants;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.kernel.comms.ChannelCommunicationModel;
import rescuecore2.standard.kernel.comms.StandardCommunicationModel;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntityURN;

import sample.SampleSearch;
import worldmodel.jobs.Task;
import worldmodel.jobs.Token;
/**
Abstract base class for MyAgent.
@param <E> The subclass of StandardEntity this agent wants to control.
*/
public abstract class MyAbstractAgent<E extends StandardEntity> extends StandardAgent<E> {
	
	private static final int RANDOM_WALK_LENGTH = 50;

    private static final String SAY_COMMUNICATION_MODEL = StandardCommunicationModel.class.getName();
    private static final String SPEAK_COMMUNICATION_MODEL = ChannelCommunicationModel.class.getName();
    protected static final Double THRESHOLD = 0.3;
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

    //private Map<EntityID, Set<EntityID>> neighbours;
    protected boolean	channelComm;
    
    private ArrayList<MyMessage> receivedMessages = new ArrayList<MyMessage>();
    
    /**
     * Known Tasks Set
     * set containing at each timestep all the tasks that has been perceived by all agents
     */
    protected Map<Integer, List<Task>> KTS = new HashMap<Integer, List<Task>>();
    /**
     * Token Set is the set of tokens each agent currently holds
     */
    protected List<Token> TkS = new ArrayList<Token>();
    /**
     * Temporary Token Set
     * set containing the tokens created by the agent in the current time step
     */
    protected List<Token> TmpTkS = new ArrayList<Token>();
    /**
     * Accomplished Tasks Set
     * set containing at each time step all the tasks that have been accomplished by all the agents
     */
    protected Map<Integer, List<Task>> ATS = new HashMap<Integer, List<Task>>();
    
    protected ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> map;
    protected ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> mapTmp;
    
    protected Task currentTask = null;
    
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
        roadIDs = new ArrayList<EntityID>();
        refugeIDs = new ArrayList<EntityID>();
        
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
        }
         
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
        //neighbours = search.getGraph();
        this.map = this.worldGraph();
        this.mapTmp = this.map;
        useSpeak = config.getValue(Constants.COMMUNICATION_MODEL_KEY).equals(SPEAK_COMMUNICATION_MODEL);
        Logger.debug("Modelo de Comunicação: " + config.getValue(Constants.COMMUNICATION_MODEL_KEY));
        Logger.debug(useSpeak ? "Usando modelo SPEAK" : "Usando modelo SAY");
    }
    
    /**
    Construct a random walk starting from this agent's current location to a random building.
     * @param fireBrigade 
    @return A random walk.
     */
    protected List<EntityID> walk(List<EntityID> path, EntityID local) {
    	
    	if(path.isEmpty()){
    		
    		Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.ROAD);
    		List<Road> road = new ArrayList<Road>();
    		for (StandardEntity next : e)
    		{
    			Road r = (Road)next;
    			road.add(r);
    		}
    		Integer index = new Random().nextInt(road.size());
    		
    		EntityID destiny = road.get(index).getID();
    		//path = search.breadthFirstSearch(local, destiny);
    		path = this.getDijkstraPath(local, destiny, this.mapTmp);
    	}else{
    		//path = search.breadthFirstSearch(location().getID(), path.get((path.size() - 1)));
    		/*List<EntityID> tmp = new ArrayList<EntityID>();
    		int ct = 0;
    		for(EntityID p : path)
    		{
    			
    			if(p != local && ct == 0)
    			{
    				tmp.add(p);
    			}else if(p == local)
    			{
    				ct = 1;
    				tmp.add(p);
    			}
    		}

    		System.out.println("Tamanho do caminho atual "+path.size());
    		path.removeAll(tmp);
    		System.out.println("Tamanho para remoção "+tmp.size());*/
    		path = this.getDijkstraPath(local, path.get((path.size() - 1)), this.mapTmp);
    		
    	}
    	return path;
    }
    
    protected Double euclidianDistance(int x, int x0, int y, int y0)
    {
    	Double norma = 0.0;
    	norma = (double) (((x - x0)^2)+(y - y0)^2);
    	Double result = Math.sqrt(norma);
		return result;
    	
    }
    
    protected ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> worldGraph()
    {
    	ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> g = new ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge>(DefaultWeightedEdge.class);
    	for(Entity next : model)
    	{    		
    		if(next instanceof Road)
    		{
    			Road b = (Road)next;
    			//Double size = GeometryTools2D.computeArea(GeometryTools2D.vertexArrayToPoints(b.getApexList()));
    			
    			if(g.containsVertex(b.getID()) == false){
    				g.addVertex(b.getID());
    				List<EntityID> neighbours = b.getNeighbours();
    				for(EntityID n : neighbours)
    				{
    					if(g.containsVertex(n)){
    						Area tmp = (Area)model.getEntity(n);
    						Double size = this.euclidianDistance(b.getX(), tmp.getX(), b.getY(), tmp.getY());
    						g.addEdge(n, b.getID());
    						g.setEdgeWeight(g.getEdge(n, b.getID()), size);
    					}
    				}
    			}
    		}
    		if(next instanceof Building)
    		{
    			Building b = (Building)next;
    			buildingIDs.add(b.getID());
    			
    			if(g.containsVertex(b.getID()) == false){
    				g.addVertex(b.getID());
    				List<EntityID> neighbours = b.getNeighbours();
    				for(EntityID n : neighbours)
    				{
    					if(g.containsVertex(n)){
    						Area tmp = (Area)model.getEntity(n);
    						Double size = this.euclidianDistance(b.getX(), tmp.getX(), b.getY(), tmp.getY());
    						g.addEdge(n, b.getID());
    						g.setEdgeWeight(g.getEdge(n, b.getID()), size);
    					}
    				}
    			}
    		}
    		if(next instanceof Refuge)
    		{
    			Refuge b = (Refuge)next;
    			refugeIDs.add(b.getID());
    			if(g.containsVertex(b.getID()) == false){
    				g.addVertex(b.getID());
    				List<EntityID> neighbours = b.getNeighbours();
    				for(EntityID n : neighbours)
    				{
    					if(g.containsVertex(n)){
    						Area tmp = (Area)model.getEntity(n);
    						Double size = this.euclidianDistance(b.getX(), tmp.getX(), b.getY(), tmp.getY());
    						g.addEdge(n, b.getID());
    						g.setEdgeWeight(g.getEdge(n, b.getID()), size);
    					}
    				}
    			}
    			
    		}
    		if(next instanceof StandardEntity)
    		{
    			StandardEntity b = (StandardEntity)next;
    			if(g.containsVertex(b.getID()) == false)
    			{
    				g.addVertex(b.getID());
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
        	List<EntityID> path = search.breadthFirstSearch(sourcePosition, t);
        	//List<EntityID> path = this.getDijkstraPath(sourcePosition, t);
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
	
	protected void sendMessage(int time, int channel, MyMessage message) {
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

    protected void heardMessage(Collection<Command> heard) {
    	this.getReceivedMessage().clear();
        for (Command next : heard) {
            if (next instanceof AKSpeak) {
                byte[] msg = ((AKSpeak) next).getContent();
                try {
                    Object object = Serializer.deserialize(msg);
                    if (object instanceof MyMessage) {
                    	MyMessage tmp = (MyMessage)object;
                    	this.getReceivedMessage().add(tmp);
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
    
    public List<EntityID> getDijkstraPath(EntityID position, EntityID destiny)
    {
    	return this.getDijkstraPath(position, destiny, this.map);
    }
    
    
  //retorna o caminho mais curto (algoritmo de Dijkstra) do local atual do agente ate um destino
    public List<EntityID> getDijkstraPath(EntityID position, EntityID destiny,
    		ListenableUndirectedWeightedGraph<EntityID, DefaultWeightedEdge> mapTarget)
    {
    	List<EntityID> returnedPath = new ArrayList<EntityID>();
    	
    	DijkstraShortestPath<EntityID, DefaultWeightedEdge> shortestPath = 
    			new DijkstraShortestPath<EntityID, DefaultWeightedEdge>(mapTarget, position, destiny);
    	returnedPath.add(shortestPath.getPath().getStartVertex());
    	for(DefaultWeightedEdge e : shortestPath.getPathEdgeList())
    	{
    		EntityID sourceV = mapTarget.getEdgeSource(e);
    		EntityID targetV = mapTarget.getEdgeTarget(e);
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

	public ArrayList<MyMessage> getReceivedMessage() {
		return receivedMessages;
	}

	public void setReceivedMessage(ArrayList<MyMessage> receivedMessage) {
		this.receivedMessages = receivedMessage;
	}

	public int getCommunicationChannel() {
		return communicationChannel;
	}

	public void setCommunicationChannel(int communicationChannel) {
		this.communicationChannel = communicationChannel;
	}
	
	
	public Boolean taskInKTS(Task task)
	{
		int ct = 0;
		for(Map.Entry<Integer, List<Task>> entry : this.KTS.entrySet())
		{
			for(Task t : entry.getValue())
			{
				if(t.getId() == task.getId())
				{
					ct++;
					break;
				}
			}
		}
		if(ct >= 1){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 
	 * @param task
	 * @param timestep
	 */
	public void onPercReceived(Task task, int timestep)
	{
		//if (task not in KTS)
		if( !this.taskInKTS(task) )
		{
			//adiciona task em KTS
			if(this.KTS.containsKey(timestep))
			{
				List<Task> tmpTask = this.KTS.get(timestep);
				tmpTask.add(task);
				this.KTS.put(timestep, tmpTask);
			}else{
				List<Task> tmpTask = new ArrayList<Task>();
				tmpTask.add(task);
				this.KTS.put(timestep, tmpTask);
			}
			
			//cria tokens e insere em TmpTokens
			for(int i = 0; i < task.getNumberTokens(); i++)
			{
				Token tok = new Token(i, task);
				tok.setThreshold(THRESHOLD);
				
				this.TmpTkS.add(tok);
			}
			
			//envia mensagem
			MyMessage msg = new MyMessage(MessageType.ANNOUNCE, task);
			msg.setSender(me().getID().getValue());
			sendMessage(timestep, this.getCommunicationChannel(), msg);
			
		}
	}
	
	public void onMsgReceived(MyMessage msg, int timestep)
	{
		if(msg.getType() == MessageType.ACCOMPLISHED_TASK)
		{
			List<Task> tasks = new ArrayList<Task>();
			if(this.ATS.get(timestep) == null)
			{
				tasks = new ArrayList<Task>();
			}else{
				tasks = this.ATS.get(timestep);
			}
			tasks.add(msg.getTask());
			this.ATS.put(timestep, tasks);
		}
		if(msg.getType() == MessageType.ANNOUNCE)
		{
			if( !this.taskInKTS(msg.getTask()) )
			{
				List<Task> tmpTasks;
				if(this.KTS.get(timestep) == null){
					tmpTasks = new ArrayList<Task>();
				}else{
					tmpTasks = this.KTS.get(timestep);
				}
				tmpTasks.add(msg.getTask());
				this.KTS.put(timestep, tmpTasks);
			}else{
				if(msg.getSender() >= me().getID().getValue())
				{
					for(Token t : this.TmpTkS)
					{
						//remove itens do TmpTkS
						if(t.getTask().getId() == msg.getTask().getId())
						{
							this.TmpTkS.remove(t);
						}
					}
					if(this.currentTask == msg.getTask())
					{
						this.stopCurrentTask();
					}
				}
			}
		}
		if(msg.getType() == MessageType.TOKEN)
		{
			this.TkS.addAll(msg.getToken());
		}
	}
	
	protected void onTaskaccomplishment(Task task, int timestep)
	{
		if(this.ATS.containsKey(timestep))
		{
			List<Task> tmpTask = new ArrayList<Task>();
			tmpTask = this.ATS.get(timestep);
			tmpTask.add(task);
			this.ATS.put(timestep, tmpTask);
		}
		MyMessage msg = new MyMessage(MessageType.ACCOMPLISHED_TASK, task);
		sendMessage(timestep, this.getCommunicationChannel(), msg);
		return;
	}
	
	protected Task tokenManagement(int timestep)
	{
		//remove tasks ja terminadas da lista de tokens
		for(Map.Entry<Integer, List<Task>> entry : this.ATS.entrySet())
		{
			for(Task t : entry.getValue()){
				for(Token tk : this.TkS)
				{
					if(tk.getTask() == t)
					{
						this.TkS.remove(tk);
					}
				}
			}
		}
		//tokens que serao realizados
		List<Token> tokenSet = this.chooseTokenSet(this.TkS);
		//tokens que serao enviados
		this.TkS.removeAll(tokenSet);
		
		List<Token> sendTokenSet = this.TkS;
		this.TkS.clear();
		
		MyMessage msg = new MyMessage(MessageType.TOKEN, sendTokenSet);
		sendMessage(timestep, this.getCommunicationChannel(), msg);
		
		this.TkS = tokenSet;
		this.TkS.addAll(this.TmpTkS);
		
		return this.startTask(chooseTask(tokenSet));
		
	}
	
	protected Task startTask(Task someTask) {
		return someTask;
		
	}

	private Task chooseTask(List<Token> tokenSet) {
		
		Integer index = new Random().nextInt(tokenSet.size());
		return tokenSet.get(index).getTask();
	}

	public ArrayList<Token> chooseTokenSet(List<Token> tokens)
	{
		return this.maxCap(tokens);
	}
	
	public ArrayList<Token> maxCap(List<Token> tokens)
	{
		ArrayList<Token> in = new ArrayList<Token>();
		Collections.sort(tokens, new ValueSorter());
		int ct = 1;
		for(Token t : tokens){
			if(ct <= 3)
			{
				in.add(t);
				ct++;
			}
		}
		return in;
	}
	
	

	
	protected abstract void stopCurrentTask();


}
