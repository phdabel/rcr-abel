package agents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;

import message.Channel;
import message.MyMessage;

import rescuecore2.messages.Command;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import sample.DistanceSorter;
import worldmodel.jobs.Object;


public class AmbulanceTeamAgent extends MyAbstractAgent<AmbulanceTeam>{
	
	private Collection<EntityID> unexploredBuildings;
	private static final int channel = Channel.BROADCAST.ordinal();
	
	private EntityID loadedAgent;
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
		this.setUnexploredBuildings(new HashSet<EntityID>(buildingIDs));
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
         * INSERÇÃO DE VALORES NA FILA
         */
        if(this.currentTask != null && this.stateQueue.isEmpty() && this.currentTask.getObject() == Object.RESCUE)
        {
        	/*
        	if(!stateQueue.isEmpty() && stateQueue.peek().getState() == "RandomWalk"){
        		stateQueue.remove(stateQueue.peek());
        	}
        	Task tmp = this.currentTask;
        	EntityID tmpID = new EntityID(tmp.getId());
        	
        	if(me().getPosition() != tmpID)
        	{
        		stateQueue.add(new AgentState("Walk", tmpID));
        		stateQueue.add(new AgentState("Unblock", tmpID));        		
        	}else{
        		stateQueue.add(new AgentState("Unblock", tmpID));
        		
        	}*/
        		
        }else if (this.stateQueue.isEmpty() && time > 5){
        	stateQueue.add(new AgentState("RandomWalk"));
        	
        }
        /**
         * fim da inserção de valores
         */
        
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
        				this.addBeginingQueue(new AgentState("LookingCivilian"));
        				sendMove(time, currentPath);
        				return;
        				
        			}else if(currentPath.size() <= 2) //quando agente está terminando seu caminho
        			{
        				stateQueue.poll();
        				pathDefined = false;
        				this.addBeginingQueue(new AgentState("LookingCivilian"));
        				sendMove(time, currentPath);
        				currentPath.clear();
        				return;
        			}else if(pathDefined == true){ //quando agente está executando o caminho
        				currentPath = this.walk(currentPath,  me().getPosition());
        				this.addBeginingQueue(new AgentState("LookingCivilian"));
        				sendMove(time, currentPath);
        			}
        			break;
        		case "LookingCivilian": //procura por civis e os insere na variável listTarget SE já não estiverem
        			listTargets = getTargets();
        			if(stateQueue.peek().getState() == "LookingCivilian")
        			{
        				stateQueue.poll();
        			}
        			if(!listTargets.isEmpty())
        			{
        				Human target = ((ArrayList<Human>)listTargets).get(0);
        				this.addBeginingQueue(new AgentState("Rescue", target.getID()));
        				this.addBeginingQueue(new AgentState("Walk", target.getID()));
        			}
        			break;
        		case "Walk":
        			Human h = (Human)model.getEntity(currentAction.getId());
        			//se o caminho está vazio e indefinido, faz uma rota até um alvo
        			if(currentPath.isEmpty() && pathDefined == false)
        			{
        				this.mapTmp = this.map;
        				currentPath = this.getDijkstraPath(me().getPosition(), h.getPosition());
        				pathDefined = true;
        				lastPath = currentPath;
        				currentPath = this.walk(currentPath, me().getPosition());
        				if(lastPath.size() == currentPath.size())
        				{
        					/**
        					 * se o caminho atual é igual ao anterior
        					 * ou está bloqueado ou está junto com um algo
        					 */
        					this.addBeginingQueue(new AgentState("TestBlockOrSave", h.getID()));
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
        					this.addBeginingQueue(new AgentState("TestBlockOrSave", h.getID()));
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
        			Human someH = (Human)model.getEntity(currentAction.getId());
        			verifyBlockedOrTarget(me().getPosition(), someH);
        			break;
        		case "LoadCivilian":
        			Human targetH1 = (Human)model.getEntity(currentAction.getId());
        			if(me().getPosition() == targetH1.getPosition()){
        				this.setLoadedAgent(currentAction.getId());
        				this.stateQueue.poll();
        				this.addBeginingQueue(new AgentState("Unload"));
        				this.addBeginingQueue(new AgentState("Walk", this.refugeIDs.get(0)));
        				sendLoad(time, currentAction.getId());
                    	return;
        			}                    	
        			break;
        		case "RescueCivilian":
        			Human targetHRescue = (Human)model.getEntity(currentAction.getId());
        			if(me().getPosition() == targetHRescue.getPosition() && targetHRescue.getBuriedness() > 0){
            			sendRescue(time, targetHRescue.getID());
                        return;	
        			}else if(me().getPosition() == targetHRescue.getPosition()){
        				this.stateQueue.poll();
        				this.addBeginingQueue(new AgentState("LoadCivilian", targetHRescue.getID()));
        			}
        			break;
        		case "Unload":
        			if(location() instanceof Refuge && this.someoneOnBoard())
        			{
        				this.setLoadedAgent(null);
        				this.stateQueue.poll();
        				sendUnload(time);
                        return;
        			}
        			break;
        	}        	
        }
        /**
         * fim da máquina de estados
         */
        
        
		
	}

	@Override
	protected void stopCurrentTask() {
		this.currentTask = null;
		this.stateQueue.clear();
	}
	
	
	protected void verifyBlockedOrTarget(EntityID minhaPos, Human alvo){
		StandardEntity position = model.getEntity(minhaPos);
		if(position instanceof Building){
			Building b = (Building)position;
			if(b.isBlockadesDefined()){
				this.mapTmp.removeVertex(b.getID());
				ConnectivityInspector<EntityID, DefaultWeightedEdge> test = new ConnectivityInspector<EntityID, DefaultWeightedEdge>(this.mapTmp);
				if(test.pathExists(minhaPos, alvo.getPosition())){
					currentPath = this.getDijkstraPath(minhaPos, alvo.getPosition(), this.mapTmp);
				}else{
					this.mapTmp = this.map;
					currentPath = this.getDijkstraPath(minhaPos, alvo.getPosition(), this.mapTmp);
				}
				System.out.println("Envia mensagem para policial");
			}else if(b.isOnFire()){
				System.out.println("Envia mensagem para os bombeiros");
			}
			this.stateQueue.poll();
		}else if(position instanceof Road){
			Road r = (Road)position;
			if(r.isBlockadesDefined()){
				ConnectivityInspector<EntityID, DefaultWeightedEdge> test = new ConnectivityInspector<EntityID, DefaultWeightedEdge>(this.mapTmp);
				if(test.pathExists(minhaPos, alvo.getPosition())){
					currentPath = this.getDijkstraPath(minhaPos, alvo.getPosition(), this.mapTmp);
				}else{
					this.mapTmp = this.map;
					currentPath = this.getDijkstraPath(minhaPos, alvo.getPosition(), this.mapTmp);
				}
				System.out.println("Envia mensagem para policial");
			}
			this.stateQueue.poll();
		}else if(minhaPos == alvo.getPosition()){
			this.stateQueue.poll();
			if(alvo.getBuriedness() == 0 && !(location() instanceof Refuge)){
				//se o alvo não está soterrado, carrega o algo
				this.addBeginingQueue(new AgentState("LoadCivilian", alvo.getID()));
			}else if(alvo.getBuriedness() > 0){
				//se ele estiver soterrado, resgata
				this.addBeginingQueue(new AgentState("RescueCivilian", alvo.getID()));
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

	public EntityID getLoadedAgent() {
		return loadedAgent;
	}

	public void setLoadedAgent(EntityID loadedAgent) {
		this.loadedAgent = loadedAgent;
	}

	public Collection<EntityID> getUnexploredBuildings() {
		return unexploredBuildings;
	}

	public void setUnexploredBuildings(Collection<EntityID> unexploredBuildings) {
		this.unexploredBuildings = unexploredBuildings;
	}


}
