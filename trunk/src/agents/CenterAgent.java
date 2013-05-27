package agents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;

import message.Channel;
import message.ColeagueInformation;
import message.LockInformation;
import message.MyMessage;
import message.ReleaseInformation;
import message.RetainedInformation;
import message.Serializer;
import message.TokenInformation;

import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.worldmodel.ChangeSet;

public class CenterAgent extends StandardAgent<Building> {
	
	private static final int channel = Channel.BROADCAST.ordinal();
	
	private ArrayList<MyMessage> receivedMessage = new ArrayList<MyMessage>();
	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.FIRE_STATION,
                StandardEntityURN.AMBULANCE_CENTRE,
                StandardEntityURN.POLICE_OFFICE);
	}

	@Override
    public String toString() {
        return "My center agent";
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channels 1 and 2
            sendSubscribe(time, channel, 2);
        }
        this.heardMessage(heard);
        for(Object msg: this.getReceivedMessage())
    	{
        	if(msg instanceof TokenInformation)
        	{
        		sendMessage(time, channel, (TokenInformation)msg);
        	}else if(msg instanceof ColeagueInformation)
        	{
        		sendMessage(time, channel, (ColeagueInformation)msg);
        	}else if(msg instanceof RetainedInformation)
        	{
        		sendMessage(time, channel, (RetainedInformation)msg);
        	}else if(msg instanceof LockInformation)
        	{
        		sendMessage(time, channel, (LockInformation)msg);
        	}else if(msg instanceof ReleaseInformation)
        	{
        		sendMessage(time, channel, (ReleaseInformation)msg);
        	}
    	}
        sendRest(time);
    }
    
    
    protected void sendMessage(int time, int channel, MyMessage buildingFire) {
        byte[] speak = null;
        try {
            speak = Serializer.serialize(buildingFire);
            sendSpeak(time, channel, speak);
//            System.out.println("Mensagem de enviada com sucesso");
            Logger.debug("Mensagem enviado com sucesso");
        } catch (IOException e) {
        	e.printStackTrace();
            Logger.error("IoException ao gerar mensagem de " + e.getMessage());
//            System.out.println("Erro ao enviar a mensagem   ");
        }
    }

    protected void heardMessage(Collection<Command> heard) {
    	this.getReceivedMessage().clear();
        for (Command next : heard) {
            if (next instanceof AKSpeak) {
                byte[] msg = ((AKSpeak) next).getContent();
                try {
                    Object object = Serializer.deserialize(msg);
                    if (object instanceof TokenInformation) {
                    	TokenInformation tmp = (TokenInformation)object;
                    	this.getReceivedMessage().add(tmp);
                    } else if (object instanceof ColeagueInformation) {
                    	ColeagueInformation tmp = (ColeagueInformation)object;
                    	this.getReceivedMessage().add(tmp);
                    }else if (object instanceof ArrayList) {
                    	for(TokenInformation t : (ArrayList<TokenInformation>)object)
                    	{
                    		this.getReceivedMessage().add(t);
                    	}
                    	  //this.getReceivedMessage().add((MyMessage)object);
                    	//System.out.println("Não entrou em nada");
                    }else{
                    	
                    }
                } catch (IOException e) {
                    Logger.error("Não entendi a mensagem!" + e.getMessage());
                } catch (ClassNotFoundException e) {
                    Logger.error("Mensagem veio com classe que não conheço.");
                }
            }
        }
    }

	public ArrayList<MyMessage> getReceivedMessage() {
		return receivedMessage;
	}

	public void setReceivedMessage(ArrayList<MyMessage> receivedMessage) {
		this.receivedMessage = receivedMessage;
	}

}
