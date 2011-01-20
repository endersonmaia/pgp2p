package net.pgp2p.verifyservice;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.pgp2p.cryptoservice.PGPManager;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;

public class PGPVerify {
	
	/**
	 * Logger for this class
	 */
	private final static Logger logger = Logger.getLogger(PGPVerify.class.getName()); 

	
	public PGPManager manager;

	public PGPVerify(PGPManager manager) {
		this.manager = manager;
	}

	/**
	 * Verifies if a given key is known and trusted in the keyring of the PGPManager.
	 * 
	 * @param pubKey
	 * @return boolean - true if the keys is known and trusted, false otherwise 
	 * @throws PGPException
	 */
	public boolean isTrusted(PGPPublicKey pubKey) throws PGPException {
		//TODO - considerar o nível de confiança

		Iterator sigs = pubKey.getSignatures();
		PGPSignature sign = null;
		
		while (sigs.hasNext()) {
			sign = (PGPSignature) sigs.next();
			if (sign.getKeyID() == manager.getPublicKey().getKeyID()) {
				logger.log(Level.INFO, manager.getUserID() + " CONFIA em " + pubKey.getUserIDs().next());
				return true;
			}
		}
		
		logger.log(Level.INFO, manager.getUserID() + " NÃO CONFIA em " + pubKey.getUserIDs().next());
		return false;
	}

	
}
