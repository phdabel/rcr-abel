package message;

import java.io.Serializable;

public enum MessageType implements Serializable{
	
	ANNOUNCE("Announcement"),
	ACCOMPLISHED_TASK("Accomplished Task"),
	TOKEN("Token");
	
	String name;
	
	private MessageType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
