package agents;

import java.util.Comparator;

import message.TokenInformation;
/**
   A comparator that sorts entities by distance to a reference point.
*/
public class ValueSorter implements Comparator<TokenInformation> {

	@Override
	public int compare(TokenInformation t1, TokenInformation t2) {
		// TODO Auto-generated method stub
		return (t1.getCapability() > t2.getCapability() ? -1 : (t1.getCapability() == t2.getCapability() ? 0 : 1));
	}
    

   
}