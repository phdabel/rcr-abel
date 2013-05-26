package agents;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

import message.Channel;
import message.TokenInformation;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;

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
	        for (Command next : heard) {
	            Logger.debug("Heard " + next);
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
	            	
	                // Move to a closest refuge (djikstra algorithm)
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
	            	
	            	if (closestPath != null) {
	                    Logger.info("Moving to refuge");
	                    sendMove(time, closestPath);
	                    return;
	                }
	                // What do I do now? Might as well carry on and see if we can dig someone else out.
	                Logger.debug("Failed to plan path to refuge");
	            }
	        }
	        // Go through targets (sorted by distance) and check for things we can do
	        for (Human next : getTargets()) {
	            if (next.getPosition().equals(location().getID())) {
	                // Targets in the same place might need rescueing or loading
	                if ((next instanceof Civilian) && next.getBuriedness() == 0 && !(location() instanceof Refuge)) {
	                    // Load
	                    Logger.info("Loading " + next);
	                    sendLoad(time, next.getID());
	                    return;
	                }
	                if (next.getBuriedness() > 0) {
	                    // Rescue
	                    Logger.info("Rescueing " + next);
	                    sendRescue(time, next.getID());
	                    return;
	                }
	            }
	            else {
	                // Try to move to the target
	            	List<EntityID> path = this.getDijkstraPath(me().getPosition(), next.getPosition());
	                if (path != null) {
	                    Logger.info("Moving to target");
	                    sendMove(time, path);
	                    return;
	                }
	            }
	        }
	        // Nothing to do
	        List<EntityID> closestPathToLookUp = new ArrayList<EntityID>();
	        if(unexploredBuildings.isEmpty() == false)
	        {
	        	for(EntityID building : unexploredBuildings)
	        	{	
        			List<EntityID> path = this.getDijkstraPath(me().getPosition(), building);
        			if(closestPathToLookUp.isEmpty())
        			{
        				closestPathToLookUp = path;
        			}else{
        				if(path.size() < closestPathToLookUp.size())
        				{
        					closestPathToLookUp = path;
        				}
        			}
        		}
        	
        		if (closestPathToLookUp != null) {
                	Logger.info("Searching buildings");
                	sendMove(time, closestPathToLookUp);
                	return;
            	}
	        }
	        
	        Logger.info("Moving randomly");
	        sendMove(time, randomWalk());
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

	    private List<Human> getTargets() {
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
	        /*for(Human next: targets)
	        {
	        	if(se tiver bloqueio proximo cria token para bloqueio e token para civil)
	        	{
	        		
	        	}elseif(){
	        		
	        	}
	        }*/
	        return targets;
	    }

	    private void updateUnexploredBuildings(ChangeSet changed) {
	        for (EntityID next : changed.getChangedEntities()) {
	            unexploredBuildings.remove(next);
	        }
	    }
	}
