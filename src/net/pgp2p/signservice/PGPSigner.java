package net.pgp2p.signservice;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;

import net.pgp2p.cryptoservice.PGPManager;

public class PGPSigner {
	
	/**
	 * Logger for this class
	 */
	private final static Logger logger = Logger.getLogger(PGPSigner.class.getName()); 

	
	public PGPManager manager;

	public PGPSigner(PGPManager manager) {
		this.manager = manager;
	}
	
	/**
	 * Signs a given public key.
	 *  
	 * @param pubKey
	 * @throws PGPException 
	 */
	public void sign(PGPPublicKey pubKey) throws PGPException {
		logger.log(Level.INFO, manager.getUserID() + " assinanto chave " + Long.toHexString(pubKey.getKeyID()));
		
		
		
		PGPPublicKey.addCertification(pubKey, manager.publicKeyRing.)
		
	}


}
