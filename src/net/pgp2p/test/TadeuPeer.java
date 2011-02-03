package net.pgp2p.test;

import net.pgp2p.cryptoservice.PGPManager;
import net.pgp2p.networkhandler.ADHOCPeer;
import net.pgp2p.networkhandler.PGP2PMessage;
import net.pgp2p.networkhandler.PGP2PService;

public class TadeuPeer {
	public static void main(String[] args) {
		try {
			
			String username = "tadeu";
			
			// creating peer
			PGPManager tadeuPGP = new PGPManager("./sandbox/gnupg-test/" + username);
			ADHOCPeer tadeuPeer = new ADHOCPeer(username).start();
			
			// creating verify message
			PGP2PMessage connectMessage = new PGP2PMessage()
				.setFromUserID(username)
				.setSourceUserID(username)
				.setFinalUserID("jose")
				.setKeyID(tadeuPGP.getPublicKey().getKeyID())
				.setArmoredPublicKey(tadeuPGP.getArmoredPublikKey())
				.setType(PGP2PService.CONNECT_REQUEST);
			
			// wait 10s
			//System.out.println("waintins 10s to send message");
			//Thread.sleep(10000);
		
			tadeuPeer.sendMessage(connectMessage.getFinalUserID(), connectMessage);
			
			connectMessage = null;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
