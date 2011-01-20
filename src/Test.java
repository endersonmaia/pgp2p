import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.sun.tools.hat.internal.model.StackTrace;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.Advertisement;
import net.jxta.document.AdvertisementFactory;
import net.jxta.document.MimeMediaType;
import net.jxta.document.StructuredTextDocument;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.id.IDFactory;
import net.jxta.impl.membership.pse.StringAuthenticator;
import net.jxta.logging.Logging;
import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.NetPeerGroupFactory;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.platform.ModuleSpecID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.protocol.ConfigParams;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.PeerGroupAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import net.jxta.rendezvous.RendezVousService;
import net.jxta.rendezvous.RendezvousEvent;
import net.jxta.rendezvous.RendezvousListener;


public class Test implements RendezvousListener {
	
	RendezVousService     netpgRendezvous;
	RendezVousService     apppgRendezvous;
   private 			NetworkConfigurator		configurator;
   private 			PeerGroup 				netPeerGroup;
   private			PeerGroup				newGroup;
   private 			DiscoveryService		discovery;
   private static final String JXTA_HOME = ".";
  
   // Rendez-vous config
   private final String RDV_CONFIG = "rdv_config";
   private final String RDV_NAME = "Rendevouz Peer";
   private final String RDV_DESCRIPTION = "Rendezvous Peer for test with PGPMembership";

   private final String peerID = "urn:jxta:uuid-79B6A084D3264DF8B641867D926C48D9F8BA10F44BA74475ABE2BB568892B0DC03";

   private static Logger logger;
      
   /** Configure rendezvous peer
    * 
    */
   private void configure() {
	  
	   // Cria configuração informando local da configuração
	   configurator = NetworkConfigurator.newRdvConfiguration(
			   new File(JXTA_HOME + System.getProperty("file.separator") + RDV_CONFIG).toURI()
	   		);
	   
	   configurator.setName(RDV_NAME);
	   configurator.setDescription(RDV_DESCRIPTION);
	   configurator.setPeerId(peerID);

	   configurator.setUseMulticast(true);

	   URI seedingURI;
	   seedingURI = new File(JXTA_HOME + 
			   System.getProperty("file.separator") + RDV_CONFIG +
			   System.getProperty("file.separator") + "seeds.txt"
			  
	   
	   ).toURI();
	   
	   configurator.addRdvSeedingURI(seedingURI);
	   configurator.addRelaySeedingURI(seedingURI);
	   configurator.setMode(NetworkConfigurator.RDV_SERVER + NetworkConfigurator.RELAY_SERVER);
	   
	   configurator.setUseOnlyRelaySeeds(false);
	   configurator.setUseOnlyRendezvousSeeds(false);
 
	   configurator.setTcpEnabled(true);
	   configurator.setTcpIncoming(true);
	   configurator.setTcpOutgoing(true);

	   try {
		   configurator.save();
	   } catch(IOException e) {
		    e.printStackTrace();
		    System.exit(1);
	   }
	
	   System.out.println("Platform configured and saved.");
	   
   }
   
   private void jxtaStart() throws PeerGroupException, Exception {
	      System.out.println("Starting JXTA platform");

	      NetPeerGroupFactory factory;
	      try {
	         factory = new NetPeerGroupFactory(
	            (ConfigParams)configurator.getPlatformConfig(),
	            new File(JXTA_HOME + System.getProperty("file.separator") + RDV_CONFIG).toURI()
	         );
	      }
	      catch(Exception e) {
	         throw new Exception(e.getMessage());
	      }

	      netPeerGroup = factory.getInterface();

	      // The rendezvous service for NetPeerGroup
	      netpgRendezvous = netPeerGroup.getRendezVousService();
	      netpgRendezvous.addListener(this);
	      netpgRendezvous.startRendezVous();

	      // The NetPeerGroup discovery service 
	      discovery = netPeerGroup.getDiscoveryService();

	      System.out.println("Platform started");
	   }

	private void createPeerGroup() throws Exception, PeerGroupException {

	      // The new-application subgroup parameters.
	      String name = "PGP Test Peer Group";
	      String desc = "Peer Grup para teste de autenticação com PGP";
	      String gid =  "urn:jxta:uuid-79B6A084D3264DF8B641867D926C48D902";
	      String specID = "urn:jxta:uuid-309B33F10EDF48738183E3777A7C3DE9C5BFE5794E974DD99AC7D409F5686F3306";

	      StringBuilder sb = new StringBuilder("=Creating group:  ");
	      sb.append(name).append(", ");
	      sb.append(desc).append(", ");
	      sb.append(gid).append(", ");
	      sb.append(specID);
	      
	      System.out.println(sb.toString());

	      ModuleImplAdvertisement implAdv = netPeerGroup.getAllPurposePeerGroupImplAdvertisement();
	      ModuleSpecID modSpecID = (ModuleSpecID )IDFactory.fromURI(new URI(specID));
	      implAdv.setModuleSpecID(modSpecID);

	      // Publish the Peer Group implementation advertisement.
	      discovery.publish(implAdv);
	      discovery.remotePublish(null, implAdv);

	      //   Create the new group using the group ID, advertisement, name, and description
	      PeerGroupID groupID = (PeerGroupID )IDFactory.fromURI(new URI(gid));
	      newGroup = netPeerGroup.newGroup(groupID,implAdv,name,desc);

	      // Start the rendezvous for our application subgroup.
	      apppgRendezvous = newGroup.getRendezVousService();
	      apppgRendezvous.addListener(this);
	      apppgRendezvous.startRendezVous();

	      // Publish the group remotely.  newGroup() handles the local publishing. 
	      PeerGroupAdvertisement groupAdv = newGroup.getPeerGroupAdvertisement();
	      discovery.remotePublish(null, groupAdv);

	      System.out.println("Private Application newGroup = " + name + " created and published");
	}
	
	private void joinGroup(PeerGroup grp) {
		
		MembershipService membership = grp.getMembershipService();
		
		AuthenticationCredential authCred = 
			new AuthenticationCredential(grp, null, null);
		
        StringAuthenticator stringAuth = (StringAuthenticator) membership.apply(authCred);

        stringAuth.setAuth1_KeyStorePassword(MyKeyStorePassword);
        stringAuth.setAuth2Identity(PID);
        stringAuth.setAuth3_IdentityPassword(MyPrivateKeyPassword);
		
		Authenticator auth = null;
		try {
			auth = (Authenticator) membership.apply(authCred);
			if (auth.isReadyForJoin()){
				System.out.println("Is ready for join!");
				Credential myCred = membership.join(auth);
			} else {
				System.out.println("NOT ready for join!");
			}
		} catch (PeerGroupException e) {
			System.out.println("EXCEPTION : Peer group esception!");
			e.printStackTrace();
		} catch (ProtocolNotSupportedException e) {
			System.out.println("EXCEPTION : Protocol not supported!");
			e.printStackTrace();
		}
		
	
	}
   
	public static void main(String[] args) {
		//Test test = new Test();
		//test.start();
	}
	
	public void start() {
		
		LogManager.getLogManager().reset();
		//logger = Logger.getLogger(this.getClass().toString());
		//logger.setLevel(Level.INFO);
		
		try {
			configure();
			jxtaStart();
			createPeerGroup();
			joinGroup(newGroup);
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}


	@Override
	public void rendezvousEvent(RendezvousEvent event) {
		
	    String eventDescription;
	    int    eventType;

         eventType = event.getType();

         switch( eventType ) {
            case RendezvousEvent.RDVCONNECT:
               eventDescription = "RDVCONNECT";
               break;
            case RendezvousEvent.RDVRECONNECT:
               eventDescription = "RDVRECONNECT";
               break;
            case RendezvousEvent.RDVDISCONNECT:
               eventDescription = "RDVDISCONNECT";
               break;
            case RendezvousEvent.RDVFAILED:
               eventDescription = "RDVFAILED";
               break;
            case RendezvousEvent.CLIENTCONNECT:
               eventDescription = "CLIENTCONNECT";
               break;
            case RendezvousEvent.CLIENTRECONNECT:
               eventDescription = "CLIENTRECONNECT";
               break;
            case RendezvousEvent.CLIENTDISCONNECT:
               eventDescription = "CLIENTDISCONNECT";
               break;
            case RendezvousEvent.CLIENTFAILED:
               eventDescription = "CLIENTFAILED";
               break;
            case RendezvousEvent.BECAMERDV:
               eventDescription = "BECAMERDV";
               break;
            case RendezvousEvent.BECAMEEDGE:
               eventDescription = "BECAMEEDGE";
               break;
            default:
               eventDescription = "UNKNOWN RENDEZVOUS EVENT";
         }

         System.out.println("RendezvousEvent:  event =  " 
                     + eventDescription + " from peer = " + event.getPeer());	
	}

}
