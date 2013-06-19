package agents;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import message.Channel;
import message.MyMessage;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.DistanceSorter;
import worldmodel.jobs.Object;
import worldmodel.jobs.Task;


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
    private List<EntityID> lastPath = new ArrayList<EntityID>();
    private Boolean pathDefined = false;
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
    	/*
		if(time <= 5){
		
			ColeagueInformation meInformation =
    			new ColeagueInformation(
    					me().getID().getValue(),
    					me().getPosition().getValue(),
    					ColeagueType.FIREMEN
    					);
			this.sendMessage(time, channel, meInformation);
		}*/
		/*
		StandardEntity pos = (StandardEntity)model.getEntity(me().getPosition());
		if(pos instanceof Road){
			if(((Road)pos).isBlockadesDefined()){
				TokenInformation blockadeToken = new TokenInformation(pos.getID().getValue(), false, MessageType.BLOCKADE);
				sendMessage(time, channel, blockadeToken);
			}
		}*/
		
    	//recebendo mensagens
    	this.heardMessage(heard);
    	
        /**
         * PLANEJAMENTO
         */
        for(MyMessage msg: this.getReceivedMessage())
    	{
        	this.onMsgReceived(msg, time);
    	   	
    	}
        if(!this.TkS.isEmpty()){
        	this.currentTask = this.tokenManagement(time);
        }
        /**
         * INSERÇÃO DE VALORES NA FILA
         */
        if(this.currentTask != null && this.stateQueue.isEmpty() && this.currentTask.getObject() == Object.BUILDING_FIRE)
        {
        	
        	if(!stateQueue.isEmpty() && stateQueue.peek().getState() == "RandomWalk")
        	{
        		stateQueue.poll();
        	}
        	
        	Task tmp = this.currentTask;
        	EntityID tmpID = new EntityID(tmp.getId());
        	
        	if(me().isWaterDefined() && me().getWater() == 0)
        	{
        		this.addBeginingQueue(new AgentState("GetWater"));
        		
        	}
        	
        	if(model.getDistance(getID(), tmpID) > maxDistance)
        	{
        		stateQueue.add(new AgentState("Walk", tmpID));
        		stateQueue.add(new AgentState("Extinguish", tmpID));
        		//this.printQueue();
        	}else{
        		stateQueue.add(new AgentState("Extinguish", tmpID));
        	}        	
        }else if (this.stateQueue.isEmpty() && this.currentTask == null && time > 3){
        	
        	this.addBeginingQueue(new AgentState("LookingForFire"));
        	stateQueue.add(new AgentState("Walk", this.somethingNextToFire(me().getPosition())));
        }
        
        
        /**
         * AÇÃO e CONTROLE
         * Máquina de Estados
         * Aqui começa a execução das ações na pilha de ações
         */
        //System.out.println("timeStep "+time+" agente "+me().getID().getValue());
        this.printQueue();
        if(!this.stateQueue.isEmpty())
        {
        	AgentState currentAction = this.stateQueue.peek();
        	
        	switch(currentAction.getState())
        	{
        		case "RandomWalk":
        			if(currentPath.isEmpty() && pathDefined == false)
        			{
        				this.currentPath = this.walk(this.currentPath);
        				pathDefined = true;
        				this.addBeginingQueue(new AgentState("LookingForFire"));
        				sendMove(time, this.currentPath);
        				return;
        			}else if(this.currentPath.size() <= 2){
        				this.stateQueue.poll();
        				pathDefined = false;
        				this.addBeginingQueue(new AgentState("LookingForFire"));
        				sendMove(time, this.currentPath);
        				this.currentPath.clear();
        				return;
        			}else if(pathDefined == true){
        				this.currentPath = this.walk(this.currentPath);
        				this.addBeginingQueue(new AgentState("LookingForFire"));
        				sendMove(time, this.currentPath);
        			}
        			break;
        		case "Walk":
        			System.out.println(currentPath);
        			if(this.currentPath.isEmpty() && pathDefined == false){
        				//this.currentPath.remove(this.currentPath.size()-1);
        				//this.currentPath = search.breadthFirstSearch(me().getPosition(), currentAction.getId());
        				this.currentPath = this.getDijkstraPath(me().getPosition(), currentAction.getId());
        				pathDefined = true;
        				
        				this.currentPath = this.walk(this.currentPath);
        				sendMove(time, this.currentPath);
        				return;
        			}else if(pathDefined == true)
        			{
        				this.currentPath = this.walk(this.currentPath);
        				sendMove(time, this.currentPath);
        				return;
        				
        			}else if(this.currentPath.size() <= 2){
        				this.stateQueue.poll();
        				this.currentPath = this.walk(this.currentPath);
        				sendMove(time,this.currentPath);
        				this.pathDefined = false;
        				this.currentPath.clear();
        				return;
        			}
        			break;
        		case "GetWater":
        			//System.out.println("GetWater da pilha de estados");
        			if(me().getWater() == this.maxWater)
        			{
        				this.stateQueue.poll();
        			}else{
        				getWater(time);
        			}
        			break;
        		case "Extinguish":
        			System.out.println("Extinguish da pilha de estados");
        			Building b = (Building)model.getEntity(currentAction.getId());
        			if(me().getWater() == 0)
        			{
        				this.addBeginingQueue(new AgentState("GetWater"));        				
        			}else if((model.getDistance(getID(), currentAction.getId()) > maxDistance) ||
        					me().getPosition() == currentAction.getId())
        			{
        				EntityID s = this.somethingNextToFire(currentAction.getId());
        				this.addBeginingQueue(new AgentState("Walk", s));
        			}else if(b.isOnFire())
        			{
        				sendExtinguish(time, b.getID(), this.maxPower);
        				return;
        			}else if(!b.isOnFire() || b.getFieryness() <= 0)
        			{
        				System.out.println(""+this.getReceivedMessage());
        				System.out.println(b.getFieryness());
        				this.stateQueue.poll();
        				this.onTaskaccomplishment(this.currentTask, time);
        				return;
        			}
        			break;
        		case "LookingForFire":
        			Collection<EntityID> all = this.getBurningBuildings();
        			List<Task> taskList = this.getFireJob(all);
        			for(Task t : taskList)
        			{
        				this.onPercReceived(t, time);
        			}
        			this.stateQueue.poll();
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
	
	protected void addBeginingQueue(AgentState newState)
    {
    	Queue<AgentState> tmpQueue = new LinkedList<AgentState>();
		tmpQueue.addAll(stateQueue);
		stateQueue.clear();
		stateQueue.add(newState);
		stateQueue.addAll(tmpQueue);
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
	
	
	
	
	/**
	 * Action
	 * Ação local do agente, recolhe todos os fireJob próximos e manda para a lista de mensagens.
	 * 
	 * @return
	 */
	private List<Task> getFireJob(Collection<EntityID> tasks)
	{
		List<Task> taskList = new ArrayList<Task>();
		for(EntityID id : tasks)
		{
			Building b = (Building)model.getEntity(id);
			Task _t = new Task(b.getID().getValue(), Object.BUILDING_FIRE, b.getID().getValue());
			
			if(b.getTotalArea() < 100)
			{
				_t.setNumberTokens(1);
			}else{
				Integer totalTokens = Math.abs(b.getTotalArea() / 100);
				_t.setNumberTokens(totalTokens);
			}
			taskList.add(_t);
		}
		
		return taskList;
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
    		if (model.getDistance(getID(), building) <= maxDistance) {
    			sendExtinguish(timestep, building, this.maxPower);
            	return;
    		}
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
				//List<EntityID> path = search.breadthFirstSearch(me().getPosition(), next.getID());
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
	
	
	private Collection<EntityID> getBurningBuildings() {
        Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.BUILDING);
        List<Building> result = new ArrayList<Building>();
        for (StandardEntity next : e) {
            if (next instanceof Building) {
                Building b = (Building)next;
                if (b.isOnFire()) {
                    result.add(b);
                }
            }
        }
        // Sort by distance
        Collections.sort(result, new DistanceSorter(location(), model));
        return objectsToIDs(result);
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
        			//List<EntityID> path = search.breadthFirstSearch(me().getPosition(), refuge);
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

	@Override
	protected void stopCurrentTask() {
		this.currentTask = null;
		this.stateQueue.clear();
		
	}
	
	

}
