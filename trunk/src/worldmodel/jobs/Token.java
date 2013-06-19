package worldmodel.jobs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Token implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
	private Task t;
	private Double threshold;
	private Double holderCapability = 0.0;
	private List<Integer> visitedAgents = new ArrayList<Integer>();
	
	
	public Token(Integer id, Task t)
	{
		this.setId(id);
		this.setTask(t);
	}
	
	public List<Integer> getVisitedAgents() {
		return visitedAgents;
	}
	public void setVisitedAgents(List<Integer> visitedAgents) {
		this.visitedAgents = visitedAgents;
	}
	public void addAgent(Integer agent)
	{
		this.visitedAgents.add(agent);
	}
	public Task getTask() {
		return t;
	}
	public void setTask(Task t) {
		this.t = t;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}

	public Double getThreshold() {
		return threshold;
	}

	public void setThreshold(Double threshold) {
		this.threshold = threshold;
	}

	public Double getHolderCapability() {
		return holderCapability;
	}

	public void setHolderCapability(Double holderCapability) {
		this.holderCapability = holderCapability;
	}

}
