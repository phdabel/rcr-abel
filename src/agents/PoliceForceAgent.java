package agents;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Random;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.GeometryTools2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Line2D;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Area;

public class PoliceForceAgent extends MyAbstractAgent<PoliceForce> {
	
	private static final String DISTANCE_KEY = "clear.repair.distance";
    private List<EntityID> currentPath = new ArrayList<EntityID>();
    private Queue<AgentState> stateQueue = new LinkedList<AgentState>();
    private List<EntityID> lastPath = new ArrayList<EntityID>();
    private int distance;
    private Boolean cleanRefuge = false;
    private Boolean pathDefined = false;
    
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
            sendSubscribe(time, this.getCommunicationChannel());
            
           }
		
		//enviando informação sobre quem sou eu para os colegas
   		
    	//recebendo mensagens
    	this.heardMessage(heard);
    	
        /**
         * PLANEJAMENTO
         */
        for(Object msg: this.getReceivedMessage())
    	{
        
    	   	
    	}
        /**
         * INSERÇÃO DE VALORES NA FILA
         */
        if(		(!this.getValue().isEmpty() && this.stateQueue.isEmpty() ) 
        		|| 
        		(!this.getValue().isEmpty() && stateQueue.peek().getState() == "RandomWalk")
           )
        {
        	if(!stateQueue.isEmpty() && stateQueue.peek().getState() == "RandomWalk"){
        		stateQueue.remove(stateQueue.peek());
        	}
        	TokenInformation tmp = this.getValue().get(this.getValue().size() - 1);
        	EntityID tmpID = new EntityID(tmp.getAssociatedValue());
        	this.getValue().remove(this.getValue().size() - 1);
        	
        	if(me().getPosition() != tmpID)
        	{
        		stateQueue.add(new AgentState("Walk", tmpID));
        		stateQueue.add(new AgentState("Unblock", tmpID));
        		this.printQueue();
        		
        	}else{
        		stateQueue.add(new AgentState("Unblock", tmpID));
        		this.printQueue();
        	}
        		
        }else if (this.stateQueue.isEmpty() && time > 5){
        	stateQueue.add(new AgentState("RandomWalk"));
        	this.printQueue();
        }
        
        /**
         * AÇÃO e CONTROLE
         * Máquina de Estados
         * Aqui começa a execução das ações na pilha de ações
         */
        if(!stateQueue.isEmpty())
        {
        	AgentState currentAction = stateQueue.peek();
        	System.out.println(stateQueue.peek().getState());
        	switch(currentAction.getState())
        	{
        		case "RandomWalk":
        			System.out.println(currentPath);
        			//System.out.println("Random Walk da pilha de estados.");
        			if(currentPath.isEmpty() && pathDefined == false)
        			{
        				currentPath = this.walk(currentPath);
        				pathDefined = true;
        				this.addBeginingQueue(new AgentState("LookingNearBlockade"));
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() <= 2){
        				stateQueue.poll();
        				pathDefined = false;
        				this.addBeginingQueue(new AgentState("LookingNearBlockade"));
        				sendMove(time, currentPath);
        				currentPath.clear();
        				return;
        			}else if(pathDefined == true){
        				currentPath = this.walk(currentPath);
        				this.addBeginingQueue(new AgentState("LookingNearBlockade"));
        				sendMove(time, currentPath);
        			}
        			break;
        		case "LookingNearBlockade":
        			Blockade target = getTargetBlockade();
        			if(stateQueue.peek().getState() == "LookingNearBlockade")
        			{
        					stateQueue.poll();
        			}
        			if(target != null)
        			{
        				this.addBeginingQueue(new AgentState("Unblock"));
        				this.addBeginingQueue(new AgentState("Walk", target.getPosition()));
        			}
        			break;
        		case "Walk":
        			System.out.println(currentAction.getId());
        			System.out.println("caminho dentro do walk "+currentPath);
        			if(currentPath.isEmpty() && pathDefined == false){
        				currentPath = search.breadthFirstSearch(me().getPosition(), currentAction.getId());
        				//currentPath = this.getDijkstraPath(me().getPosition(), currentAction.getId());
        				pathDefined = true;
        				lastPath = currentPath;
        				currentPath = this.walk(currentPath);
        				System.out.println("Last Path "+lastPath);
        				System.out.println("Current Path "+currentPath);
        				System.out.println(lastPath.size() == currentPath.size());
        				if(lastPath.size() == currentPath.size())
        				{
        					this.addBeginingQueue(new AgentState("Unblock"));
        				}
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() >2 && pathDefined == true)
        			{
        				lastPath = currentPath;
        				currentPath = this.walk(currentPath);
        				System.out.println("Last Path "+lastPath);
        				System.out.println("Current Path "+currentPath);
        				System.out.println(lastPath.size() == currentPath.size());
        				if(lastPath.size() == currentPath.size())
        				{
        					this.addBeginingQueue(new AgentState("Unblock"));
        				}
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() <= 2){
        				stateQueue.poll();
        				sendMove(time,currentPath);
        				pathDefined = false;
        				return;
        			}
        			
        			break;
        		
        		case "Unblock":
        			Blockade newTarget = getTargetBlockade();
        			if(newTarget != null && newTarget.isPositionDefined()){
        				System.out.println("ID "+newTarget.getID());
        				sendClear(time, newTarget.getID());
        			}else{
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
        System.out.println("Agente Policial  "+me().getID().getValue());
        System.out.println("Posição Atual "+me().getPosition());
        System.out.println("Caminho a seguir "+currentPath);
        System.out.println("Lista de values "+this.getValue());
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
    
    protected void printQueue()
    {
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

	@Override
	protected void stopCurrentTask() {
		// TODO Auto-generated method stub
		
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