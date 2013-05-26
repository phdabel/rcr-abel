package worldmodel.jobs;

public enum JobType {
	
	BURNING_BUILDING(0),
	BLOCKADE_ROAD(1),
	CIVILIAN_RESCUE(2);
	
	public int jobValue;
	JobType(int value)
	{
		this.jobValue = value;
	}

}
