package message;

public enum MessageType {
	
	BUILDING_FIRE("Building on Fire"),
	BLOCKADE("Blockade"),
	RESCUE("Civillian Rescue");
	
	String name;
	
	private MessageType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
