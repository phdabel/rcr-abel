package message;

import java.io.Serializable;
/**
 *
 * @author rick
 */
public abstract class MyMessage implements Serializable{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int id;
    private int position;
    private MessageType valueType;

    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.id;
        hash = 97 * hash + this.position;
        return hash;
    }

	public MessageType getValueType() {
		return valueType;
	}

	public void setValueType(MessageType classInformation) {
		this.valueType = classInformation;
	}

}
