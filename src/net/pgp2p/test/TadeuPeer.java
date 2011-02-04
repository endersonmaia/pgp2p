package net.pgp2p.test;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.pgp2p.cryptoservice.PGPManager;
import net.pgp2p.networkhandler.ADHOCPeer;
import net.pgp2p.networkhandler.PGP2PMessage;
import net.pgp2p.networkhandler.PGP2PService;

public class TadeuPeer {
	public static void main(String[] args) {
		
		Logger logger = Logger.getLogger(ADHOCPeer.class
				.getName());
		
		logger.setLevel(Level.ALL);
		
		try {
			
			String username = "tadeu";
			
			// creating peer
			PGPManager tadeuPGP = new PGPManager("./sandbox/gnupg-test/" + username);
			ADHOCPeer tadeuPeer = new ADHOCPeer(username).start();

			PGP2PMessage message = new PGP2PMessage();
			
			// TESTE 00
			// CONNECT_REQUEST FROM TADEU TO JOSE
			message
				.setFromUserID(username)
				.setSourceUserID(username)
				.setFinalUserID("joao")
				.setKeyID(tadeuPGP.getPublicKey().getKeyID())
				.setArmoredPublicKey(tadeuPGP.getArmoredPublicKey())
				.setType(PGP2PService.CONNECT_REQUEST);
			
			tadeuPeer.sendMessage("joao", message);
			message = null;

			
			
			// TEST 01
			// Send a CONNECT_REQUEST to cristina with final = jose
			// Expected: Received CONNECT_REPLY with STATUS_ERROR from cristina Code: 8
			/*
			message
				.setFromUserID(username)
				.setSourceUserID(username)
				.setFinalUserID("jose")
				.setKeyID(tadeuPGP.getPublicKey().getKeyID())
				.setArmoredPublicKey(tadeuPGP.getArmoredPublicKey())
				.setType(PGP2PService.CONNECT_REQUEST);
			tadeuPeer.sendMessage("cristina", message);
			message = null;
			*/
			
			//TEST 02
			// Send a C
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
