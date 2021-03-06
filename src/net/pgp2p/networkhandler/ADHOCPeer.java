/*
Copyright (c) 2011 Enderson Maia

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package net.pgp2p.networkhandler;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.Messenger;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.pgp2p.cryptoservice.PGPManager;
import net.pgp2p.signservice.PGPSigner;
import net.pgp2p.verifyservice.PGPVerify;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;

/**
* This class is used to instantiate a new JXTA Peer with mode ADHOC on the 
* network.
*
* @author Enderson Maia <endersonmaia@gmail.com>
*/

public class ADHOCPeer implements EndpointListener  {

	/**
	 * Logger
	 */
	private static final Logger logger = Logger.getLogger(ADHOCPeer.class
			.getName());
	
	/**
	 * JXTA Logger
	 * TODO  use logging.properties file
	 */
	private static final Logger jxtaLogger = Logger.getLogger("net.jxta");
	
	static {
		jxtaLogger.setLevel(Level.SEVERE);
	}

	private String username;
	private String tcpPort;
	private File configFile;

	public PeerID peerID;
	private PeerGroup netPeerGroup;
	private NetworkManager netManager;
	private EndpointService endpointService;
	private PGPManager pgpManager;
	private PGPVerify verifyService;
	private PGPSigner signService;

	public ADHOCPeer(String username) {
		this.username = username;
		this.tcpPort = StringToTCPPortNumber.get(username);
		this.configFile = new File("." + System.getProperty("file.separator")
				+ "jxta" + System.getProperty("file.separator") + username);
		this.peerID = getPeerIDforUserame(username);
		
		try {
			this.pgpManager = new PGPManager("./sandbox/gnupg-test/" + username);
			this.verifyService = new PGPVerify(pgpManager);
			this.signService = new PGPSigner(pgpManager);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public ADHOCPeer(String username, boolean exclude) {
		this(username);

		// remove all configuration by default
		if (exclude)
			NetworkManager.RecursiveDelete(configFile);

	}

	/*FIXME
	 * all the communication is based on the PeerID being
	 * generated form the username, this is not the desired behave
	 * for the real application 
	 */
	private PeerID getPeerIDforUserame(String username) {
		return IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, username
				.getBytes());
	}

	public ADHOCPeer start() {
		try {
			netManager = new NetworkManager(NetworkManager.ConfigMode.ADHOC,
					username, configFile.toURI());

			NetworkConfigurator netConfig = netManager.getConfigurator();

			// Setting Configuration
			netConfig.setUseMulticast(true);
			
			// ADHOC Mode doesn't need TCP configuration
			//netConfig.setTcpPort(Integer.parseInt(tcpPort));
			//netConfig.setTcpEnabled(true);
			//netConfig.setTcpIncoming(true);
			//netConfig.setTcpOutgoing(true);
			netConfig.setPeerID(peerID);

			netConfig.save();

			netPeerGroup = netManager.startNetwork();
			endpointService = netPeerGroup.getEndpointService();

			// Adding supported services
			registerListeners();

			netPeerGroup.getRendezVousService().setAutoStart(false);

			// Debug
			logger.log(Level.INFO, "Peer name: " + username);
			logger.log(Level.INFO, "PeerID :" + netPeerGroup.getPeerID());
			logger.log(Level.INFO, "Network started with ID :"
					+ netPeerGroup.getPeerGroupID());
			logger.log(Level.INFO, "Listening on the port "
					+ tcpPort.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return this;
	}

	/**
	 * Adds all the incoming message listeners defined in PGP2PService.
	 * 
	 *  @see PGP2PService
	 */
	private void registerListeners() {
		for (int i = 0; i < PGP2PService.PARAMS.length; i++) {
			endpointService.addIncomingMessageListener(this,
					PGP2PService.NAMESPACE, PGP2PService.PARAMS[i]);
		}
	}

	public boolean canReach(String username) {
		return endpointService.isReachable(getPeerIDforUserame(username), true);
	}

	/**
	 * Sends a message to the Peer identified by peerID parameter.
	 * 
	 * The message must contain PGP2PMessageType.NAMESPACE and use one of the
	 * PGP2PMessageType.PARAMS[] services.
	 * 
	 * @param peerID
	 * @param msg
	 * @throws IOException
	 */
	public void sendMessage(PeerID peerID, Message msg) {
		
		logger.log(Level.FINE, "--++ SENDING MESSAGE ++--\n" + dumpMessage(msg));

		// gets the message
		PGP2PMessage message = new PGP2PMessage().fromMessage(msg);
		
		// gets the EndpointAddress for the service identified by the
		// messageType
		EndpointAddress addr = new EndpointAddress(peerID,
				PGP2PService.NAMESPACE, PGP2PService.PARAMS[message.getType()]);
		
		Messenger messenger = endpointService.getMessenger(addr);
		
		//FIXME - check for Messenger status instead of this hardcoded timeout
		long t0 = System.currentTimeMillis();  
		
		while ( (messenger == null) && (System.currentTimeMillis() - t0 < PGP2PService.REQUEST_TIMEOUT) )  {
			messenger = endpointService.getMessenger(addr);
		}

		// verify if the endpoint is reachable ...
		if ( messenger != null ) {	

			logger.log(Level.INFO, "Can reach.\n" + "peerID: "
					+ peerID.toString());

			// if messageType is valid ...
			if (message.getType() <= PGP2PService.PARAMS.length) {

				// sends the message
				try {
					if (messenger.sendMessage(msg)) {
						logger.log(Level.INFO, "Message sent.");
					} else {
						logger.log(Level.INFO, "Can't send message.");
					}
				} catch (IOException e) {
					logger.log(Level.SEVERE, "Can't send message. Caught IOException.");
					e.printStackTrace();
				}

			} else {
				logger.log(Level.INFO, "Invalid message type.");
			}
		} else {
			logger.log(Level.INFO, "Can't reach.\n" + "peerID: "
					+ peerID.toString());
		}
	}

	/**
	 * Receives the username string as a parameter that then is
	 * converted to a peerID and repassed within the message 
	 * to sendMessage(PeerID, Message).
	 *   
	 * @param username
	 * @param msg
	 */
	public void sendMessage(String username, Message msg) {
		// gets the message
		PGP2PMessage message = new PGP2PMessage().fromMessage(msg);
		String messageType = PGP2PService.PARAMS[message.getType()];
		
		String status = " ";
		if (message.getStatus() != PGP2PService.STATUS_OK) {
			status += "with STATUS_ERROR Code: " + message.getStatus();			
		} else {
			status += "with STATUS_OK";
		}
		
		logger.log(Level.INFO, "Sending a " + 
				 messageType.toUpperCase() +
				" to " + username + status);
		
		sendMessage(getPeerIDforUserame(username), msg);
	}

	@Override
	/**
	 * All messages arrives here and are redirected to the respective 
	 * processor according to it's TYPE.
	 * 
	 */
	public void processIncomingMessage(Message message,
			EndpointAddress srcAddr, EndpointAddress dstAddr) {

		logger.log(Level.FINE, "--++ ARRIVING MESSAGE ++--\n" + dumpMessage(message));

		PGP2PMessage msg = new PGP2PMessage().fromMessage(message);

		if (msg.getType() <= PGP2PService.PARAMS.length) {
			try {
				switch (msg.getType()) {
				case PGP2PService.CONNECT_REQUEST:
					processConnectRequest(msg);
					break;
				case PGP2PService.CONNECT_REPLY:
					processConnectReply(msg);
					break;
				case PGP2PService.VERIFY_REQUEST:
					processVerifyRequest(msg);
					break;
				case PGP2PService.VERIFY_REPLY:
					processVerifyReply(msg);
					break;
				case PGP2PService.SIGN_REQUEST:
					processSignRequest(msg);
					break;
				case PGP2PService.SIGN_REPLY:
					processSignReply(msg);
					break;

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	/**
	 * Receives a connection request from another Peer and must check if 
	 * this Peer is in the local trust.
	 * 
	 * In case the requester is in local trust list, a CONNECT_REPLY is sent 
	 * with STATUS_OK, otherwise a CONNECT_REPLY with STATUS_ERROR is sent. 
	 * 
	 * @param message
	 * @param srcAddr
	 * @param dstAddr
	 * @throws IOException 
	 */
	private void processConnectRequest(PGP2PMessage message) 
	throws IOException {

		logger.log(Level.INFO, "Processing CONNECT_REQUEST from " + message.getFromUserID());
		
		try {
			PGP2PMessage replyMessage = new PGP2PMessage()
				.setFromUserID(username)
				.setFinalUserID(message.getFinalUserID())						
				.setSourceUserID(message.getSourceUserID())
				.setKeyID(message.getKeyID())
				.setArmoredPublicKey(message.getArmoredPublicKey())
				.setType(PGP2PService.CONNECT_REPLY);
			
			if ( message.getFinalUserID().equals(username) ) {
				if (verifyService.isTrusted(message.getArmoredPublicKey()) ) {
					replyMessage.setStatus(PGP2PService.STATUS_OK);
				} else {
					replyMessage.setStatus(PGP2PService.STATUS_ERROR);
				}				
			} else {
				replyMessage.setStatus(PGP2PService.STATUS_ERROR_NOT_CONNECTING_TO_FINAL_USER);
			}
			
			sendMessage(message.getFromUserID(), replyMessage);
		} catch (PGPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Process connect reply. In case STATUS_OK, do nothing, if STATUS_ERROR, 
	 * sends back a VERIFY_REQUEST so the peer can search with other peers 
	 * in the web of trust.
	 * 
	 * @param message
	 * @param srcAddr
	 * @param dstAddr
	 * @throws IOException 
	 */
	private void processConnectReply(
			PGP2PMessage message) 
	throws IOException {

		logger.log(Level.INFO, "Processing CONNECT_REPLY from " + message.getFromUserID());
		
		switch (message.getStatus()) {
			case PGP2PService.STATUS_OK:
				logger.log(Level.INFO, "Recieved CONNECT_REPLY with STATUS_OK from " + message.getFromUserID());
				break;
				
			case PGP2PService.STATUS_ERROR_NOT_CONNECTING_TO_FINAL_USER:
				logger.log(Level.INFO, "Recieved CONNECT_REPLY with STATUS_ERROR from " 
						+ message.getFromUserID()
						+ " Code: " + message.getStatus());
				break;

			case PGP2PService.STATUS_ERROR:
				logger.log(Level.INFO, "Recieved CONNECT_REPLY with STATUS_ERROR from " + message.getFromUserID());
			
				PGP2PMessage verifyRequest = new PGP2PMessage()
					.setFromUserID(username)
					.setFinalUserID(message.getFinalUserID())						
					.setSourceUserID(message.getSourceUserID())
					//FIXME - don't need to get from message, could get from pgpManager
					.setKeyID(message.getKeyID())
					.setArmoredPublicKey(message.getArmoredPublicKey())
					.setType(PGP2PService.VERIFY_REQUEST)
					.setFromConnect(true)
					.addTrack(message.getFromUserID());
				
				sendMessage(message.getFromUserID(), verifyRequest);
				break;
		}		
	}

	/**
	 * Process VERIFY_REQUEST messages to check in local keyRing for trust 
	 * information about the publicKEy in the message. In case the given key 
	 * is known in local keyRing, a VERIFY_REPLY is sent back with STATUS_OK.
	 * 
	 * If there's no trust information in local keyRing, a new VERIFY_REQUEST
	 * message is sent to all trusted users searching for any trust information.
	 *  
	 * @param message
	 * @throws IOException
	 */
	private void processVerifyRequest(PGP2PMessage message) throws IOException {
		
		logger.log(Level.INFO, "Processing VERIFY_REQUEST from " + message.getFromUserID());
				
		try {
			/*
			 *  Se n�o veio de um connectReply com STATUS_ERROR
			 *  ent�o faz a verifica�ao local.
			 */
			
			logger.log(Level.INFO, "Verifying in local " 
					+ username + "'s keyring for trust information about " 
					+ message.getSourceUserID());

			if ( ! message.isFromConnect() ) {
				
				//FIXME - it this necessary ? id I can't accept, why reply about it, just ignore
				if ( ! pgpManager.getTrustedUserIDs().contains(message.getFromUserID()) ) {
					
					logger.log(Level.INFO, "Can't accept a VERIFY_REQUEST from a untrusted user.");
					
					PGP2PMessage verifyReply = new PGP2PMessage()
						.setFromUserID(username)
						//.setFinalUserID(message.getFinalUserID())
						//.setSourceUserID(message.getSourceUserID())
						//.setKeyID(message.getKeyID())
						//.setArmoredPublicKey(message.getArmoredPublicKey())
						.setType(PGP2PService.VERIFY_REPLY)
						.setStatus(PGP2PService.STATUS_ERROR_CANT_ACCEPT_FROM_UTRUSTED_USER);

					sendMessage(message.getFromUserID(), verifyReply);
					
					return;
				} 
				
				if (verifyService.isTrusted(message.getArmoredPublicKey())) {
				
					logger.log(Level.INFO, username + " trusts " + message.getSourceUserID() + "'s key " + message.getKeyID());
	
					System.out.println(" PUBLIC KEY TO ENTER IN AUTH : \n " + pgpManager.getArmoredPublicKey());
					
					PGP2PMessage verifyReply = new PGP2PMessage()
						.setFromUserID(username)
						.setFinalUserID(message.getFinalUserID())
						.setSourceUserID(message.getSourceUserID())
						.setKeyID(message.getKeyID())
						.setArmoredPublicKey(message.getArmoredPublicKey())
						.setType(PGP2PService.VERIFY_REPLY)
						.setStatus(PGP2PService.STATUS_OK)
						.setFromConnect(false)
						.addTrack(message.getTrack())
						.addTrack(username)
						// FIXME - setAut should receive a signature to be verified, using pubKey for now
						.setAuth(pgpManager.getArmoredPublicKey());
	
					sendMessage(message.getSourceUserID(), verifyReply);

					return;
				}

			} else {
								
				// TODO - change the logic to avoid code duplication
				if ( verifyService.isTrusted(message.getArmoredPublicKey())) {

					logger.log(Level.INFO, username + " trusts " + message.getSourceUserID() + "'s key " + message.getKeyID());
					
					System.out.println(" PUBLIC KEY TO ENTER IN AUTH : \n " + pgpManager.getArmoredPublicKey());

					PGP2PMessage verifyReply = new PGP2PMessage()
						.setFromUserID(username)
						.setFinalUserID(message.getFinalUserID())
						.setSourceUserID(message.getSourceUserID())
						.setKeyID(message.getKeyID())
						.setArmoredPublicKey(message.getArmoredPublicKey())
						.setType(PGP2PService.VERIFY_REPLY)
						.setStatus(PGP2PService.STATUS_OK)
						.setFromConnect(false)
						.addTrack(message.getTrack())
						.addTrack(username)
						.setAuth(message.getArmoredPublicKey());

					//logger.log(Level.INFO, "Sending VERIFY_REPLY with STATUS_OK to " + message.getSourceUserID());

					sendMessage(message.getSourceUserID(), verifyReply);

					return;
				}			

			}
			
			// Don't go to deep in verifyRequest 
			if (message.getTrack().size() > PGP2PService.LIMIT_VERIFY_DEPTH) {
				logger.log(Level.INFO, "Search depth limit reached.");
				return;
			}
			
			Iterator<PGPPublicKey> trustedPeers;
			trustedPeers = pgpManager.getTrustedPublicKeys().iterator();
			while ( trustedPeers.hasNext() ) {
				PGPPublicKey trustedPeer = trustedPeers.next();
				String trustedUserID = PGPManager.getUserID(trustedPeer);
				
				// Avoid consulting a user that has already been consulted
				if (message.getTrack().contains(trustedUserID)){
					logger.log(Level.INFO, "The user "+ trustedUserID + " has already been consulted.");
					
					continue;
				}
				
				logger.log(Level.INFO, "Repassing a VERIFY_REQUEST to " + trustedUserID);

				PGP2PMessage verifyRequest = new PGP2PMessage()
					.setFromUserID(username)
					.setFinalUserID(message.getFinalUserID())
					.setSourceUserID(message.getSourceUserID())
					.setKeyID(message.getKeyID())
					.setArmoredPublicKey(message.getArmoredPublicKey())
					.setType(PGP2PService.VERIFY_REQUEST)
					.setFromConnect(false)
					.addTrack(message.getTrack())
					.addTrack(username);
				
				sendMessage(trustedUserID, verifyRequest);
			}
				
		} catch (PGPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}

	}
	
	/**
	 * Process VERIFY_REPLY messages. If the status is OK, should ask for 
	 * signing the key.
	 * TODO - send a SIGN_REQUEST
	 * 
	 * @param message
	 */
	private void processVerifyReply(PGP2PMessage message) {
		
		// TODO - validates reply and begins searching for other 
		// peers or giving a OK
		logger.log(Level.INFO, "Processing VERIFY_REPLY from " + message.getFromUserID());
		
		if ( message.getStatus() == PGP2PService.STATUS_OK) {

			// sendo o usu�rio final o receptor deste tipo de mensagem sendo OK
			// a chave deve ser importada e assinada
			
			if ( username.equals(message.getSourceUserID()) ) {
				logger.log(Level.INFO, "Final user received a VERIFY_REPLY with STATUS_OK from " 
						+ message.getFromUserID() + " through " 
						+ message.getTrack().toString() );
				
				PGP2PMessage signRequest = new PGP2PMessage()
						.setFromUserID(username)
						.setFinalUserID(message.getFinalUserID())
						.setSourceUserID(message.getSourceUserID())
						.setKeyID(message.getKeyID())
						.setArmoredPublicKey(message.getArmoredPublicKey())
						.setType(PGP2PService.SIGN_REQUEST)
						.setStatus(PGP2PService.STATUS_OK)
						.setFromConnect(false)
						.addTrack(message.getTrack())
						// FIXME - setAut should receive a signature to be verified, using pubKey for tests  
						.setAuth(message.getAuth());
				
				sendMessage(message.getFinalUserID(), signRequest);
				
			} else {
				/*
				 * FIXME - decide what to do, VERIFY_REPLY STATUS_OK should only come from
				 * a VERIFY_REQUEST STATUS_OK to the SOURCE_USER_ID.
				 */
				logger.log(Level.INFO, "SHOULD NEVE GET HERE Received a VERIFY_REPLY with STATUS_OK");
			}
			
			// TODO - if the verify reply was OK, import the publicKey in local keyRing
			// via signRequest
		} else {
			logger.log(Level.INFO, "Recieved VERIFY_REPLY with STATUS_ERRROR Code: " + message.getStatus());
		}
	}


	/**
	 * Process SIGN_REQUEST messages. Is case the AUTH field is 
	 * trusted, proceed with importing the pubKey and sending
	 * it's pubKey with signReply to get imported in the requester.
	 *  
	 * @param message
	 */
	private void processSignRequest(PGP2PMessage message) {

		// TODO - verifies trust for the requestor and then sign its key with
		// processSignReply
		logger.log(Level.INFO, "Processing SIGN_REQUEST from " + message.getFromUserID());
		
		PGPPublicKey authPubKey, pubKeyToSign;
		try {
			authPubKey = PGPManager.getPublicKey(message.getAuth());
			pubKeyToSign = PGPManager.getPublicKey(message.getArmoredPublicKey());
			
			String authUserID = PGPManager.getUserID(authPubKey);
			
			if ( verifyService.isTrusted(authPubKey) || message.getTrack().contains(authUserID) ) {
				logger.log(Level.INFO, "Authentication through "+ message.getTrack() +" succeeded");

				//sign and import the sourceUserID pubKey
				signService.signPublicKey(pubKeyToSign);
				
				//send SIGN_REPLY with Final's pubKey to be imported at Source
				PGP2PMessage signReply = new PGP2PMessage()
					.setFromUserID(username)
					.setFinalUserID(message.getFinalUserID())
					.setSourceUserID(message.getSourceUserID())
					.setKeyID(pgpManager.getPublicKey().getKeyID())
					.setArmoredPublicKey(pgpManager.getArmoredPublicKey())
					.setType(PGP2PService.SIGN_REPLY)
					.setStatus(PGP2PService.STATUS_OK)
					.setFromConnect(false)
					.addTrack(message.getTrack())
					// FIXME - setAut should receive a signature to be verified, using pubKey for tests  
					.setAuth(message.getAuth());
				
				sendMessage(message.getFromUserID(), signReply);
			} else {
				logger.log(Level.INFO, "Authentication from " + authUserID + " failed");					
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PGPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	private void processSignReply(PGP2PMessage message) {

		logger.log(Level.INFO, "Processing SIGN_REPLY from " + message.getFromUserID());
		PGPPublicKey authPubKey, pubKeyToSign;
		try {
			authPubKey = PGPManager.getPublicKey(message.getAuth());
			pubKeyToSign = PGPManager.getPublicKey(message.getArmoredPublicKey());
			
			String authUserID = PGPManager.getUserID(authPubKey);
			
			if ( verifyService.isTrusted(authPubKey) || message.getTrack().contains(authUserID) ) {
				logger.log(Level.INFO, "Authentication through "+ message.getTrack() +" succeeded");

				//sign and import the sourceUserID pubKey
				signService.signPublicKey(pubKeyToSign);
				
				//send SIGN_REPLY with Final's pubKey to be imported at Source
				PGP2PMessage signReply = new PGP2PMessage()
					.setFromUserID(username)
					.setFinalUserID(message.getFinalUserID())
					.setSourceUserID(message.getSourceUserID())
					.setKeyID(pgpManager.getPublicKey().getKeyID())
					.setArmoredPublicKey(pgpManager.getArmoredPublicKey())
					.setType(PGP2PService.SIGN_REPLY)
					.setStatus(PGP2PService.STATUS_OK)
					.setFromConnect(false)
					// FIXME - setAut should receive a signature to be verified, using pubKey for tests  
					.setAuth(message.getAuth());
				
				sendMessage(message.getFromUserID(), signReply);
			} else {
				logger.log(Level.INFO, "Authentication from " + authUserID + " failed");					
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PGPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchProviderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Utility method to dump the messages for debugging purpose.
	 * 
	 * @param msg
	 * @return String - the content of the message and it's fields' names.
	 */
	private String dumpMessage(Message msg) {
		String dump = "\n";
		Iterator<String> ns = msg.getMessageNamespaces();
		while (ns.hasNext()) {
			String nameSpace = ns.next();
			dump += ">>NameSpace: " + nameSpace + "\n";

			Iterator<MessageElement> e = msg
					.getMessageElementsOfNamespace(nameSpace);
			while (e.hasNext()) {
				MessageElement elem = e.next();
				dump += elem.getElementName() + ": " + elem.toString() + "\n";
			}
			dump += "<<\n";
		}
		return dump;
	}

}
