package net.pgp2p.networkhandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.pgp2p.cryptoservice.PGPManager;
import net.pgp2p.signservice.PGPSigner;
import net.pgp2p.verifyservice.PGPVerify;

public class ADHOCPeer implements EndpointListener {

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

	private PGP2PMessage getMessageHeader() throws PGPException, IOException {
		PGP2PMessage message;
		message = new PGP2PMessage();
		
		message.setUserID(pgpManager.getUserID());
		message.setKeyID(pgpManager.getPublicKey().getKeyID());
		message.setArmoredPublicKey(pgpManager.getArmoredPublikKey());
		
		return message;
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

		// gets the message
		PGP2PMessage message = new PGP2PMessage().fromMessage(msg);
		
		//int messageType = Integer.parseInt(msg.getMessageElement(
		//		PGP2PService.NAMESPACE, PGP2PMessage.TYPE_FIELD).toString());

		// gets the EndpointAddress for the service identified by the
		// messageType
		EndpointAddress addr = new EndpointAddress(peerID,
				PGP2PService.NAMESPACE, PGP2PService.PARAMS[message.getType()]);

		
		Messenger messenger = endpointService.getMessenger(addr);
		
		//FIXME - check for Messenger status instead of this hardcoded timeout
		long t0 = System.currentTimeMillis();  
		long timeOut= 5000; // ms
		
		while ( (messenger == null) || (System.currentTimeMillis() - t0 < timeOut) )  {
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

	public void sendMessage(String username, Message msg) {
		sendMessage(getPeerIDforUserame(username), msg);
	}

	@Override
	/**
	 * {@inheritDoc}
	 */
	public void processIncomingMessage(Message message,
			EndpointAddress srcAddr, EndpointAddress dstAddr) {

		logger.log(Level.INFO, dumpMessage(message));

		PGP2PMessage msg = new PGP2PMessage().fromMessage(message);

		if (msg.getType() <= PGP2PService.PARAMS.length) {
			try {
				switch (msg.getType()) {
				case PGP2PService.CONNECT_REQUEST:
					logger
							.log(Level.INFO,
									"Recieving a connect request, sending the message to proccessConnectRequest.");
					processConnectRequest(msg);
					break;
				case PGP2PService.CONNECT_REPLY:
					logger
							.log(Level.INFO,
									"Recieving a connect reply, sending the message do processConnectReply.");
					processConnectReply(msg);
					break;
				case PGP2PService.VERIFY_REQUEST:
					logger
							.log(Level.INFO,
									"Recieving a verify request, sending the message to proccessVerifyRequest.");
					processVerifyRequest(msg);
					break;
				case PGP2PService.VERIFY_REPLY:
					logger
							.log(Level.INFO,
									"Recieving a verify reply, sending the message do processVerifyReply.");
					processVerifyReply(msg);
					break;
				case PGP2PService.SIGN_REQUEST:
					logger
							.log(Level.INFO,
									"Recieving a sign request, sending the message to proccessSignRequest.");
					processSignRequest(msg);
					break;
				case PGP2PService.SIGN_REPLY:
					logger
							.log(Level.INFO,
									"Recieving a sign reply, sending the message do processSignReply.");
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

		logger.log(Level.INFO, "Processing CONNECT_REQUEST");
		
		try {
			PGP2PMessage replyMessage = getMessageHeader();
				
			replyMessage.setType(PGP2PService.CONNECT_REPLY);
			
			if (verifyService.isTrusted(message.getArmoredPublicKey()) ) {
				
				logger.log(Level.INFO, "Sending CONNECT_REPLY with STATUS_OK.");
				replyMessage.setStatus(PGP2PService.STATUS_OK);
				sendMessage(message.getUserID(), replyMessage);

			} else {

				logger.log(Level.INFO, "Sending CONNECT_REPLY with STATUS_ERROR.");
				replyMessage.setStatus(PGP2PService.STATUS_ERROR);
				sendMessage(message.getUserID(), replyMessage);

			}
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

		logger.log(Level.INFO, "Processing CONNECT_REPLY");
		
		switch (message.getStatus()) {
			case PGP2PService.STATUS_OK:
				logger.log(Level.INFO, "Recieved CONNECT_REPLY with STATUS_OK.");
				break;
				
			case PGP2PService.STATUS_ERROR:
				logger.log(Level.INFO, "Recieved CONNECT_REPLY with STATUS_ERROR.");
			
				try {
					PGP2PMessage verifyRequest = getMessageHeader();
					
					logger.log(Level.INFO, "Sending a VERIFY_REQUEST.");
					
					verifyRequest
						.setType(PGP2PService.VERIFY_REQUEST)
						.setSourceUserID(username)
						.setFinalUserID(message.getUserID())
						.addTrack(message.getUserID())
						.setFromConnect(true);
					
					sendMessage(message.getUserID(), verifyRequest);
				} catch (PGPException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
		
		logger.log(Level.INFO, "Processing VERIFY_REQUEST");

		try {
			/*
			 *  Se n‹o veio de um connectReply com STATUS_ERROR
			 *  ent‹o faz a verificaao local.
			 */
			if ( ! message.isFromConnect() && verifyService.isTrusted(message.getArmoredPublicKey()) ) {

				logger.log(Level.INFO, username + " trusts " + message.getUserID());
				
				PGP2PMessage verifyReply = getMessageHeader()
					.setType(PGP2PService.VERIFY_REPLY)
					.setStatus(PGP2PService.STATUS_OK)
					.addTrack(message.getTrack());
				
				logger.log(Level.INFO, "Sending VERIFY_REPLY with STATUS_OK");
				
				sendMessage(message.getUserID(), verifyReply);
				
			} else {
				
				if ( verifyService.isTrusted(message.getArmoredPublicKey())) {
					logger.log(Level.INFO, username + " trusts " + message.getUserID());
					
					PGP2PMessage verifyReply = getMessageHeader()
						.setType(PGP2PService.VERIFY_REPLY)
						.setStatus(PGP2PService.STATUS_OK)
						.addTrack(message.getTrack());
					
					logger.log(Level.INFO, "Sending VERIFY_REPLY with STATUS_OK");
					
					sendMessage(message.getUserID(), verifyReply);
					return;
				}
				
				if (message.getTrack().size() > PGP2PService.LIMIT_VERIFY_DEPTH) {
					logger.log(Level.INFO, "Search depth limit reached.");
					return;
				}
				
				Iterator<PGPPublicKey> trustedPeers;
				trustedPeers = pgpManager.getTrustedPublicKeys().iterator();
				while ( trustedPeers.hasNext() ) {
					PGPPublicKey trustedPeer = trustedPeers.next();
					String trustedUserID = PGPManager.getUserID(trustedPeer);
					
					if (message.getTrack().contains(trustedUserID)){
						logger.log(Level.INFO, "The user "+ trustedUserID + " has already been consulted.");
						break;
					}

					logger.log(Level.INFO, "Repassing verify request to " + trustedUserID);

					message
						.setType(PGP2PService.VERIFY_REQUEST)
						.setFromConnect(false)
						.addTrack(username);
					
					sendMessage(trustedUserID, message);
				}
			}
				
		} catch (PGPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}
	}

	
	private void processVerifyReply(PGP2PMessage message) {
		
		// TODO - validates reply and begins searching for other peers or giving
		// a OK
		logger.log(Level.INFO, "Processing VERIFY_REPLY");
		
		if ( message.getStatus() == PGP2PService.STATUS_OK) {
			logger.log(Level.INFO, "Recieved VERIFY_REPLY with STATUS_OK");
			
			// TODO - if the verify reply was OK, import the publicKey in local keyRing
			// via signRequest
		} else {
			logger.log(Level.INFO, "Recieved VERIFY_REPLY with STATUS_ERRROR");
		}
	}

	private void processSignRequest(PGP2PMessage message) {

		// TODO - verifies trust for the requestor and then sign its key with
		// processSignReply
		logger.log(Level.INFO, "Processing SIGN_REQUEST");
	}

	private void processSignReply(PGP2PMessage message) {

		// TODO - save the signature in the user's keychain
		logger.log(Level.INFO, "Processing SIGN_REPLY");
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
