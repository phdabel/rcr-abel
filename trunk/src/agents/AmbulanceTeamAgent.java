package agents;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Random;

import message.Channel;
import message.ColeagueInformation;
import message.LockInformation;
import message.MessageType;
import message.ReleaseInformation;
import message.RetainedInformation;
import message.TokenInformation;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import sample.DistanceSorter;

public class AmbulanceTeamAgent extends MyAbstractAgent<AmbulanceTeam> {

	    private Collection<EntityID> unexploredBuildings;
	    private static final int channel = Channel.BROADCAST.ordinal();
	    private List<EntityID> currentPath = new ArrayList<EntityID>();
	    private EntityID lastVertex;
	    private EntityID civillianToSave;
	    @Override
	    public String toString() {
	        return "Sample ambulance team";
	    }

	    @Override
	    protected void postConnect() {
	        super.postConnect();
	        model.indexClass(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE, StandardEntityURN.BUILDING);
	        unexploredBuildings = new HashSet<EntityID>(buildingIDs);
	    }

	    @Override
	    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
	        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
	            // Subscribe to channel 1
	            sendSubscribe(time, channel);
	        }
	        
	        if(time <= 5){
	    		
				ColeagueInformation meInformation =
	    			new ColeagueInformation(
	    					me().getID().getValue(),
	    					me().getPosition().getValue()
	    					);
				this.sendMessage(time, channel, meInformation);
			}
	        
	        
	        if(me().getBuriedness() > 0)
	        {
	        	System.out.println("ambulance preso em "+me().getPosition().getValue());
	        	TokenInformation t = new TokenInformation(me().getPosition().getValue(), false, MessageType.BLOCKADE);
	        	t.setThreshold(THRESHOLD);
	        	sendMessage(time, channel, t);
	        }
	        
	        updateUnexploredBuildings(changed);
	        
	        // Am I transporting a civilian to a refuge?
	        if (someoneOnBoard()) {
	            // Am I at a refuge?
	            if (location() instanceof Refuge) {
	                // Unload!
	                Logger.info("Unloading");
	                sendUnload(time);
	                return;
	            }
	            else {
	            	
	            	if((currentPath.isEmpty() || lastVertex != currentPath.get(currentPath.size()-1))){
	            		List<EntityID> closestPath = new ArrayList<EntityID>();
	            		
	            		//verifica bloqueio mais proximo para limpar a area
	            		if(refugeIDs.isEmpty() == false)
	            		{
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
	            		}
	            		lastVertex = closestPath.get(closestPath.size()-1);
	            		currentPath = closestPath;
	            	}
	            	
	            	if (currentPath.isEmpty() == false) {
	                    Logger.info("Moving to refuge");
	                    currentPath = this.walk(currentPath);
	                	sendMove(time,currentPath);
	                    return;
	                }
	                // What do I do now? Might as well carry on and see if we can dig someone else out.
	                Logger.debug("Failed to plan path to refuge");
	            }
	        }
	        this.heardMessage(heard);
	    	this.getReceivedMessage().addAll(this.getTargets());
	    	/**
	    	 *  inicio do LA DCOP
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
	    	   	if(msg instanceof TokenInformation && ((TokenInformation)msg).getValueType() == MessageType.RESCUE)
	    	   	{
	    	   		TokenInformation token = (TokenInformation)msg;
	    	   		StandardEntity target = model.getEntity(new EntityID(token.getAssociatedValue()));
	    	   		
	    	   		//se limiar é menor que a capacidade do agente
	    	   		Double capability = 0.0;
	    	   		capability = this.computeCapability(target.getID());
	    	   		if(token.getThreshold() < capability)
	    	   		{
	    	   			token.setCapability(capability);
	    	   			if(token.getPotential() && token.getOwner() == 0)
	    	   			{
	    	   				token.setOwner(me().getID().getValue());
	    	   				this.getPotentialValue().add(token);
	    	   				RetainedInformation retained = new RetainedInformation(token.getAssociatedValue(), me().getID().getValue());
	    	   				this.sendMessage(time,  channel, retained);
	    	   			}else{
	    	   				this.getValue().add(token);
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
	    	   		
	    	   	}
	   	    }
	    	
	    	if( (currentPath.isEmpty() ||
	    			lastVertex != currentPath.get((currentPath.size()-1))) && this.getValue().isEmpty() == false)
	    	{
	    		int index = new Random().nextInt(this.getValue().size());
		    	TokenInformation t = this.getValue().get(index);
		    	Human next = (Human)model.getEntity(new EntityID(t.getAssociatedValue()));
		    	civillianToSave = next.getID();
		    	lastVertex = next.getPosition();
	    		currentPath = this.getDijkstraPath(me().getPosition(), lastVertex);
	    		this.getValue().remove(t);
	    			
	    	}else if(currentPath.isEmpty() == false &&  lastVertex == currentPath.get(currentPath.size()-1)){
	    		Human next = (Human)model.getEntity(civillianToSave);
		    	
	    		if(next.getPosition().equals(location().getID())){
	    			if(next instanceof Civilian && next.getBuriedness() == 0 && !(location() instanceof Refuge))
	    			{
	    				Logger.info("Loading "+next);
	    				sendLoad(time, next.getID());
	    				return;
	    			}
	    				
	    		}else if(next.getBuriedness() > 0)
	    		{
	    			Logger.info("Rescueing "+next);
	    			sendRescue(time,next.getID());
	    			return;
	    		}
	    	}
	    		// Try to move to the target
	        currentPath = this.walk(currentPath);
	        if (currentPath != null) {
	        	Logger.info("Moving to target");
	            sendMove(time, currentPath);
	            return;    
	    	}
	    	
	    	/**
	    	 * fim
	    	 */
	    	
	        // Nothing to do
	    	if(this.getValue().isEmpty() && currentPath.isEmpty())
	    	{
	    		List<EntityID> closestPathToLookUp = new ArrayList<EntityID>();
		        if(unexploredBuildings.isEmpty() == false)
		        {
		        	for(EntityID building : unexploredBuildings)
		        	{
		        		List<EntityID> path = this.getDijkstraPath(me().getPosition(), building);
		        		if(closestPathToLookUp.isEmpty())
		        		{
		        			closestPathToLookUp = path;
		        		}else if(path.size() < closestPathToLookUp.size())
		        		{
		        			closestPathToLookUp = path;
		        		}
		        	}
		        	
		        }
		        if(closestPathToLookUp.isEmpty() == false)
		        {
		        	Logger.info("Searching buildings");
		        	closestPathToLookUp = this.walk(closestPathToLookUp);
		        	sendMove(time, closestPathToLookUp);
		        	return;
		        }
	    		
	    	}
        	
	    }

	    @Override
	    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
	        return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
	    }

	    private boolean someoneOnBoard() {
	        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
	            if (((Human)next).getPosition().equals(getID())) {
	                Logger.debug(next + " is on board");
	                return true;
	            }
	        }
	        return false;
	    }

	    private ArrayList<TokenInformation> getTargets() {
	        List<Human> targets = new ArrayList<Human>();
	        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM)) {
	            Human h = (Human)next;
	            if (h == me()) {
	                continue;
	            }
	            if (h.isHPDefined()
	                && h.isBuriednessDefined()
	                && h.isDamageDefined()
	                && h.isPositionDefined()
	                && h.getHP() > 0
	                && (h.getBuriedness() > 0 || h.getDamage() > 0)) {
	            	
	                targets.add(h);
	            }
	        }
	        Collections.sort(targets, new DistanceSorter(location(), model));
	        ArrayList<TokenInformation> rescueJob = new ArrayList<TokenInformation>();
	        for(Human next: targets)
	        {
	        	Collection<StandardEntity> otherTargets = this.tasksInRange(next.getPosition());
	        	for(StandardEntity oT : otherTargets)
	        	{
	        		System.out.println("StandardURN "+oT.getStandardURN());
	        		System.out.println("Road URN "+StandardEntityURN.ROAD);
	        		if(oT.getStandardURN() == StandardEntityURN.ROAD)
	        		{
	        			Road r = (Road)oT;
	                	if (r.isBlockadesDefined() && !r.getBlockades().isEmpty() && this.getOtherJobs().contains(r.getID()) == false ) {
	                		this.getOtherJobs().add(r.getID());
	                		TokenInformation job = new TokenInformation(r.getID().getValue(), true, MessageType.BLOCKADE);
	                		job.setThreshold(THRESHOLD);
	                    	rescueJob.add(job);
	                	}
	        		}
	        	}
	        	TokenInformation job = new TokenInformation(next.getID().getValue(), false, MessageType.RESCUE);
	        	job.setThreshold(THRESHOLD);
	        	rescueJob.add(job);
	        	
	        }
	        return rescueJob;
	    }

	    private void updateUnexploredBuildings(ChangeSet changed) {
	        for (EntityID next : changed.getChangedEntities()) {
	            unexploredBuildings.remove(next);
	        }
	    }
	}
