package net.pgp2p.networkhandler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
	public static final String FROM_USER_ID_FIELD = "FROM_USER_ID";
	public static final String KEY_ID_FIELD		= "KEY_ID";
	public static final String PUBLIC_KEY_FIELD	= "PUBLIC_KEY";
	public static final String AUTH_FIELD		= "AUTH";
	public static final String TYPE_FIELD 		= "TYPE";
	public static final String STATUS_FIELD		= "STATUS";
	public static final String TRACK_FIELD		= "TRACK";
	public static final String IS_FROM_CONNECT_FIELD	= "IS_FROM_CONNECT";

	private String sourceUserID;
	private String finalUserID;
	private String fromUserID;

	private long keyID;
	private String armoredPublicKey;
	private String auth;
	private int type;
	private int status;
	private Collection<String> track = new HashSet<String>();
	private boolean isFromConnect = false;
	
	public PGP2PMessage fromMessage(Message message) {
		
		String 	fromUserID	= message.getMessageElement(NAMESPACE, FROM_USER_ID_FIELD).toString();
		int		type		= Integer.valueOf(message.getMessageElement(NAMESPACE, TYPE_FIELD).toString());

		this.setFromUserID(fromUserID)
			.setType(type);
		
		long	keyID;
		
		if (message.getMessageElement(NAMESPACE, KEY_ID_FIELD) != null) {
			keyID = new BigInteger(message.getMessageElement(NAMESPACE, KEY_ID_FIELD).toString(), 16).longValue();
			this.setKeyID(keyID);
		}
			
		
		String	publicKey;
		
		if (message.getMessageElement(NAMESPACE, PUBLIC_KEY_FIELD) != null) {
			publicKey	= message.getMessageElement(NAMESPACE, PUBLIC_KEY_FIELD).toString();
			this.setArmoredPublicKey(publicKey);
		}
		
		String	auth;
		
		if (message.getMessageElement(NAMESPACE, AUTH_FIELD) != null) {
			auth = message.getMessageElement(NAMESPACE, AUTH_FIELD).toString();
			this.setAuth(auth);
		}

		
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
		
		List<String> tracks = new ArrayList<String>();
		
		if (message.getMessageElement(NAMESPACE, TRACK_FIELD) != null ) {
			String element = message.getMessageElement(NAMESPACE, TRACK_FIELD).toString();
			tracks.addAll(Arrays.asList( element.split( ", " ) ));
			this.track.clear();
			this.addTrack(tracks);
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
		MessageElement elemArmoredPublicKey = new StringMessageElement(PUBLIC_KEY_FIELD, publicKey, null);
		replaceMessageElement(NAMESPACE, elemArmoredPublicKey);
		return this;
	}
	
	public String getArmoredPublicKey() {
		return this.armoredPublicKey;
	}
	
	public PGP2PMessage setAuth(String auth) {
		this.auth = auth;
		MessageElement elemAuth = new StringMessageElement(AUTH_FIELD, auth, null);
		replaceMessageElement(NAMESPACE, elemAuth);
		return this;
	}

	public String getAuth() {
		return this.auth;
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
	
	public PGP2PMessage setFromUserID(String fromUserID) {
		this.fromUserID = fromUserID;
		MessageElement elemFromUserID = new StringMessageElement(FROM_USER_ID_FIELD, fromUserID, null);
		replaceMessageElement(NAMESPACE, elemFromUserID);
		return this;
	}
	
	public String getFromUserID() {
		return this.fromUserID;
	}

	public PGP2PMessage addTrack(String track) {
		this.track.add(track);
		
		MessageElement elemTrack = new StringMessageElement(TRACK_FIELD, this.track.toString().replace("[", "").replace("]", ""), null);
		replaceMessageElement(NAMESPACE, elemTrack);
		return this;
	}
	
	public PGP2PMessage addTrack(Collection<String> tracks) {
		this.track.addAll(tracks);
		
		MessageElement elemTrack = new StringMessageElement(TRACK_FIELD, this.track.toString().replace("[", "").replace("]", ""), null);
		replaceMessageElement(NAMESPACE, elemTrack);
		
		return this;
	}

	public Collection<String> getTrack() {
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
