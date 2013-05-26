package worldmodel.jobs;

import rescuecore2.standard.entities.Building;


public class FireJob  {
	
	private Building burningBuilding;

	public FireJob()
	{
		
	}
	
	public FireJob(Building b)
	{
		this.setBurningBuilding(b);
	}

	/**
	 * returns the building associated to this job
	 * @return Building
	 */
	public Building getBurningBuilding() {
		return burningBuilding;
	}

	/**
	 * sets a burning building
	 * @param burningBuilding
	 */
	public void setBurningBuilding(Building burningBuilding) {
		this.burningBuilding = burningBuilding;
	}
	
	/**
	 * Method to return the Fieryness of the building.
	 * 0 - unburnt
	 * 1 - heating
	 * 2 - burning
	 * 3 - inferno
	 * 4 - water damage
	 * 5 - minor damage
	 * 6 - moderate damage
	 * 7 - severe damage
	 * 8 - burnt out
	 * @return int
	 */
	public int getBuildingStatus()
	{
		return this.burningBuilding.getFieryness();
	}
	
	/**
	 * returns the composition of building 
	 * 0 wood
	 * 1 steel
	 * 2 concrete
	 * @return int
	 */
	public int getBuildingCode()
	{
		return this.burningBuilding.getBuildingCode();
	}
	
	

}
