package message;

public class RetainedInformation extends MyMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int value;	
	
	public RetainedInformation(int associatedValue)
	{
		this.setId(associatedValue);
		this.setValue(associatedValue);
	}


	public int getValue() {
		return value;
	}


	public void setValue(int value) {
		this.value = value;
	}

}
