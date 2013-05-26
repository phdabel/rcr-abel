package worldmodel.jobs;

import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class Token extends StandardEntity {

	private Double threshold = .5;
	private EntityID owner;
	private EntityID value;
	private Boolean potential = false;
	
	public Token(EntityID id) {
		super(id);
		this.setValue(id);
	}
	
	public Token(EntityID id, Double threshold)
	{
		super(id);
		this.setValue(id);
		this.setThreshold(threshold);
		
	}

	@Override
	public StandardEntityURN getStandardURN() {
		return StandardEntityURN.WORLD;
	}

	@Override
	protected Entity copyImpl() {
		return new Token(getID());
	}

	public Double getThreshold() {
		return threshold;
	}

	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}

	public EntityID getOwner() {
		return owner;
	}

	public void setOwner(EntityID owner) {
		this.owner = owner;
	}

	public EntityID getValue() {
		return value;
	}

	public void setValue(EntityID value) {
		this.value = value;
	}

	public Boolean getPotential() {
		return potential;
	}

	public void setPotential(Boolean potential) {
		this.potential = potential;
	}

}
