package agents;

import java.util.Collection;
import java.util.EnumSet;


import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;

public class CenterAgent extends StandardAgent<Building> {
	
	
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
    }

}
