package agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;

import message.Channel;
import message.MyMessage;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.DistanceSorter;

public class AmbulanceTeamAgent extends MyAbstractAgent<PoliceForce>{
	
	private Collection<EntityID> unexploredBuildings;
	private static final int channel = Channel.BROADCAST.ordinal();
	
	private List<EntityID> currentPath = new ArrayList<EntityID>();
    private Queue<AgentState> stateQueue = new LinkedList<AgentState>();
    private List<EntityID> lastPath = new ArrayList<EntityID>();
    private Collection<Human> listTargets = new ArrayList<Human>();
    private Boolean pathDefined = false;
    
    
	@Override
	public String toString(){
		return "My Ambulance Team";
	}
	
	@Override
	protected void postConnect(){
		super.postConnect();
		model.indexClass(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE, StandardEntityURN.BUILDING);
		this.unexploredBuildings = new HashSet<EntityID>(buildingIDs);
		this.setCommunicationChannel(channel);
	}
	
	@Override
	protected void think(int time, ChangeSet changed, Collection<Command> heard) {
		if(time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY))
		{
			sendSubscribe(time, this.getCommunicationChannel());
			mapTmp = this.map;
		}
		
		// ESCUTA MENSAGENS
		this.heardMessage(heard);
    	
        /**
         * PLANEJAMENTO
         */
        for(MyMessage msg: this.getReceivedMessage())
    	{
        
    	   	
    	}
        
        /**
         * AÇÃO e CONTROLE
         * Máquina de Estados
         * Aqui começa a execução das ações na pilha de ações
         */
        if(!stateQueue.isEmpty()){
        	
        	AgentState currentAction = stateQueue.peek();
        	switch(currentAction.getState())
        	{
        		case "RandomWalk":
        			/**
        			 * Quando agente não tem caminho para ir
        			 */
        			if(currentPath.isEmpty() && pathDefined == false)
        			{
        				currentPath = this.walk(currentPath, me().getPosition());
        				pathDefined = true;
        				this.addBeginingQueue(new AgentState("LookingCivillian"));
        				sendMove(time, currentPath);
        				return;
        				
        			}else if(currentPath.size() <= 2) //quando agente está terminando seu caminho
        			{
        				stateQueue.poll();
        				pathDefined = false;
        				this.addBeginingQueue(new AgentState("LookingCivillian"));
        				sendMove(time, currentPath);
        				currentPath.clear();
        				return;
        			}else if(pathDefined == true){ //quando agente está executando o caminho
        				currentPath = this.walk(currentPath,  me().getPosition());
        				this.addBeginingQueue(new AgentState("LookingCivillian"));
        				sendMove(time, currentPath);
        			}
        			break;
        		case "LookingCivillian": //procura por civis e os insere na variável listTarget SE já não estiverem
        			listTargets = getTargets();
        			if(stateQueue.peek().getState() == "LookingCivillian")
        			{
        				stateQueue.poll();
        			}
        			if(!listTargets.isEmpty())
        			{
        				Human target = ((ArrayList<Human>)listTargets).get(0);
        				this.addBeginingQueue(new AgentState("Rescue"));
        				this.addBeginingQueue(new AgentState("Walk", target.getID()));
        			}
        			break;
        		case "Walk":
        			//se o caminho está vazio e indefinido, faz uma rota até um alvo
        			if(currentPath.isEmpty() && pathDefined == false)
        			{
        				this.mapTmp = this.map;
        				currentPath = this.getDijkstraPath(me().getPosition(), currentAction.getId());
        				pathDefined = true;
        				lastPath = currentPath;
        				currentPath = this.walk(currentPath, me().getPosition());
        				if(lastPath.size() == currentPath.size())
        				{
        					/**
        					 * se o caminho atual é igual ao anterior
        					 * ou está bloqueado ou está junto com um algo
        					 */
        					this.addBeginingQueue(new AgentState("TestBlockOrSave"));
        				}
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() > 2 && pathDefined == true)
        			{
        				lastPath = currentPath;
        				currentPath = this.walk(currentPath,  me().getPosition());
        				if(lastPath.size() == currentPath.size())
        				{
        					/**
        					 * se o caminho atual é igual ao anterior
        					 * ou está bloqueado ou está junto com um algo
        					 */
        					this.addBeginingQueue(new AgentState("TestBlockOrSave"));
        				}
        				sendMove(time, currentPath);
        				return;
        			}else if(currentPath.size() <= 2)
        			{
        				stateQueue.poll();
        				sendMove(time, currentPath);
        				currentPath.clear();
        				return;
        			}
        			break;
        		case "TestBlockOrSave":
        			StandardEntity position = model.getEntity(me().getPosition()); 
        			break;
        			
        	
        	
        	}
        	
        	
        	
        }
        
        
		
	}

	@Override
	protected void stopCurrentTask() {
		// TODO Auto-generated method stub
		
	}
	
	
	protected void verifyBlockedOrTarget(EntityID minhaPos, EntityID alvo){
		StandardEntity position = model.getEntity(minhaPos);
		if(position instanceof Building){
			Building b = (Building)position;
			if(b.isBlockadesDefined()){
				this.mapTmp.removeVertex(b.getID());
				currentPath = this.getDijkstraPath(minhaPos, alvo, this.mapTmp);
				System.out.println("Envia mensagem para policial");
			}else if(b.isOnFire()){
				System.out.println("Envia mensagem para os bombeiros");
			}
		}else if(position instanceof Road){
			Road r = (Road)position;
			if(r.isBlockadesDefined()){
				this.mapTmp.removeVertex(r.getID());
				currentPath = this.getDijkstraPath(minhaPos, alvo, this.mapTmp);
				System.out.println("Envia mensagem para policial");
			}
		}
		
	}

	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
	}
	
	/**
	 * verifica se ha alguem junto ao agente
	 * @return
	 */
	private boolean someoneOnBoard() {
        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
            if (((Human)next).getPosition().equals(getID())) {
                return true;
            }
        }
        return false;
    }
	
	/**
	 * retorna a lista de alvos humanos para salvamento
	 * @return
	 */
	private Collection<Human> getTargets() {
        List<Human> targets = new ArrayList<Human>();
        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM)) {
            Human h = (Human)next;
            if (h == me()) {
                continue;
            }
            if ( !listTargets.contains(h) &&
            	   (h.isHPDefined()
                && h.isBuriednessDefined()
                && h.isDamageDefined()
                && h.isPositionDefined()
                && h.getHP() > 0
                && (h.getBuriedness() > 0 || h.getDamage() > 0)) ) {
                targets.add(h);
            }
        }
        Collections.sort(targets, new DistanceSorter(location(), model));
        return targets;
    }
	
	/**
	 * adiciona estado ao inicio da fila
	 * @param newState Novo Estado
	 */
	protected void addBeginingQueue(AgentState newState)
    {
    	Queue<AgentState> tmpQueue = new LinkedList<AgentState>();
		tmpQueue.addAll(stateQueue);
		stateQueue.clear();
		stateQueue.add(newState);
		stateQueue.addAll(tmpQueue);
    }
    /**
     * imprime a fila
     */
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


}
