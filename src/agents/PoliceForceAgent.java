package agents;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import message.Channel;
import message.ColeagueInformation;
import message.LockInformation;
import message.MessageType;
import message.ReleaseInformation;
import message.RetainedInformation;
import message.TokenInformation;

import org.jgrapht.Graph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.KruskalMinimumSpanningTree;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Line2D;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Area;

/**
   A sample police force agent.
 */
public class PoliceForceAgent extends MyAbstractAgent<PoliceForce> {
	
	private static final int channel = Channel.BROADCAST.ordinal();
    private static final String DISTANCE_KEY = "clear.repair.distance";

    
    private int distance;
    private Boolean cleanRefuge = false;
    @Override
    public String toString() {
        return "Sample police force";
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.ROAD);
        distance = config.getIntValue(DISTANCE_KEY);
        this.setCommunicationChannel(channel);
    }
    

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channel 1
        	sendSubscribe(time, this.getCommunicationChannel());
        }
        
        this.heardMessage(heard);
        //acrescentar as mensagens, as tasks obtidas pelo proprio agente
        //this.getReceivedMessage().addAll(this.getFireJob());
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
    	   	if(msg instanceof TokenInformation && ((TokenInformation)msg).getValueType() == MessageType.BLOCKADE)
    	   	{
    	   		TokenInformation token = (TokenInformation)msg;    	   		
    	   		System.out.println("Token Recebido "+token.getId()+" eu sou policial "+me().getID().getValue());
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
    	   				RetainedInformation retained = new RetainedInformation(token.getAssociatedValue());
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
    	   	ArrayList<TokenInformation> removeList = new ArrayList<TokenInformation>();
        	for(TokenInformation t : this.getValue())
        	{
        		Road r = (Road)model.getEntity(new EntityID(t.getAssociatedValue()));
        		if(r.isBlockadesDefined()){
        			this.getDijkstraPath(me().getPosition(), r.getID());
        			sendClear(time, r.getID());
        		}else{
        			removeList.add(t);
        		}
        	}
        	this.getValue().removeAll(removeList);
    	}
        
        
        //Am I near a blockade?
    	Blockade someTarget = getTargetBlockade();
    	if (someTarget != null) {
        	Logger.info("Clearing blockade " + someTarget);
        	sendClear(time, someTarget.getID());
        	return;
    	}
    	
        if(cleanRefuge == false){
        	//verifica bloqueio mais proximo para limpar a area
        	List<EntityID> closestPath = new ArrayList<EntityID>();
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
    			sendMove(time, closestPath);
    		// 	Am I near a blockade?
        		Blockade target = getTargetBlockade();
        		if (target != null) {
            		Logger.info("Clearing blockade " + target);
            		sendClear(time, target.getID());
            		return;
        		}
        	}
        	cleanRefuge = true;
        }
        
        
        // Plan a path to a blocked area
        
        //List<EntityID> path = search.breadthFirstSearch(me().getPosition(), getBlockedRoads());
        if(getBlockedRoads().isEmpty() == false)
        {
        	List<EntityID> path = getDijkstraPath(me().getPosition(), getBlockedRoads().get(0));
        
        	if (path != null) {
            	Logger.info("Moving to target");
            	Road r = (Road)model.getEntity(path.get(path.size() - 1));
            	Blockade b = getTargetBlockade(r, -1);
            	sendMove(time, path, b.getX(), b.getY());
            	Logger.debug("Path: " + path);
            	Logger.debug("Target coordinates: " + b.getX() + ", " + b.getY());
            	return;
        	}
        }
        if(this.getValue().isEmpty()){
        	Logger.debug("Couldn't plan a path to a blocked road");
        	Logger.info("Moving randomly");
        	sendMove(time, randomWalk());
        }
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.POLICE_FORCE);
    }
    

    private List<EntityID> getBlockedRoads() {
        Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.ROAD);
        List<EntityID> result = new ArrayList<EntityID>();
        for (StandardEntity next : e) {
            Road r = (Road)next;
            if (r.isBlockadesDefined() && !r.getBlockades().isEmpty()) {
                result.add(r.getID());
            }
        }
        return result;
    }

    private Blockade getTargetBlockade() {
        Logger.debug("Looking for target blockade");
        Area location = (Area)location();
        Logger.debug("Looking in current location");
        Blockade result = getTargetBlockade(location, distance);
        if (result != null) {
            return result;
        }
        Logger.debug("Looking in neighbouring locations");
        for (EntityID next : location.getNeighbours()) {
            location = (Area)model.getEntity(next);
            result = getTargetBlockade(location, distance);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Blockade getTargetBlockade(Area area, int maxDistance) {
        //        Logger.debug("Looking for nearest blockade in " + area);
        if (area == null || !area.isBlockadesDefined()) {
            //            Logger.debug("Blockades undefined");
            return null;
        }
        List<EntityID> ids = area.getBlockades();
        // Find the first blockade that is in range.
        int x = me().getX();
        int y = me().getY();
        for (EntityID next : ids) {
            Blockade b = (Blockade)model.getEntity(next);
            double d = findDistanceTo(b, x, y);
            //            Logger.debug("Distance to " + b + " = " + d);
            if (maxDistance < 0 || d < maxDistance) {
                //                Logger.debug("In range");
                return b;
            }
        }
        //        Logger.debug("No blockades in range");
        return null;
    }
    

    private int findDistanceTo(Blockade b, int x, int y) {
        //        Logger.debug("Finding distance to " + b + " from " + x + ", " + y);
        List<Line2D> lines = GeometryTools2D.pointsToLines(GeometryTools2D.vertexArrayToPoints(b.getApexes()), true);
        double best = Double.MAX_VALUE;
        Point2D origin = new Point2D(x, y);
        for (Line2D next : lines) {
            Point2D closest = GeometryTools2D.getClosestPointOnSegment(next, origin);
            double d = GeometryTools2D.getDistance(origin, closest);
            //            Logger.debug("Next line: " + next + ", closest point: " + closest + ", distance: " + d);
            if (d < best) {
                best = d;
                //                Logger.debug("New best distance");
            }

        }
        return (int)best;
    }

    /**
       Get the blockade that is nearest this agent.
       @return The EntityID of the nearest blockade, or null if there are no blockades in the agents current location.
    */
    /*
    public EntityID getNearestBlockade() {
        return getNearestBlockade((Area)location(), me().getX(), me().getY());
    }
    */

    /**
       Get the blockade that is nearest a point.
       @param area The area to check.
       @param x The X coordinate to look up.
       @param y The X coordinate to look up.
       @return The EntityID of the nearest blockade, or null if there are no blockades in this area.
    */
    /*
    public EntityID getNearestBlockade(Area area, int x, int y) {
        double bestDistance = 0;
        EntityID best = null;
        Logger.debug("Finding nearest blockade");
        if (area.isBlockadesDefined()) {
            for (EntityID blockadeID : area.getBlockades()) {
                Logger.debug("Checking " + blockadeID);
                StandardEntity entity = model.getEntity(blockadeID);
                Logger.debug("Found " + entity);
                if (entity == null) {
                    continue;
                }
                Pair<Integer, Integer> location = entity.getLocation(model);
                Logger.debug("Location: " + location);
                if (location == null) {
                    continue;
                }
                double dx = location.first() - x;
                double dy = location.second() - y;
                double distance = Math.hypot(dx, dy);
                if (best == null || distance < bestDistance) {
                    bestDistance = distance;
                    best = entity.getID();
                }
            }
        }
        Logger.debug("Nearest blockade: " + best);
        return best;
    }
    */
}