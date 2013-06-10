package agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import message.Channel;
import message.ColeagueInformation;
import message.LockInformation;
import message.MessageType;
import message.ReleaseInformation;
import message.RetainedInformation;
import message.TokenInformation;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.DistanceSorter;
import static rescuecore2.misc.Handy.objectsToIDs;


public class FireBrigadeAgent extends MyAbstractAgent<FireBrigade> {
	
	/**
	 * 
	 */
	
	private static final String MAX_WATER_KEY = "fire.tank.maximum";
    private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
    private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
    private static final int channel = Channel.BROADCAST.ordinal();
    private int maxWater;
    private int maxDistance;
    private int maxPower;
    
    private ArrayList<Building> buildingDetected = new ArrayList<Building>();
    //private ArrayList<Integer> potentialTmp = new ArrayList<Integer>();
    private Queue<AgentState> stateQueue = new LinkedList<AgentState>();
    private List<EntityID> currentPath = new ArrayList<EntityID>();
    private Boolean pathDefined = false;
    private TokenInformation tokenDef = null;
    @Override
    public String toString() {
        return "FireBrigade LADCOP";
    }
    
    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE);
        maxWater = config.getIntValue(MAX_WATER_KEY);
        maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
        maxPower = config.getIntValue(MAX_POWER_KEY);
        this.setCommunicationChannel(channel);
        
        Logger.info("Fire Brigade LADCOP está conectado: distância máxima de extinção de chamas = " + maxDistance + ", força máxima = " + maxPower + ", máxima capacidade do tanque = " + maxWater);
    }
    
    /**
     * this method just returns the URN of FIRE_BRIGADE
     */
	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
	}
	

	@Override
	protected void think(int time, ChangeSet changeset, Collection<Command> heard) {
		
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            sendSubscribe(time, this.getCommunicationChannel());
        }
		
		//enviando informação sobre quem sou eu para os colegas
    	
		if(time <= 5){
		
			ColeagueInformation meInformation =
    			new ColeagueInformation(
    					me().getID().getValue(),
    					me().getPosition().getValue()
    					);
			this.sendMessage(time, channel, meInformation);
		}
		
		StandardEntity pos = (StandardEntity)model.getEntity(me().getPosition());
		if(pos instanceof Road){
			if(((Road)pos).isBlockadesDefined()){
				TokenInformation blockadeToken = new TokenInformation(pos.getID().getValue(), false, MessageType.BLOCKADE);
				sendMessage(time, channel, blockadeToken);
			}
		}
		
    	//recebendo mensagens
    	this.heardMessage(heard);
    	this.getReceivedMessage().addAll(this.getFireJob());
    	
        /**
         * PLANEJAMENTO
         */
        for(Object msg: this.getReceivedMessage())
    	{
    	   	if(msg instanceof ColeagueInformation)
    	   	{
    	   		ColeagueInformation tmpColeague = (ColeagueInformation)msg;
    	   		if(this.getColeagues().contains((Integer)tmpColeague.getId()) == false)
    	   		{
    	   			this.getColeagues().add((Integer)tmpColeague.getId());
    	   		}
    	   	}
    	   	if(msg instanceof TokenInformation && ((TokenInformation)msg).getValueType() == MessageType.BUILDING_FIRE )
    	   	{
    	   		TokenInformation t = (TokenInformation)msg;
    	   		this.getValue().add(t);
    	   		this.sendMessage(time,  channel, t);
    	   	}
    	   	
    	}
        /**
         * INSERÇÃO DE VALORES NA FILA
         */
        if((!this.getValue().isEmpty() && this.stateQueue.isEmpty())
        		||
        		(!this.getValue().isEmpty() && stateQueue.peek().getState() == "RandomWalk")
        		)
        {
        	if(stateQueue.peek().getState() == "RandomWalk")
        	{
        		stateQueue.poll();
        	}
        	TokenInformation tmp = this.getValue().get(0);
        	EntityID tmpID = new EntityID(tmp.getAssociatedValue());
        	this.getValue().remove(0);
        	if(me().isWaterDefined() && me().getWater() == 0)
        	{
        		Queue<AgentState> tmpQueue = stateQueue;
        		stateQueue.clear();
        		stateQueue.add(new AgentState("GetWater"));
        		stateQueue.addAll(tmpQueue);
        	}
        	if(model.getDistance(getID(), tmpID) <= maxDistance)
        	{
        		stateQueue.add(new AgentState("Walk", tmpID));
        		stateQueue.add(new AgentState("Extinguish", tmpID));
        		this.printQueue();
        	}else{
        		stateQueue.add(new AgentState("Extinguish", tmpID));
        	}
        		
        }else if (this.stateQueue.isEmpty()){
        	stateQueue.add(new AgentState("RandomWalk"));
        }
        
        /**
         * AÇÃO e CONTROLE
         * Máquina de Estados
         * Aqui começa a execução das ações na pilha de ações
         */
        if(!stateQueue.isEmpty())
        {
        	AgentState currentAction = stateQueue.peek();
        	
        	switch(currentAction.getState())
        	{
        		case "RandomWalk":
        			//System.out.println("Random Walk da pilha de estados.");
        			if(currentPath.isEmpty() && pathDefined == false)
        			{
        				currentPath = this.walk(currentPath);
        				pathDefined = true;
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() <= 2){
        				stateQueue.poll();
        				pathDefined = false;
        				sendMove(time, currentPath);
        				return;
        			}else{
        				currentPath = this.walk(currentPath);
        				sendMove(time, currentPath);
        			}
        			break;
        		case "Walk":
        			//System.out.println("Walk da pilha de estados");
        			System.out.println(currentPath);
        			if(currentPath.isEmpty() && pathDefined == false){
        				currentPath = this.getDijkstraPath(me().getPosition(), currentAction.getId());
        				pathDefined = true;
        				currentPath = this.walk(currentPath);
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() >2 && pathDefined == true)
        			{
        				currentPath = this.walk(currentPath);
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() <= 2){
        				stateQueue.poll();
        				sendMove(time,currentPath);
        				pathDefined = false;
        				return;
        			}
        			break;
        		case "GetWater":
        			//System.out.println("GetWater da pilha de estados");
        			if(me().getWater() == this.maxWater)
        			{
        				stateQueue.poll();
        			}else{
        				getWater(time);
        			}
        			break;
        		case "Extinguish":
        			//System.out.println("Extinguish da pilha de estados");
        			Building b = (Building)model.getEntity(currentAction.getId());
        			if(me().getWater() == 0)
        			{
        				Queue<AgentState> tmpQueue = new LinkedList<AgentState>();
        				tmpQueue.addAll(stateQueue);
                		stateQueue.clear();
                		stateQueue.add(new AgentState("GetWater"));
                		stateQueue.addAll(tmpQueue);        				
        			}else if((model.getDistance(getID(), currentAction.getId()) > maxDistance) ||
        					me().getPosition() == currentAction.getId())
        			{
        				EntityID s = this.somethingNextToFire(currentAction.getId());
        				Queue<AgentState> tmpQueue = stateQueue;
        				System.out.println("Fila temp "+tmpQueue);
                		stateQueue.clear();
                		stateQueue.add(new AgentState("Walk", s));
                		stateQueue.addAll(tmpQueue);
                		System.out.println("Fila apos "+stateQueue);
        			}else if(b.isOnFire())
        			{
        				sendExtinguish(time, b.getID(), this.maxPower);
        			}else if(!b.isOnFire())
        			{
        				stateQueue.poll();
        			}
        			break;
        	}
        }
        /**
         * fim da máquina de estados
         */
        
        /**
         * imprime fila de tarefas
         
        System.out.println("------------------------------------");
        System.out.println("TimeStep "+time);
        System.out.println("Agente Bombeiro "+me().getID().getValue());
        System.out.println("Posição Atual "+me().getPosition());
        System.out.println("Lista de values "+this.getValue());
        System.out.println("Caminho a seguir "+currentPath);
        int ct = 1;
        for(AgentState a : this.stateQueue)
        {
        	System.out.println("Estado "+ct+" - "+a.getState());
        	System.out.println("Alvo: "+a.getId());
        	ct++;
        }
        System.out.println("------------------------------------");
        */
	}
	
	protected void printQueue(){
		int ct = 1;
		System.out.println("------------------------------------");
        for(AgentState a : this.stateQueue)
        {
        	System.out.println("Estado "+ct+" - "+a.getState());
        	System.out.println("Alvo: "+a.getId());
        	ct++;
        }
        System.out.println("------------------------------------");
	}
	
	
	/**
	 * recursos necessarios para apagar um dado incendio
	 * @param b Building
	 * @return
	 */
	public Double spentResource(Building b)
	{
		Integer fierynessArea = (1500 * b.getFieryness() * b.getTotalArea());
		return (fierynessArea.doubleValue() / 100) /  this.maxWater;
	}
	
	public Double myWaterQuantity()
	{
		Integer water = me().getWater();
		return water.doubleValue() / this.maxWater;
	}
	
	public ArrayList<TokenInformation> maxCap()
	{
		ArrayList<TokenInformation> out = new ArrayList<TokenInformation>();
		ArrayList<TokenInformation> in = new ArrayList<TokenInformation>();
		Double resTot = 0.0;
		if(this.getValue().isEmpty() == false){
			Collections.sort(this.getValue(), new ValueSorter());
			System.out.println("values sorteados "+this.getValue());
			Iterator<TokenInformation> values = this.getValue().iterator();
			while(values.hasNext()){
				TokenInformation next = (TokenInformation)values.next();
				Building b = (Building)model.getEntity(new EntityID(next.getAssociatedValue()));
				Double spentResource = this.spentResource(b);
				if((resTot + spentResource) >= this.myWaterQuantity())
				{
					out.add(next);
				}
				else
				{
					in.add(next);
					resTot = resTot + spentResource;
				}
				this.getValue().clear();
				this.setValue(in);
			}
		}
		return out;
	}
	
	/**
	 * Action
	 * Ação local do agente, recolhe todos os fireJob próximos e manda para a lista de mensagens.
	 * 
	 * @return
	 */
	private ArrayList<TokenInformation> getFireJob()
	{
		Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.BUILDING);
		List<Building> buildings = new ArrayList<Building>();
		for (StandardEntity next : e)
		{
			Building b = (Building)next;
			if (b.isOnFire() && this.getBuildingDetected().contains(b) == false)
			{
				this.getBuildingDetected().add(b);
				buildings.add(b);
			}
		}
		//sort by distance
		Collections.sort(buildings, new DistanceSorter(location(), model));
		ArrayList<TokenInformation> buildingFire = new ArrayList<TokenInformation>();
		for(Building next : buildings)
		{ 
			if(next.getTotalArea() < 200)
			{
				//System.out.println("Get Fire Job detectou predio "+next.getID().getValue()+" Agente: "+me().getID().getValue());
				TokenInformation _t = new TokenInformation(next.getID().getValue(), false, MessageType.BUILDING_FIRE);
				_t.setThreshold(THRESHOLD);
				buildingFire.add(_t);
			}else
			{
				//System.out.println("Get Fire Job detectou predio com potential "+next.getID().getValue());
				Integer totalTokens = Math.abs(next.getTotalArea() / 100);
				for(int i = 0; i < totalTokens; i++)
				{
					TokenInformation _t = new TokenInformation(next.getID().getValue(), true, MessageType.BUILDING_FIRE);
					_t.setId(_t.getId()+i);
					_t.setThreshold(THRESHOLD);
					buildingFire.add(_t);
				}
			}
		}
		return buildingFire;
	}

	/**
	 * Action
	 * 
	 * Essa ação será utilizada para extinguir as chamas de um prédio.
	 * @param timestep
	 * @param building - EntityID do prédio que está em chamas
	 */
	public void extinguishFire(int timestep, EntityID building)
	{
    		Logger.info("Apagando Incêndio " + building);
    		sendExtinguish(timestep, building, this.maxPower);
            return;
    }
	
	/**
	 * Ação
	 * 
	 * Planeja um caminho para uma região próxima a um incendio
	 * @param target
	 * @return
	 */
	public EntityID somethingNextToFire(EntityID target){
		
		Collection<StandardEntity> targets = model.getObjectsInRange(target, maxDistance);
		List<EntityID> closestPath = new ArrayList<EntityID>();
		EntityID result = null;
		for(StandardEntity next : targets)
		{
			if(next instanceof Road){
				List<EntityID> path = this.getDijkstraPath(me().getPosition(), next.getID());
				if(closestPath.isEmpty())
				{
					closestPath = path;
					result = next.getID();
				}else{
					if(path.size() < closestPath.size())
					{
						closestPath = path;
						result = next.getID();
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Action
	 * 
	 * Essa ação será utilizada para levar o agente até o refúgio para reabastecer ou então reabastecer se o agente já
	 * está no refúgio.
	 * @param timestep 
	 */
	public void getWater(int timestep){
		// Are we currently filling with water?
        if (me().isWaterDefined() && me().getWater() < maxWater && location() instanceof Refuge) {
            Logger.info("Filling with water at " + location());
            sendRest(timestep);
            currentPath.clear();
            pathDefined = false;
            return;
        }
        
        // Are we out of water?
        if (me().isWaterDefined() && me().getWater() == 0) {
            // Head for a refuge
        	if(pathDefined == false)
        	{
        		List<EntityID> closestPath = new ArrayList<EntityID>();
        		for(EntityID refuge : refugeIDs)
        		{
        			List<EntityID> path = this.getDijkstraPath(me().getPosition(), refuge);
        			if(closestPath.isEmpty())
        			{
        				closestPath = path;
        			}else{
        				if(path.size() < closestPath.size())
        				{
        					closestPath = path;
        				}
        			}
        		}
        		currentPath = closestPath;
        		pathDefined = true;
        	}else{
        		currentPath = walk(currentPath);
        		sendMove(timestep, currentPath);
        		return;
        	}
        }
	}

	public ArrayList<Building> getBuildingDetected() {
		return buildingDetected;
	}

	public void setBuildingDetected(ArrayList<Building> buildingDetected) {
		this.buildingDetected = buildingDetected;
	}
	
	

}
