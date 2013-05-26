package message;


public class ColeagueInformation extends MyMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public ColeagueInformation(int me, int position)
	{
		this.setId(me);
		this.setPosition(position);
	}

}
