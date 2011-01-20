package net.pgp2p.cryptoservice;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.derby.iapi.util.ByteArray;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.UserIDPacket;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRing;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.util.encoders.Hex;

import sun.misc.HexDumpEncoder;


/**
 * This class is used to manage a PGP directory with public and secret 
 * keyrings. There's a premise that each secret keyring can only contain 
 * one private key. Like it's a directory per user. 
 * 
 * @author Enderson Maia <endersonmaia@gmail.com>
 *
 */
public class PGPManager {
	
	static {
		// init the security provider
		Security.addProvider(new BouncyCastleProvider());
	}

	/**
	 * Logger for this class
	 */
	private final static Logger logger = Logger.getLogger(PGPManager.class.getName()); 

	/**
	 * Constantes que armazenam o nome padr‹o dos arquivos de chaves pœblicas e privadas.
	 */
	//GnuPG
	private static final String PUBRING_FILE = "pubring.gpg";
	private static final String SECRING_FILE = "secring.gpg";

	// BouncyCastle
	//private static final String PUBRING_FILE = "pubring.bpg";
	//private static final String SECRING_FILE = "secring.bpg";
	
	/**
	 * Caminha para pasta onde ser‹o armazenados os arquivos do PGP.
	 */
	private String keyRingPath;

	/**
	 * Caminho para o arquivo que armazena as chaves pœblicas.
	 */
	private String pubringFilePath;
	
	/**
	 * Caminho para o arquivo que armazena as chaves privadas.
	 */
	private String secringFilePath;
	
	/**
	 * The PublicKeyring in the pubringFilePath
	 */
	public PGPPublicKeyRingCollection publicKeyRing;
	
	/**
	 * The SecretKeyring in the secringFilePath
	 */
	private PGPSecretKeyRing secretKeyRing;

	/**
	 * Instanciates the PGPManager informing the directory in which the 
	 * pubring and secring will be stored.
	 * 
	 * @param path
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws PGPException
	 */
	public PGPManager(String path) throws FileNotFoundException, IOException, PGPException {
		logger.setLevel(Level.OFF);
		setKeyRingPath(path);

		this.pubringFilePath = path + System.getProperty("file.separator") + PUBRING_FILE;
		publicKeyRing = new PGPPublicKeyRingCollection(new FileInputStream(this.pubringFilePath));
		
		this.secringFilePath = path + System.getProperty("file.separator") + SECRING_FILE;
		secretKeyRing = new PGPSecretKeyRing(new FileInputStream(this.secringFilePath));
		
		prepareKeyRing();
	}
	
	public String getKeyRingPath(){
		return this.keyRingPath;
	}

	public void setKeyRingPath(String path){
		this.keyRingPath = path;
	}
	
    /**
	 * Return the first public key in the public keyring, considered the onwner's public key.
	 *  
	 * @return PGPPublicKey
	 * @throws PGPException 
	 */
	public PGPPublicKey getPublicKey() throws PGPException  {
		PGPPublicKey pubKey = this.publicKeyRing.getPublicKey(getSecretKey().getKeyID());
		logger.log(Level.INFO, "Returninig public key "+ Long.toHexString(pubKey.getKeyID()));
		return pubKey;
	}

	/**
	 * Recupera o ID do usu‡rio da primeira chave pœblica na pubring.
	 *  
	 * @return String
	 * @throws PGPException 
	 */
	public String getUserID() throws PGPException {
		
		PGPPublicKey pubKey = getPublicKey();
		
		Iterator i = pubKey.getUserIDs();
		
		String userID = null;
		while(i.hasNext()){
			userID = (String)i.next();
		}
		
		logger.log(Level.INFO,"userID: " + userID+", keyID: "+Long.toHexString(pubKey.getKeyID()));
		return userID;
	}
	

	/**
	 * Returns a list of Users ID that exists in the local publicKeyRing
	 * 
	 * @return List<PGPPUblicKeys>
	 * @throws PGPException 
	 */
	public List<PGPPublicKey> getTrustedPublicKeys() throws PGPException {
		
		Iterator<PGPPublicKeyRing>    rIt = this.publicKeyRing.getKeyRings();
		List<PGPPublicKey> trustedPubKeys = new ArrayList();

		long ownerKeyID	= getSecretKey().getKeyID();
		long trustedKeyID;
		
        while (rIt.hasNext()) {
            PGPPublicKey pubKey = (PGPPublicKey)rIt.next().getPublicKey();
            trustedKeyID = pubKey.getKeyID();
            if( trustedKeyID != ownerKeyID) {
            	trustedPubKeys.add(pubKey);
            	logger.log(Level.INFO,"userID: " + trustedKeyID+", keyID: "+Long.toHexString(pubKey.getKeyID()));
            }
        }

		return trustedPubKeys;
	}
	
	/**
	 * Returns the PGPPublicKey for the given the user name or e-mail
	 * 
	 * @param userID
	 * @return PGPPublicKey
	 * @throws PGPException 
	 */
	public PGPPublicKey getPublicKeyByUserID(String userID) throws PGPException {

		Iterator keyRings = this.publicKeyRing.getKeyRings(userID, true);
		
		PGPPublicKey pubKey;
		if (keyRings.hasNext()) {
			pubKey = ((PGPPublicKeyRing) keyRings.next()).getPublicKey();
			logger.log(Level.INFO, "Found key " + Long.toHexString(pubKey.getKeyID()) + " when searching for "+ userID);
			return pubKey;
		} else {
			return null;
		}
	}

	/**
	 * Prepares the directory for storing the PUBRING_FILE and SECRING_FILE.
	 * 
	 * Creates the keyring directory according to the keyRingPath and creates 
	 * the PUBRING_FILE and SECRING_FILE. If the directory and files already exists,
	 * it does nothing.
	 *  
	 * @return void
	 */
	private void prepareKeyRing(){
		if (this.keyRingPath != null) {
		
			File dir = new File(this.keyRingPath);
			if ( ! dir.exists() ) {
				dir.mkdirs();
				logger.log(Level.INFO, "Criando diret—rio " + this.keyRingPath + ".");
			} else {
				logger.log(Level.INFO, "Diret—rio informado j‡ existe.");
			}
			
		}
		//TODO - create files
		/* boolean success_on_creating_directory = dir.mkdirs();
		if (success_on_creating_directory) {
			(new File(dir.getPath() + PUBRING_FILE)).;
			new File(dir.getPath() + SECRING_FILE);
			return true;
		} else {
			return false;
		} */
	}

	/**
	 * Verifies if the keyring that exists contains a valid PGP keyring.
	 * 
	 */
	// TODO - verifies if the path informed is a valid keyring
	/*private boolean isKeyRingValid {
		
		File keyRing = new File(this.keyRingPath);
		
		for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
			type type = (type) iterator.next();
			
		}
		
		for (File keyRing.listFiles() : file) {
			System.out.println(file.getName());
		} 
		
		return false;
	}*/
	
	
	/**
	 * If the pubring and secring doesn't exist in the informed directory, 
	 * they can be created with this method.
	 * 
	 * @param String identity - the identification of the keypair (Ex.: "John Doe <john.doe@example.com>")
	 * @param char[] password - the password that will protect the secring.
	 */
	private void createPGPKeyring(String identity, char[] password)  {
		
		PGPKeyPair keyPair = createPGPKeyPair();
		
		PGPKeyRingGenerator keyRingGenerator = null;
		try {
			keyRingGenerator = new PGPKeyRingGenerator(
					PGPSignature.POSITIVE_CERTIFICATION, 
					keyPair, 
					identity, 
					PGPEncryptedData.AES_256, 
					password, 
					true, null, null, 
					new SecureRandom(), 
					"BC");
		} catch (NoSuchProviderException nspe) {
			// TODO Auto-generated catch block
			nspe.printStackTrace();
		} catch (PGPException pe) {
			// TODO Auto-generated catch block
			pe.printStackTrace();
		}
		
		try {
			OutputStream secring = new FileOutputStream(this.keyRingPath + System.getProperty("file.separator") + SECRING_FILE);
			//secring = new ArmoredOutputStream(secring);
			
			OutputStream pubring = new FileOutputStream(this.keyRingPath+ System.getProperty("file.separator") + PUBRING_FILE);
			//pubring = new ArmoredOutputStream(pubring);
			
			keyRingGenerator.generateSecretKeyRing().encode(secring);
			keyRingGenerator.generatePublicKeyRing().encode(pubring);
		} catch (FileNotFoundException fnfe) {
			// TODO Auto-generated catch block
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		}
	}

	/**
	 * This method is used to create the keypair to be stored with createPGPKeyring()
	 * 
	 * @return PGPKeyPair
	 */
	private static PGPKeyPair createPGPKeyPair() {
	
		// instanciando um gerador de chaves DSA com o provider BouncyCastle
		KeyPairGenerator dsaKeyPairGenerator = null;
		try {
			dsaKeyPairGenerator = KeyPairGenerator.getInstance("DSA", "BC");
		} catch (NoSuchAlgorithmException nsae) {
			// TODO Auto-generated catch block
			nsae.printStackTrace();
		} catch (NoSuchProviderException nspe) {
			// TODO Auto-generated catch block
			nspe.printStackTrace();
		}
	
		dsaKeyPairGenerator.initialize(1024);
	
	    // gerando o par de chaves
	    KeyPair dsaKeyPair = dsaKeyPairGenerator.generateKeyPair();
	    PGPKeyPair pgpKeyPair = null;
		try {
			pgpKeyPair = new PGPKeyPair(PGPPublicKey.DSA, dsaKeyPair, new Date());
		} catch (PGPException pe) {
			// TODO Auto-generated catch block
			pe.printStackTrace();
		}
		
		return pgpKeyPair;
	}

	/**
	 * A simple routine that opens a key ring file and loads the first available key suitable for
	 * signature generation.
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws PGPException
	 */
	public PGPSecretKey getSecretKey() {
		
		PGPSecretKey secKey = this.secretKeyRing.getSecretKey();
		logger.log(Level.INFO, "Returning private key "+ Long.toHexString(secKey.getKeyID()));
		return secKey;
	}


}
