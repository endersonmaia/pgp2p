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
	public void sendMessage(PeerID peerID, Message msg) throws IOException {

		// gets the messageType code
		int messageType = Integer.parseInt(msg.getMessageElement(
				PGP2PService.NAMESPACE, PGP2PMessage.TYPE_FIELD).toString());

		// gets the EndpointAddress for the service identified by the
		// messageType
		EndpointAddress addr = new EndpointAddress(peerID,
				PGP2PService.NAMESPACE, PGP2PService.PARAMS[messageType]);

		// verify if the endpoint is reachable ...
		if (endpointService.isReachable(peerID, true)) {

			Messenger messenger = endpointService.getMessenger(addr);

			logger.log(Level.INFO, "Can reach.\n" + "peerID: "
					+ peerID.toString());

			// if messageType is valid ...
			if (messageType <= PGP2PService.PARAMS.length) {

				// sends the message
				if (messenger.sendMessage(msg)) {
					logger.log(Level.INFO, "Message sent.");
				} else {
					logger.log(Level.INFO, "Can't send message.");
				}

			} else {
				logger.log(Level.INFO, "Invalid message type.");
			}
		} else {
			logger.log(Level.INFO, "Can't reach.\n" + "peerID: "
					+ peerID.toString());
		}
	}

	public void sendMessage(String username, Message msg) throws IOException {
		sendMessage(getPeerIDforUserame(username), msg);
	}

	@Override
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
					processConnectRequest(msg, srcAddr, dstAddr);
					break;
				case PGP2PService.CONNECT_REPLY:
					logger
							.log(Level.INFO,
									"Recieving a connect reply, sending the message do processConnectReply.");
					processConnectReply(msg, srcAddr, dstAddr);
					break;
				case PGP2PService.VERIFY_REQUEST:
					logger
							.log(Level.INFO,
									"Recieving a verify request, sending the message to proccessVerifyRequest.");
					processVerifyRequest(msg, srcAddr, dstAddr);
					break;
				case PGP2PService.VERIFY_REPLY:
					logger
							.log(Level.INFO,
									"Recieving a verify reply, sending the message do processVerifyReply.");
					processVerifyReply(msg, srcAddr, dstAddr);
					break;
				case PGP2PService.SIGN_REQUEST:
					logger
							.log(Level.INFO,
									"Recieving a sign request, sending the message to proccessSignRequest.");
					processSignRequest(msg, srcAddr, dstAddr);
					break;
				case PGP2PService.SIGN_REPLY:
					logger
							.log(Level.INFO,
									"Recieving a sign reply, sending the message do processSignReply.");
					processSignReply(msg, srcAddr, dstAddr);
					break;

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	/**
	 * Recieves a connection request from another Peer and must check if 
	 * this Peer is in the local trust.
	 * 
	 * In case the requestor is not in trust list, a verifyRequest is sent 
	 * for each Peer in local trust, to find someone who trusts the requestor.
	 * If it's not found, a CONNECT_REPLY with NOT_IN_WEB_OF_TRUST is rerturned.
	 * 
	 * @param message
	 * @param srcAddr
	 * @param dstAddr
	 * @throws IOException
	 */
	private void processConnectRequest(PGP2PMessage message,
			EndpointAddress srcAddr, EndpointAddress dstAddr)
			throws IOException {

		logger.log(Level.INFO, "Processing CONNECT_REQUEST");
		
		// TODO - process message and checks for trust


		try {
			Message replyMessage = new PGP2PMessage()
			.setUserID(pgpManager.getUserID())
			.setKeyID(pgpManager.getPublicKey().getKeyID())
			.setArmoredPublicKey(pgpManager.getArmoredPublikKey())
			.setType(PGP2PService.CONNECT_REPLY);
			
			if (verifyService.isTrusted(message.getArmoredPublicKey()) ) {

				((PGP2PMessage) replyMessage).setStatus(PGP2PService.STATUS_OK);
				sendMessage(message.getUserID(), replyMessage);

			} else {

				((PGP2PMessage) replyMessage).setStatus(PGP2PService.STATUS_ERROR);
				sendMessage(message.getUserID(), replyMessage);

			}
		} catch (PGPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void processConnectReply(PGP2PMessage message,
			EndpointAddress srcAddr, EndpointAddress dstAddr) {

		// TODO - validates reply and begins searching for other peers or giving
		// a OK
		logger.log(Level.INFO, "Processing CONNECT_REPLY");
	}

	private void processVerifyRequest(PGP2PMessage message,
			EndpointAddress srcAddr, EndpointAddress dstAddr) {

		// TODO - validates reply and begins searching for other peers or giving
		// a OK
		logger.log(Level.INFO, "Processing VERIFY_REPLY");
	}

	private void processVerifyReply(PGP2PMessage message,
			EndpointAddress srcAddr, EndpointAddress dstAddr) {

		// TODO - validates reply and begins searching for other peers or giving
		// a OK
		logger.log(Level.INFO, "Processing VERIFY_REPLY");
	}

	private void processSignRequest(PGP2PMessage message,
			EndpointAddress srcAddr, EndpointAddress dstAddr) {

		// TODO - verifies trust for the requestor and then sign its key with
		// processSignReply
		logger.log(Level.INFO, "Processing SIGN_REQUEST");
	}

	private void processSignReply(PGP2PMessage message,
			EndpointAddress srcAddr, EndpointAddress dstAddr) {

		// TODO - save the signature in the user's keychain
		logger.log(Level.INFO, "Processing SIGN_REPLY");
	}

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
