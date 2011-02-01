package net.pgp2p.networkhandler;

// TODO - convert to Enum with constructor
public class PGP2PService {

	public static final int CONNECT_REQUEST	= 0;
	public static final int CONNECT_REPLY	= 1;
	public static final int VERIFY_REQUEST	= 2;
	public static final int VERIFY_REPLY 	= 3;
	public static final int SIGN_REQUEST		= 4;
	public static final int SIGN_REPLY		= 5;
	
	public static final int STATUS_OK = 0;
	public static final int STATUS_ERROR = 1;
	
	protected static final String NAMESPACE = "PGP2P_AUTH";

	protected static final String[] PARAMS = {
											"connectRequest",
											"connectReply", 
											"verifyRequest",
											"verifyReply",
											"signRequest",
											"signReply"
											};
}
