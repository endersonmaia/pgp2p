package net.pgp2p.signservice;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;

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
	 * @throws NoSuchProviderException 
	 * @throws NoSuchAlgorithmException 
	 * @throws SignatureException 
	 */
	public void sign(PGPPublicKey pubKey) throws PGPException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException {
		logger.log(Level.INFO, manager.getUserID() + " assinanto chave " + Long.toHexString(pubKey.getKeyID()));
		
        // Ler o pubKeyRing para poder salvar a assinatura l‡
        if ( manager.publicKeyRing.contains(pubKey.getKeyID()) ) {
        	logger.log(Level.INFO, manager.getUserID() + " j‡ tem a chave de " + pubKey.getUserIDs().next());
        } else {
            PGPSecretKey             pgpSec = manager.getSecretKey();
            PGPPrivateKey            pgpPrivKey = pgpSec.extractPrivateKey("".toCharArray(), "BC");
            
            PGPSignatureGenerator    sGen = new PGPSignatureGenerator(pgpSec.getPublicKey().getAlgorithm(), PGPUtil.SHA1, "BC");
            
            //sGen.initSign(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);
            
            sGen.generateCertification(pgpSec.getPublicKey(), pubKey);
    		//PGPPublicKey.addCertification(pubKey, manager.publicKeyRing);        	
        }
	}
}
