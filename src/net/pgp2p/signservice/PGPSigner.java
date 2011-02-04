package net.pgp2p.signservice;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.iapi.sql.dictionary.SubKeyConstraintDescriptor;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRing;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;

import net.pgp2p.cryptoservice.PGPManager;
import net.pgp2p.verifyservice.PGPVerify;

public class PGPSigner {
	
	/**
	 * Logger for this class
	 */
	private final static Logger logger = Logger.getLogger(PGPSigner.class.getName()); 

	
	public PGPManager manager;

	public PGPSigner(String path) {
		
		try {
			//this.manager = PGPManager.getInstance(path);
			this.manager = new PGPManager(path);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PGPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
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
	 * @throws IOException 
	 */
	public void signPublicKey(Iterator<PGPPublicKey> pubKeys) throws PGPException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException, IOException {
		while (pubKeys.hasNext()) {
			PGPPublicKey pubKey = pubKeys.next();
			importKey(pubKey);
			if (pubKey.isMasterKey()) {
				//TODO - sign keys
			}
		}
	}

	public void signPublicKey(PGPPublicKey publicKey) throws IOException, PGPException {
		
		importKey(publicKey);
		// TODO - sign imported keys
		
//      PGPSecretKey             pgpSec = manager.getSecretKey();
//      PGPPrivateKey            pgpPrivKey = pgpSec.extractPrivateKey("".toCharArray(), "BC");
//      
//      PGPSignatureGenerator    sGen = new PGPSignatureGenerator(pgpSec.getPublicKey().getAlgorithm(), PGPUtil.SHA1, "BC");
//      
//      sGen.initSign(PGPSignature.DEFAULT_CERTIFICATION, pgpPrivKey);
//      
//      PGPSignature signature = sGen.generateCertification(pubKey);
//      
//		PGPPublicKey.addCertification(pubKey, signature);
//
//		PGPPublicKey pubKeyInsideKeyRing = manager.getPublicKeyByUserID((String) pubKey.getUserIDs().next());
//		
//		if ( pubKeyInsideKeyRing != null) {
//			System.out.println("Chave " + Long.toHexString(pubKeyInsideKeyRing.getKeyID()) + " encontrada no keyRing");    			
//		} else {
//			System.out.println("N‹o foi opssivel encontrar a chave fornecida dentro do keyring");
//		}
//			
	}
	
	private void importKey(PGPPublicKey publicKey) throws PGPException, IOException {

		
		//if ( manager.getTrustedPublicKeys().contains(publicKey) ) {
		if (manager.publicKeyRing.getPublicKey(publicKey.getKeyID()) != null) {
			logger.log(Level.INFO, manager.getUserID() + " j‡ tem a chave "+ Long.toHexString(publicKey.getKeyID()));
			return;
		}
		
		PGPVerify verify = new PGPVerify(manager);
		
		if (! verify.isTrusted(publicKey) ) {
			
			logger.log(Level.INFO, manager.getUserID() + " importando a chave "+ Long.toHexString(publicKey.getKeyID()).toUpperCase().substring(8, 16));
           
    		PGPPublicKeyRing newPubKeyRing = new PGPPublicKeyRing(publicKey.getEncoded());
    		
    		manager.publicKeyRing = PGPPublicKeyRingCollection.addPublicKeyRing(manager.publicKeyRing, newPubKeyRing);
    		manager.savePublicKeyRing();
		}
		return;
	}
}
