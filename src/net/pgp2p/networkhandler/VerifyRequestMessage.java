package net.pgp2p.networkhandler;

import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;

public class VerifyRequestMessage extends Message {
	
	/**
	 * Generated serial version UID
	 */
	private static final long serialVersionUID = -3521104394888893998L;

	public VerifyRequestMessage() {
		super();
	}
	
	public VerifyRequestMessage setUserID(String userID) {
		MessageElement elemUserID = new StringMessageElement("USER_ID", userID, null);
		addMessageElement(elemUserID);
		return this;
	}
	
	public VerifyRequestMessage setKeyID(Long keyID) {
		MessageElement elemKeyID = new StringMessageElement("KEY_ID", Long.toHexString(keyID), null);
		addMessageElement(elemKeyID);
		return this;
	}
	
	public VerifyRequestMessage setArmoredPublicKey(String publicKey) {
		// FIXME - avaliar o uso de TextDocumentElement
		MessageElement elemArmoredPublicKey = new StringMessageElement("PUBLIC_KEY", publicKey, null);
		addMessageElement(elemArmoredPublicKey);
		return this;

	}
}
