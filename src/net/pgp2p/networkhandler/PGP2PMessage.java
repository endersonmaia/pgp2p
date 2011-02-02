package net.pgp2p.networkhandler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;

public class PGP2PMessage extends Message {

	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -3521104394888893998L;
	
	private static final String NAMESPACE 		= PGP2PService.NAMESPACE;
	public static final String SOURCE_USER_ID_FIELD = "SOURCE_USER_ID";
	public static final String FINAL_USER_ID_FIELD	= "FINAL_USER_ID";
	public static final String USER_ID_FIELD		= "USER_ID";
	public static final String KEY_ID_FIELD		= "KEY_ID";
	public static final String PUBLIC_KEY_FIELD	= "PUBLIC_KEY";
	public static final String TYPE_FIELD 		= "TYPE";
	public static final String STATUS_FIELD		= "STATUS";
	public static final String TRACK_FIELD		= "TRACK";
	public static final String IS_FROM_CONNECT_FIELD	= "IS_FROM_CONNECT";

	private String sourceUserID;
	private String finalUserID;
	private String userID;
	private long keyID;
	private String armoredPublicKey;
	private int type;
	private int status;
	private List<String> track = new ArrayList<String>();
	private boolean isFromConnect = false;
	
	public PGP2PMessage fromMessage(Message message) {
		
		String	userID		= message.getMessageElement(NAMESPACE,USER_ID_FIELD).toString();
		long	keyID 		= new BigInteger(message.getMessageElement(NAMESPACE, KEY_ID_FIELD).toString(), 16).longValue();
		String	publicKey	= message.getMessageElement(NAMESPACE, PUBLIC_KEY_FIELD).toString();
		int		type		= Integer.valueOf(message.getMessageElement(NAMESPACE, TYPE_FIELD).toString());

		this.setUserID(userID)
			.setKeyID(keyID)
			.setArmoredPublicKey(publicKey)
			.setType(type);

		int 	status;
		
		if (message.getMessageElement(NAMESPACE, STATUS_FIELD) != null ) {
			status = Integer.valueOf(message.getMessageElement(NAMESPACE, STATUS_FIELD).toString());
			this.setStatus(status);
		}
		
		String sourceUserID;
		
		if (message.getMessageElement(NAMESPACE, SOURCE_USER_ID_FIELD) != null ) {
			sourceUserID = message.getMessageElement(NAMESPACE, SOURCE_USER_ID_FIELD).toString();
			this.setSourceUserID(sourceUserID);
		}

		String finalUserID;
		
		if (message.getMessageElement(NAMESPACE, FINAL_USER_ID_FIELD) != null ) {
			finalUserID = message.getMessageElement(NAMESPACE, FINAL_USER_ID_FIELD).toString();
			this.setFinalUserID(finalUserID);
		}
		
		List<String> track = new ArrayList<String>();
		
		if (message.getMessageElement(NAMESPACE, TRACK_FIELD) != null ) {
			String element = message.getMessageElement(NAMESPACE, TRACK_FIELD).toString();
			
			int elementSize = element.length() - 1; 
			element = element.substring(1, elementSize);
			
			track.clear();
			track.addAll(Arrays.asList( element.split( ", " ) ));
		}
		
		boolean isFromConnect = false;
		
		if ( message.getMessageElement(NAMESPACE, IS_FROM_CONNECT_FIELD) != null ) {
			isFromConnect = Boolean.valueOf(message.getMessageElement(NAMESPACE, IS_FROM_CONNECT_FIELD).toString());
			
			this.setFromConnect(isFromConnect);
		}
		return this;
	}

	public PGP2PMessage() {
		super();
	}
	
	public PGP2PMessage setUserID(String userID) {
		this.userID = userID;
		MessageElement elemUserID = new StringMessageElement(USER_ID_FIELD, userID, null);
		replaceMessageElement(NAMESPACE, elemUserID);
		return this;
	}
	
	public String getUserID() {
		return this.userID;
	}
	
	public PGP2PMessage setKeyID(Long keyID) {
		this.keyID = keyID;
		MessageElement elemKeyID = new StringMessageElement(KEY_ID_FIELD, Long.toHexString(keyID), null);
		replaceMessageElement(NAMESPACE, elemKeyID);
		return this;
	}
	
	public long getKeyID() {
		return this.keyID;
	}
	
	public PGP2PMessage setArmoredPublicKey(String publicKey) {
		this.armoredPublicKey = publicKey;
		// FIXME - avaliar o uso de TextDocumentElement
		MessageElement elemArmoredPublicKey = new StringMessageElement(PUBLIC_KEY_FIELD, publicKey, null);
		replaceMessageElement(NAMESPACE, elemArmoredPublicKey);
		return this;
	}
	
	public String getArmoredPublicKey() {
		return this.armoredPublicKey;
	}
	
	public PGP2PMessage setType(int messageType) {
		this.type = messageType;
		
		MessageElement elemMessageType = new StringMessageElement(TYPE_FIELD, String.valueOf(messageType), null);
		replaceMessageElement(NAMESPACE, elemMessageType);
		return this;
	}
	
	public int getType() {
		return this.type;
	}
	
	public PGP2PMessage setStatus(int status) {
		this.status = status;
		MessageElement elemStatus = new StringMessageElement(STATUS_FIELD, String.valueOf(status), null);
		replaceMessageElement(NAMESPACE, elemStatus);
		return this;
	}
	
	public int getStatus() {
		return this.status;
	}

	public PGP2PMessage setSourceUserID(String sourceUserID) {
		this.sourceUserID = sourceUserID;
		MessageElement elemSourceUserID = new StringMessageElement(SOURCE_USER_ID_FIELD, sourceUserID, null);
		replaceMessageElement(NAMESPACE, elemSourceUserID);
		return this;
	}

	public String getSourceUserID() {
		return this.sourceUserID;
	}

	public PGP2PMessage setFinalUserID(String finalUserID) {
		this.finalUserID = finalUserID;
		MessageElement elemFinalUserID = new StringMessageElement(FINAL_USER_ID_FIELD, finalUserID, null);
		replaceMessageElement(NAMESPACE, elemFinalUserID);
		return this;
	}

	public String getFinalUserID() {
		return this.finalUserID;
	}

	public PGP2PMessage addTrack(String track) {
		this.track.add(track);
		
		MessageElement elemTrack = new StringMessageElement(TRACK_FIELD, track.toString(), null);
		replaceMessageElement(NAMESPACE, elemTrack);
		return this;
	}
	
	public PGP2PMessage addTrack(List<String> tracks) {
		this.track.addAll(tracks);
		
		MessageElement elemTrack = new StringMessageElement(TRACK_FIELD, track.toString(), null);
		replaceMessageElement(NAMESPACE, elemTrack);
		
		return this;
	}

	public List<String> getTrack() {
		return this.track;
	}

	public PGP2PMessage setFromConnect(boolean isFromConnect) {
		this.isFromConnect = isFromConnect;
		MessageElement elemIsFromConnect = new StringMessageElement(IS_FROM_CONNECT_FIELD, String.valueOf(isFromConnect), null);
		replaceMessageElement(NAMESPACE, elemIsFromConnect);
		return this;
	}

	public boolean isFromConnect() {
		return this.isFromConnect;
	}
}
