package agents;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;

import message.Channel;
import message.MyMessage;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
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
import worldmodel.jobs.Object;
import worldmodel.jobs.Task;
import worldmodel.jobs.Token;


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
        	
        }else{
        	for(Token t : this.TmpTkS)
        	{
        		if(!this.TkS.contains(t))
        		{
        			this.TkS.add(t);
        		}
        	}
        }
        
        this.printQueue();
        System.out.println(this.currentPath);
        
        /**
         * INSERÇÃO DE VALORES NA FILA
         */
        if(this.currentTask != null && this.currentTask.getObject() == Object.BUILDING_FIRE)
        {
        	if(!stateQueue.isEmpty() && stateQueue.peek().getState() == "RandomWalk")
        	{
        		stateQueue.poll();
        	}
        	if(stateQueue.isEmpty()){
        		Task tmp = this.currentTask;
        		EntityID tmpID = new EntityID(tmp.getId());
        	
        		if(me().isWaterDefined() && me().getWater() == 0)
        		{
        			this.addBeginingQueue(new AgentState("GetWater"));
        		
        		}
        	
        		if(model.getDistance(me().getPosition(), tmpID) > maxDistance)
        		{
        			stateQueue.add(new AgentState("Walk", tmpID));
        			stateQueue.add(new AgentState("Extinguish", tmpID));
        		//	this.printQueue();
        		}else{
        			stateQueue.add(new AgentState("Extinguish", tmpID));
        		}
        	}
        }else if (this.stateQueue.isEmpty() && this.currentTask == null && time > 3){
        	
        	this.addBeginingQueue(new AgentState("LookingForFire"));
        	stateQueue.add(new AgentState("RandomWalk"));
        	//stateQueue.add(new AgentState("Walk", this.somethingNextToFire(me().getPosition())));
        }
        
        
        /**
         * AÇÃO e CONTROLE
         * Máquina de Estados
         * Aqui começa a execução das ações na pilha de ações
         */
        //System.out.println("timeStep "+time+" agente "+me().getID().getValue());
        
        if(!this.stateQueue.isEmpty())
        {
        	AgentState currentAction = this.stateQueue.peek();
        	
        	switch(currentAction.getState())
        	{
        		case "RandomWalk":
        			if(currentPath.isEmpty() && pathDefined == false)
        			{
        				this.currentPath = this.walk(this.currentPath, me().getPosition());
        				pathDefined = true;
        				this.addBeginingQueue(new AgentState("LookingForFire"));
        				sendMove(time, this.currentPath);
        				return;
        			}else if(pathDefined == true){
        				lastPath = this.currentPath;
        				this.currentPath = this.walk(this.currentPath, me().getPosition());
        				if(this.currentPath.size() == this.lastPath.size())
        				{
        					/**
        					 * se o caminho atual é igual ao anterior
        					 * ou está bloqueado ou está junto com um algo
        					 */
        					if(!this.currentPath.isEmpty()){
        						this.addBeginingQueue(new AgentState("VerifyBlockade", this.currentPath.get(this.currentPath.size() - 1)));
        					}
        				}
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
        			}
        			break;
        		case "Walk":
        			if(this.currentPath.isEmpty() && pathDefined == false){
        				this.currentPath = this.getDijkstraPath(me().getPosition(), currentAction.getId());
        				pathDefined = true;
        				lastPath = currentPath;
        				
        				this.currentPath = this.walk(this.currentPath,  me().getPosition());
        				
        				this.addBeginingQueue(new AgentState("nextToTarget", currentAction.getId()));
        				sendMove(time, this.currentPath);
        				return;
        			}else if(pathDefined == true)
        			{
        				lastPath = currentPath;
        				this.currentPath = this.walk(this.currentPath,  me().getPosition());
        				if(lastPath.size() == currentPath.size())
        				{
        					/**
        					 * se o caminho atual é igual ao anterior
        					 * ou está bloqueado ou está junto com um algo
        					 */
        					this.addBeginingQueue(new AgentState("VerifyBlockade", currentAction.getId()));
        				}
        				this.addBeginingQueue(new AgentState("nextToTarget", currentAction.getId()));
        				sendMove(time, this.currentPath);
        				return;
        				
        			}else if(this.currentPath.size() <= 2){
        				this.stateQueue.poll();
        				
        				lastPath = currentPath;
        				this.currentPath = this.walk(currentPath,  me().getPosition());
        				if(lastPath.size() == currentPath.size())
        				{
        					/**
        					 * se o caminho atual é igual ao anterior
        					 * ou está bloqueado ou está junto com um algo
        					 */
        					this.addBeginingQueue(new AgentState("VerifyBlockade", currentAction.getId()));
        				}
        				
        				this.addBeginingQueue(new AgentState("nextToTarget", currentAction.getId()));
        				this.pathDefined = false;
        				this.currentPath.clear();
        				sendMove(time,this.currentPath);
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
        			Building b = (Building)model.getEntity(currentAction.getId());
        			if(me().getWater() == 0)
        			{
        				this.addBeginingQueue(new AgentState("GetWater"));
        				this.pathDefined = false;
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
        				this.stateQueue.poll();
        				this.onTaskaccomplishment(this.currentTask, time);
        				return;
        			}
        			break;
        		case "LookingForFire":
        			this.stateQueue.poll();
        			Collection<EntityID> all = this.getBurningBuildings();
        			List<Task> taskList = this.getFireJob(all);
        			for(Task t : taskList)
        			{
        				this.onPercReceived(t, time);
        			}
        			break;
        		case "VerifyBlockade":
        			verifyBlockade(me().getPosition(), currentAction.getId());
        			break;
        		case "nextToTarget":
        			if(this.nextToTarget(currentAction.getId()))
        			{
        				//remove nextToTarget da fila
        				this.stateQueue.poll();
        				//remove Walk da fila
        				if(this.stateQueue.peek().getState() == "Walk")
        				{
        					this.stateQueue.poll();
        				}
        			}
        			break;
        	}
        }
        /**
         * fim da máquina de estados
         */
        
        
	}
	
	protected Boolean nextToTarget(EntityID target)
	{
		Collection<StandardEntity> targets = model.getObjectsInRange(target, maxDistance);
		for(StandardEntity t : targets)
		{
			if(location() == t)
			{
				return true;
			}
		}
		return false;
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
	
	
	protected void verifyBlockade(EntityID minhaPos, EntityID alvo){
		StandardEntity position = model.getEntity(minhaPos);
		if(position instanceof Building){
			Building b = (Building)position;
			if(b.isBlockadesDefined()){
				System.out.println("Envia mensagem para policial");
			}
			this.stateQueue.poll();
		}else if(position instanceof Road){
			Road r = (Road)position;
			if(me().getBuriedness()>0){
				this.mapTmp.removeEdge(r.getID(), currentPath.get(1));
				System.out.println("remove aresta");
				ConnectivityInspector<EntityID, DefaultWeightedEdge> test = new ConnectivityInspector<EntityID, DefaultWeightedEdge>(this.mapTmp);
				if(test.pathExists(minhaPos, alvo)){
					System.out.println("existe caminho");
					
					currentPath = this.getDijkstraPath(minhaPos, alvo, this.mapTmp);
				}else{
					this.mapTmp = null;
					this.mapTmp = this.map;
					currentPath = this.walk(currentPath, me().getPosition());
				}
				System.out.println("Envia mensagem para policial");
			}
			this.stateQueue.poll();
		}
		
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
                if (b.isOnFire() && (b.getFieryness() > 0 && b.getFieryness() < 7)) {
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
        			List<EntityID> path = this.getDijkstraPath(me().getPosition(), refuge, this.mapTmp);
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
        		this.lastPath = this.currentPath;
        		this.currentPath = walk(this.currentPath, me().getPosition());
        		if(this.lastPath.size() == this.currentPath.size())
        		{
    				/**
    				* se o caminho atual é igual ao anterior
    				* ou está bloqueado ou está junto com um algo
    				*/
        			if(this.currentPath.size() > 2)
        			{
        				this.addBeginingQueue(new AgentState("VerifyBlockade", this.currentPath.get(this.currentPath.size()-1)));
        			}
        		}
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
