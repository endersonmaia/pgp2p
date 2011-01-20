package net.jxta.impl.membership.pgp.example;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.membership.MembershipService;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.OutputPipeEvent;
import net.jxta.pipe.OutputPipeListener;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;

abstract class RendezvousPeer implements OutputPipeListener, PipeMsgListener {
	
    // PipeService.UnicastType or PipeService.UnicastSecureType or PipeService.PropagateType
    public static final String PipeType = PipeService.UnicastType;

	private String name;
	private int tcpPort;
	private PeerID PID;
	private File configurationFile;
	
	private NetworkManager networkManager;

	public RendezvousPeer(String name, int tcpPort) {
    	this.setName(name);
    	this.tcpPort = tcpPort;
    	this.PID = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID, name.getBytes());
    	this.configurationFile = new File("." + System.getProperty("file.separator") + name);
    	
    	configureNetwork();
	}
    

	@Override
	public void outputPipeEvent(OutputPipeEvent event) {
        try {
        	
            // Notifying the user
        	System.out.println("Alguém conectou ao Pipe, enviando mensagem.");

            // Retrieving the output pipe to the peer who connected to the input end
            OutputPipe outputPipe= event.getOutputPipe();
            
            // Creating a Hello message !!!
            Message message = new Message();
            StringMessageElement keyIDElement = new StringMessageElement("KeyID", "KeyID from " + getName(), null);
            message.addMessageElement("PGPVerify", keyIDElement);

            // Sending the message
            outputPipe.send(message);
            
        } catch (IOException ioe) {
            
            System.out.println("ERRO : Enviando mensagem via outputPipeEvent");
            ioe.printStackTrace();
            
        }		
	}

	@Override
	public void pipeMsgEvent(PipeMsgEvent event) {

		// mensagem recebida
        Message receivedMessage = event.getMessage();
        String message = receivedMessage.getMessageElement("PGPVerify", "KeyID").toString();

		System.out.println("Mensagem recebida: " + message);
	}
	
    private static PipeAdvertisement getPipeAdvertisement(RendezvousPeer rdvPeer) {
        
        // Creating a Pipe Advertisement
        PipeAdvertisement pipeAdvertisement = (PipeAdvertisement) AdvertisementFactory.newAdvertisement(PipeAdvertisement.getAdvertisementType());
        PipeID pipeID = IDFactory.newPipeID(PeerGroupID.defaultNetPeerGroupID, rdvPeer.getName().getBytes());

        pipeAdvertisement.setPipeID(pipeID);
        pipeAdvertisement.setType(PipeType);
        pipeAdvertisement.setName("Test Pipe");
        pipeAdvertisement.setDescription("Created by " + rdvPeer.getName());
        
        return pipeAdvertisement;
        
    }

    public void configureNetwork() {

    	try {
			// Creation of network manager
			networkManager = new NetworkManager(NetworkManager.ConfigMode.RENDEZVOUS,
			        getName(), configurationFile.toURI());
			
			// Retrieving the network configurator
			NetworkConfigurator networkConfigurator = networkManager.getConfigurator();
			
			// Setting more configuration
			networkConfigurator.setTcpPort(tcpPort);
			networkConfigurator.setTcpEnabled(true);
			networkConfigurator.setTcpIncoming(true);
			networkConfigurator.setTcpOutgoing(true);

			// Setting the Peer ID
			System.out.println("Peer " + this.getClass().toString() + " - configurando PeerID para : " + this.PID.toString());
			networkConfigurator.setPeerID(PID);
			
			// Saving the configuration
			networkConfigurator.save();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
    }
    
    public void startNetwork(RendezvousPeer rdvPeer) {

    	try {
			// Starting the JXTA network
			System.out.println("Peer " + getName() + " - iniciando a rede JXTA");
			PeerGroup netPeerGroup = networkManager.startNetwork();
			
			// Retrieving the PSE membership service
			MembershipService membershipService = netPeerGroup.getMembershipService();

			// Waiting for other peers to connect to JXTA
			System.out.println("Esperando outros peers conectarem");

			// Creating an input pipe
			PipeAdvertisement pipeAdv = getPipeAdvertisement(rdvPeer);
			PipeService pipeService = netPeerGroup.getPipeService();
			pipeService.createInputPipe(pipeAdv, rdvPeer);
			
			// Displaying pipe advertisement to identify potential compilation issues
			System.out.println(pipeAdv.toString());
			
			// Going to sleep for some time
			gotoSleep(60000);
			
			// Retrieving connected peers
			showConnectedRendezvous(netPeerGroup.getRendezVousService(), getName());
			
			// Resigning all credentials
			membershipService.resign();

			// Stopping the network
			System.out.println(getName() + " Stop JXTA network");
			networkManager.stopNetwork();
		} catch (PeerGroupException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
    }
    
    private static final void gotoSleep(long Duration) {
        
        long Delay = System.currentTimeMillis() + Duration;

        while (System.currentTimeMillis()<Delay) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException Ex) {
                // We don't care
            }
        }
        
    }
    
    private void showConnectedRendezvous(RendezVousService rendezvous, String name) {
        
        List<PeerID> rdvList = rendezvous.getLocalRendezVousView();
        Iterator<PeerID> i = rdvList.iterator();
        int count = 0;
        
        while (i.hasNext()) {
            
            count = count + 1;

            System.out.println(name + " Connected to rendezvous:\n\n"
                    + i.next().toString());
            
        }
        
        if (count==0) {
            
            System.out.println(name + "No rendezvous connected to this rendezvous!");
            
        }

    }
    
    public void start(RendezvousPeer rdvPeer) {
    	configureNetwork();
    	startNetwork(rdvPeer);
    }


	public void setName(String name) {
		this.name = name;
	}


	public String getName() {
		return name;
	}
}


