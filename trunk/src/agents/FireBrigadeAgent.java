package agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.DistanceSorter;


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
    private ArrayList<Integer> potentialTmp = new ArrayList<Integer>();
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
		// Are we currently filling with water?
        if (me().isWaterDefined() && me().getWater() < maxWater && location() instanceof Refuge) {
            Logger.info("Filling with water at " + location());
            sendRest(time);
            return;
        }
        // Are we out of water?
        if (me().isWaterDefined() && me().getWater() == 0) {
            // Head for a refuge
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
            //List<EntityID> path = search.breadthFirstSearch(me().getPosition(), refugeIDs);
            if (closestPath != null) {
                Logger.info("Moving to refuge");
                sendMove(time, closestPath);
                return;
            }
            else {
                Logger.debug("Couldn't plan a path to a refuge.");
                closestPath = randomWalk();
                Logger.info("Moving randomly");
                sendMove(time, closestPath);
                return;
            }
        }
		
    	//recebendo mensagens
    	this.heardMessage(heard);
    	this.getReceivedMessage().addAll(this.getFireJob());
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
    	   	if(msg instanceof TokenInformation && ((TokenInformation)msg).getValueType() == MessageType.BUILDING_FIRE)
    	   	{
    	   		TokenInformation token = (TokenInformation)msg;    	   		
    	   		System.out.println("Token Recebido "+token.getId()+" eu sou "+me().getID().getValue()+" limiar "+token.getThreshold());
    	   		StandardEntity target = model.getEntity(new EntityID(token.getAssociatedValue()));
    	   		
    	   		/*
    	   		Collection<StandardEntity> otherTargets = this.tasksInRange(target.getID());
	        	for(StandardEntity oT : otherTargets)
	        	{
	        		if(oT.getStandardURN() == StandardEntityURN.ROAD)
	        		{
	        			Road r = (Road)oT;
	                	if (r.isBlockadesDefined() && !r.getBlockades().isEmpty() && this.getOtherJobs().contains(r.getID()) == false ) {
	                		this.getOtherJobs().add(r.getID());
	                		TokenInformation job = new TokenInformation(r.getID().getValue(), true, MessageType.BLOCKADE);
	                    	this.sendMessage(time, channel, job);
	                	}
	        		}
	        	}*/
    	   		
    	   		//se limiar é menor que a capacidade do agente
    	   		Double capability = 0.0;
    	   		capability = this.computeCapability(target.getID());
    	   		if(token.getThreshold() < capability)
    	   		{
    	   			token.setCapability(capability);
    	   			//AND MONITOR
    	   			if(token.getPotential() && token.getOwner() == 0)
    	   			{
    	   				token.setOwner(me().getID().getValue());
    	   				if(potentialTmp.contains(token) == false)
    	   				{
    	   					this.potentialTmp.add(token.getAssociatedValue());
    	   				}
    	   				this.sendMessage(time, channel, token);
    	   				
    	   				
    	   			}
    	   			//TOKEN RECEIVED BY OTHER AGENT
    	   			else if(token.getPotential() && token.getOwner() != 0 && token.getOwner() != me().getID().getValue()){
    	   				token.setOwner(me().getID().getValue());
    	   				this.getPotentialValue().add(token);
    	   				RetainedInformation retained = new RetainedInformation(token.getAssociatedValue(),me().getID().getValue());
    	   				this.sendMessage(time,  channel, retained);
    	   			}else{
    	   				this.getValue().add(token);
    	   			}
    	   			//se os recursos necessarios nao extrapolam os limites do agente
        	   		//calcula recursos necessarios
        	   		Double sumSpentResource = 0.0;
        	   			
        	   		for(TokenInformation ttmp : this.getValue())
        	   		{
                		Building b = (Building)model.getEntity(new EntityID(ttmp.getAssociatedValue()));
                		sumSpentResource += this.spentResource(b);
                		//System.out.println("Gasto de recurso para "+b.getID().getValue()+" - "+this.spentResource(b));
        	   		}
        	   		if(sumSpentResource > this.myWaterQuantity())
        	   		{
        	   			
        	   			ArrayList<TokenInformation> out = maxCap();
        	   			for(TokenInformation t : out)
        	   			{
        	   				if(t.getPotential() == true)
        	   				{
        	   					t.setOwner(me().getID().getValue());
        	   					ReleaseInformation release = new ReleaseInformation(t);
        	   					this.sendMessage(time, channel, release);
        	   				}else{
        	   					this.sendMessage(time, channel, token);
        	   				}
        	   			}
        	   		}
    	   			
    	   		}else{
    	   			this.sendMessage(time, channel, token);
    	   		}
    			
    	   	}else if(msg instanceof LockInformation){
    	   		TokenInformation token = ((LockInformation)msg).getToken();
    	   		if(this.getPotentialValue().contains(token)==true){
    	   			this.getPotentialValue().remove(token);
    	   			this.getValue().add(token);
    	   		}else{
    	   			TokenInformation t = ((LockInformation)msg).getToken();
    	   			t.setOwner(me().getID().getValue());
    	   			ReleaseInformation r = new ReleaseInformation(t);
    	   			this.sendMessage(time, channel, r);
    	   		}
    	   	
    	   	}else if(msg instanceof ReleaseInformation){
    	   		ReleaseInformation r = (ReleaseInformation)msg;
    	   		if(r.getToken().getOwner() == me().getID().getValue())
    	   		{
    	   			this.getPotentialValue().remove(r.getToken());
    	   		}
    	   		
    	   	}else if(msg instanceof RetainedInformation && potentialTmp.contains(((RetainedInformation)msg).getValue()) ){
    	   		this.getRetained().add((RetainedInformation)msg);
    	   	}else if(msg instanceof ReleaseInformation && potentialTmp.contains(((RetainedInformation)msg).getValue()) ){
    	   		this.getRetained().remove((RetainedInformation)msg);
    	   	}
    	   	//definir grupo para trabalho conjunto
    	   	ArrayList<LockInformation> lockers = new ArrayList<LockInformation>();
    	   	ArrayList<ReleaseInformation> releasers = new ArrayList<ReleaseInformation>();
    	   	
    	   for(RetainedInformation r : this.getRetained())
    	   {
    	   		TokenInformation tokenTmp = new TokenInformation(r.getValue(), true, MessageType.BUILDING_FIRE);
    	   		tokenTmp.setThreshold(THRESHOLD);
    	   		tokenTmp.setOwner(r.getSender());
    	   		Integer tmpSize = this.getDijkstraPath(new EntityID(r.getSender()), new EntityID(tokenTmp.getAssociatedValue())).size();
	   				
    	   		if(potentialTmp.contains(r.getValue())  ){
    	   			
    	   			for(LockInformation l : lockers)
    	   			{
    	   				if(l.getToken().getAssociatedValue() == r.getValue())
	   					{
    	   					Integer lSize = this.getDijkstraPath(new EntityID(l.getToken().getOwner()), new EntityID(l.getToken().getId())).size(); 
    	   					if(tmpSize < lSize)
    	   					{
    	   						lockers.remove(l);
    	   						lockers.add(new LockInformation(tokenTmp));
    	   						releasers.add(new ReleaseInformation(l.getToken()));
    	   					}else{
    	   						releasers.add(new ReleaseInformation(tokenTmp));
    	   					}
	   					}
    	   			}
    	   		}
    	   }
    	   for(LockInformation l : lockers)
    	   {
    		   this.sendMessage(time, channel, l);
    	   }
    	   for(ReleaseInformation r: releasers)
    	   {
    		   this.sendMessage(time, channel, r);
    	   }
    	   	
   	    }
    	ArrayList<TokenInformation> removeList = new ArrayList<TokenInformation>();
    	for(TokenInformation t : this.getValue())
    	{
    		//System.out.println("Lista de values "+this.getValue());
    		Building b = (Building)model.getEntity(new EntityID(t.getAssociatedValue()));
    		
    		if(b.isOnFire()){
    			this.extinguishFire(time, new EntityID(t.getAssociatedValue()));
    		}else{
    			removeList.add(t);
    		}
    	}
    	for(TokenInformation t : removeList)
    	{
    		Building b = (Building)model.getEntity(new EntityID(t.getAssociatedValue()));
    		this.getBuildingDetected().remove(b);
    	}
    	this.getValue().removeAll(removeList);
    	
    	if(this.getValue().isEmpty()){
    	
    		/**
         	* If any of previous plans do not work
         	* agent moves randomly
         	*/
    		List<EntityID> path = null;
        	Logger.debug("Não é possível planejar um caminho até o incêndio.");
        	path = randomWalk();
        	Logger.info("Movendo-se aleatoriamente.");
        	//	System.out.println("Movendo aleatoriamente.");
        	sendMove(time, path);
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
	
	public ArrayList<TokenInformation> maxCap()
	{
		ArrayList<TokenInformation> out = new ArrayList<TokenInformation>();
		ArrayList<TokenInformation> in = new ArrayList<TokenInformation>();
		Double resTot = 0.0;
		Collections.sort(this.getValue(), new ValueSorter());
		System.out.println("values sorteados "+this.getValue());
		for(TokenInformation t : this.getValue()){
			Building b = (Building)model.getEntity(new EntityID(t.getAssociatedValue()));
    		Double spentResource = this.spentResource(b);
			if((resTot + spentResource) >= this.myWaterQuantity())
			{
				out.add(t);
			}
			else
			{
				in.add(t);
				resTot = resTot + spentResource;
			}
			this.getValue().clear();
			this.setValue(in);
		}
		return out;
	}
	
	/**
	 * local function that returns a Collection of Burning Building
	 * 
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

	public void extinguishFire(int timestep, EntityID building)
	{
    	if((model.getDistance(me().getID(), building) <= this.maxDistance) || me().getPosition() != building)
    	{
    		Logger.info("Apagando Incêndio " + building);
    		sendExtinguish(timestep, building, this.maxPower);
            return;
    	}
    	else
    	{
    		List<EntityID> path = planPathToTask(me().getPosition() ,building, this.maxDistance);
            //if path is not null
            if (path != null) {
                Logger.info("Movendo-se para um alvo.");
                //sends Move command to a path (list of EntityIDs) at specified time
                sendMove(timestep, path);
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
