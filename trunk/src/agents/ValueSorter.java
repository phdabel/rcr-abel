package agents;

import java.util.Comparator;

import worldmodel.jobs.Token;

/**
   A comparator that sorts entities by distance to a reference point.
*/
public class ValueSorter implements Comparator<Token> {

	@Override
	public int compare(Token t1, Token t2) {
		// TODO Auto-generated method stub
		return (t1.getHolderCapability() > t2.getHolderCapability() ? -1 : (t1.getHolderCapability() == t2.getHolderCapability() ? 0 : 1));
	}
    

   
}