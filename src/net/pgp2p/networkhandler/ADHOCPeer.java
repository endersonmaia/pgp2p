package net.pgp2p.networkhandler;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.tools.tree.IncDecExpression;

import net.jxta.endpoint.EndpointAddress;
import net.jxta.endpoint.EndpointListener;
import net.jxta.endpoint.EndpointService;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.Messenger;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.impl.endpoint.EndpointServiceImpl;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;

public class ADHOCPeer implements EndpointListener {
	
    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(ADHOCPeer.class.getName());

	private String username;
	private String tcpPort;
	private File configFile;

	public PeerID PID;

	private PeerGroup netPeerGroup;
	private NetworkManager netManager;

	private EndpointService endpointService;

	public ADHOCPeer(String username) {
		this.username = username;
		this.tcpPort = StringToTCPPortNumber.get(username);
		this.configFile = new File("." + System.getProperty("file.separator")
				+ "jxta" + System.getProperty("file.separator") + username);
		this.PID = getPeerIDforUserame(username);

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

	public Message getHelloMessage(String username) {
		Message msg = new Message();
		StringMessageElement titleElement = new StringMessageElement("TITLE",
				"Hello from " + username, null);
		// StringMessageElement bodyElement = new StringMessageElement("BODY",
		// "Hi " + username + ", \n" +
		// "how are you ?"
		// , null);
		msg.addMessageElement("MSG", titleElement);
		// msg.addMessageElement("MSG", bodyElement);
		return msg;
	}

	public ADHOCPeer start() {
		try {
			netManager = new NetworkManager(NetworkManager.ConfigMode.ADHOC,
					username, configFile.toURI());

			NetworkConfigurator netConfig = netManager.getConfigurator();

			// Setting Configuration
			netConfig.setUseMulticast(true);
			netConfig.setTcpPort(Integer.parseInt(tcpPort));
			netConfig.setTcpEnabled(true);
			netConfig.setTcpIncoming(true);
			netConfig.setTcpOutgoing(true);
			netConfig.setPeerID(PID);

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
			logger.log(Level.INFO, "Listening on the port " + tcpPort.toString());

		} catch (Exception e) {
			e.printStackTrace();
		}

		return this;
	}

	private void registerListeners() {
		for (int i = 0; i < PGP2PService.PARAMS.length; i++) {
			endpointService.addIncomingMessageListener(this, PGP2PService.NAME, PGP2PService.PARAMS[i]);
		}
	}

	public boolean canReach(String username) {
		return endpointService.isReachable(getPeerIDforUserame(username), true);
	}
	
	public void sendMessage(PeerID peerID, Message msg, String param) throws IOException {
		if ( endpointService.isReachable(peerID, true)) {
			logger.log(Level.INFO, "OK : CAN REACH");
			
			EndpointAddress addr = null;
			
			switch(msgParam) {
				case 1 : 
					addr = new EndpointAddress(getPeerIDforUserame(username)
					, "MSG", "PING");
					break;
				case 2:
					addr = new EndpointAddress(getPeerIDforUserame(username)
					, "MSG", "PONG");
					break;
			}
			
			Messenger messenger = endpointService.getMessenger(addr);
			Message msg = getHelloMessage(username);
	
			if (messenger.sendMessage(msg)) {
				System.out.println("OK : MESSAGE SENT");
			} else {
				System.out.println("FAIL : MESSAGE NOT SENT");
			}
		} else {
			System.out.println("FAIL: CANT REACH");
		}
	}

	public void sendMessage(String username,  msg, String param) throws IOException {
		sendMessage(getPeerIDforUserame(username), msg, param);
	}

	public void sendVerifyRequest(String username, VerifyRequestMessage msg) {
		String param = PGP2PService.PARAMS[PGP2PService.VERIFY_REQUEST];
		sendMessage(username, msg, param);
	}

	@Override
	public void processIncomingMessage(Message message,
			EndpointAddress srcAddr, EndpointAddress dstAddr) {
				
		Iterator<MessageElement> i = message.getMessageElements();
		while (i.hasNext()) {
			MessageElement elem = i.next();
			System.out.println(elem.getElementName() + ":" + elem.toString());
		}
		
	}

}
