package net.pgp2p.app;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.bouncycastle.openpgp.PGPException;

import net.pgp2p.cryptoservice.PGPManager;
import net.pgp2p.exceptions.PGP2PUnreachablePeerException;
import net.pgp2p.networkhandler.ADHOCPeer;


//TODO - concluir classe de fachada
public class PGP2PUser {
	
	private ADHOCPeer peer;
	private PGPManager pgp;

	private String username;
	
	public PGP2PUser(String username) throws FileNotFoundException, IOException, PGPException {
		this.username = username;
		
		pgp	 = new PGPManager(username);
		peer = new ADHOCPeer(username);
		start();
	}
	
	
	public void start() {
		peer.start();
	}
	
	public boolean isTrust(String username) {
		
		return true;
	}
	
	public boolean isReachable(String username) {
		if ( ! peer.canReach(username) ) {
			return false;
		}
		return true;		
	}
	
	public boolean isTrustedBySomeoneITrust() {
		
	}
	
	public boolean sign(String username) {
		return true;
	}

	public String getUsername() {
		return username;
	}
	
		

}
