package message;

public class ReleaseInformation extends MyMessage {

	private static final long serialVersionUID = 1L;
	
	private TokenInformation token;
	
	public ReleaseInformation(TokenInformation token)
	{
		this.setId(token.getAssociatedValue());
		this.setToken(token);
	}


	public TokenInformation getToken() {
		return token;
	}


	public void setToken(TokenInformation token) {
		this.token = token;
	}

}
