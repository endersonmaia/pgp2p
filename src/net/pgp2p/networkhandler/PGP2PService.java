package net.pgp2p.networkhandler;

// TODO - convert to Enum
public class PGP2PService {

	protected static final String NAME = "PGP2P_AUTH";
	
	protected static final int VERIFY_REQUEST	= 0;
	protected static final int VERIFY_REPLY 		= 1;
	protected static final int SIGN_REQUEST		= 2;
	protected static final int SIGN_REPLY		= 3;
	
	protected static String[] PARAMS = new String[4];
	
	static {
		PARAMS[0] = "verifyRequest";
		PARAMS[1] = "verifyReply";
		PARAMS[2] = "signRequest";
		PARAMS[3] = "signReply";
	}
	

public static void main(String[] args) {
	for (int i = 0; i < PARAMS.length; i++) {
		
	}
}

}
