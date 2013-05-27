package message;

import rescuecore2.worldmodel.EntityID;

public class RetainedInformation extends MyMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int value;
	private Integer sender;
	
	public RetainedInformation(int associatedValue, Integer sender)
	{
		this.setId(associatedValue);
		this.setValue(associatedValue);
		this.setSender(sender);
	}


	public int getValue() {
		return value;
	}


	public void setValue(int value) {
		this.value = value;
	}


	public Integer getSender() {
		return sender;
	}


	public void setSender(Integer sender) {
		this.sender = sender;
	}

}
