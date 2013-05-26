package message;

public class TokenInformation extends MyMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int associatedValue;
	private Double threshold = 0.9;
	private Double capability = 0.0;
	private Boolean potential = false;
	private int owner = 0;
	
	public TokenInformation(int value, Boolean potential, MessageType valueType)
	{
		this.setId(value);
		this.setAssociatedValue(value);
		this.setPotential(potential);
		this.setValueType(valueType);
		
	}
	
	public int getAssociatedValue() {
		return associatedValue;
	}
	public void setAssociatedValue(int associatedValue) {
		this.associatedValue = associatedValue;
	}
	public Double getThreshold() {
		return threshold;
	}
	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}
	public Boolean getPotential() {
		return potential;
	}
	public void setPotential(Boolean potential) {
		this.potential = potential;
	}
	public int getOwner() {
		return owner;
	}
	public void setOwner(int owner) {
		this.owner = owner;
	}

	public Double getCapability() {
		return capability;
	}

	public void setCapability(Double capability) {
		this.capability = capability;
	}

}
