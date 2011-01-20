package net.pgp2p.networkhandler;


public class StringToTCPPortNumber {

	/**
	 * Dumb methods that returns a TCP port number in the range from 49152Ð65535 (dynamic, private or ephemeral ports).
	 * 
	 * NUMBERS CAN COLLIDE.
	 * 
	 * @param String - any string
	 * @return Integer - a "distinct" TCP Port Number
	 */
	public static final int get(String string) {
		char[] localString = string.toCharArray();
		
		final int MIN=49152;
		final int MAX=65535;
		
		int finalNumber = 0;
		
		for (int i = 0; i < localString.length; i++) {
			finalNumber += (int) localString[i];
		}

		finalNumber = MIN + finalNumber;
		
		if  ( finalNumber > MAX)
			finalNumber = MAX - (finalNumber - MAX);
			
		return finalNumber; 
	}
}
