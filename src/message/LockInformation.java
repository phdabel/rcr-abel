package message;

import java.util.ArrayList;
import java.util.List;

public class LockInformation extends MyMessage {
	
private static final long serialVersionUID = 1L;
	
	private TokenInformation token;
	private List<Integer> agents = new ArrayList<Integer>();
	
	public LockInformation(TokenInformation token)
	{
		this.setId(token.getId());
		this.setToken(token);
	}

	public TokenInformation getToken() {
		return token;
	}

	public void setToken(TokenInformation token) {
		this.token = token;
	}

	public List<Integer> getAgents() {
		return agents;
	}

	public void setAgents(List<Integer> agents) {
		this.agents = agents;
	}
	
	


}
