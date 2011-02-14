/*
Copyright (c) 2011 Enderson Maia

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package net.pgp2p.cryptoservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;


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
	 * Singleton instance holder
	 */
	private static PGPManager instance = null;
	
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
		
		setKeyRingPath(path);

		this.pubringFilePath = path + System.getProperty("file.separator") + PUBRING_FILE;
		publicKeyRing = new PGPPublicKeyRingCollection(new FileInputStream(this.pubringFilePath));
		
		this.secringFilePath = path + System.getProperty("file.separator") + SECRING_FILE;
		secretKeyRing = new PGPSecretKeyRing(new FileInputStream(this.secringFilePath));
		
		prepareKeyRing();
	}
	
	/**
	 * Returns a unique instance of PGPManager.
	 * @param path
	 * @return
	 * @throws PGPException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static PGPManager getInstance(String path) throws FileNotFoundException, IOException, PGPException {
		if (instance == null) {
			instance = new PGPManager(path);
		} 
		return instance;
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
		//TODO - convert keyID to a field
		long keyID = getSecretKey().getKeyID();
		
		PGPPublicKey pubKey = this.publicKeyRing.getPublicKey(keyID);
		logger.log(Level.FINE, "Returninig public key "+ Long.toHexString(keyID));
		return pubKey;
	}
	
	public String getArmoredPublicKeyRing() throws IOException, PGPException {
		return getArmoredPublicKeyRing(publicKeyRing.getPublicKeyRing(getPublicKey().getKeyID()));
	}
	
	public static String getArmoredPublicKeyRing(PGPPublicKeyRing pubKeyRing) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ArmoredOutputStream aos = new ArmoredOutputStream(baos);
		
		pubKeyRing.encode(aos);
		
		aos.close();
		baos.close();

		String armoredPubKeyRing = baos.toString();
		return armoredPubKeyRing;
	}
	
	/**
	 * Return the user's publicKey in ASCII armored.
	 * 
	 * @return
	 * @throws IOException
	 * @throws PGPException
	 */
	public String getArmoredPublicKey() throws IOException, PGPException {
		return getArmoredPublicKey(getPublicKey());
	}
	
	/**
	 * Return the passed publicKey in ASCII armored.
	 * 
	 * @param pubKey
	 * @return
	 * @throws IOException 
	 */
	public static String getArmoredPublicKey(PGPPublicKey pubKey) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ArmoredOutputStream aos = new ArmoredOutputStream(baos);
		
		pubKey.encode(aos);
		
		aos.close();
		baos.close();

		String armoredPubKey = baos.toString();
		return armoredPubKey;
	}

	/**
	 * Returns a PGPPublicKey for the givenn armoredPublicKey.
	 * 
	 * @param armoredPubKey
	 * @return
	 * @throws IOException 
	 */
	public static PGPPublicKey getPublicKey(String armoredPubKey) throws IOException {
		
		ByteArrayInputStream bais = new ByteArrayInputStream(armoredPubKey.getBytes());
		ArmoredInputStream ais = new ArmoredInputStream(bais);
		PGPObjectFactory	pgpFact		= new PGPObjectFactory(ais); 
		PGPPublicKeyRing	pgpPub		= (PGPPublicKeyRing)pgpFact.nextObject();
		PGPPublicKey 		pubKey		= pgpPub.getPublicKey();
		
		return pubKey;
	}

	public Iterator<PGPPublicKey> getPublicKeys() throws PGPException {
		Iterator<PGPPublicKey> pubKeys = this.publicKeyRing.getPublicKeyRing(getPublicKey().getKeyID()).getPublicKeys();
		logger.log(Level.FINE, "Returninig publickeys for user " + getUserID());
		return pubKeys;		
	}

	/**
	 * Recupera o ID do usu‡rio da primeira chave pœblica na pubring.
	 *  
	 * @return String
	 * @throws PGPException 
	 * @throws PGPException 
	 */
	public String getUserID() throws PGPException {
		return getUserID(getPublicKey());
	}
	
	/**
	 * Recupera o userID da chave pœblica fornecida.
	 * 
	 * @param pubKey
	 * @return
	 */
	public static String getUserID(PGPPublicKey pubKey) {

		String userID = (String) pubKey.getUserIDs().next();
		logger.log(Level.FINE,"userID: " + userID+", keyID: "+Long.toHexString(pubKey.getKeyID()));
		return userID;
	}
	
	/**
	 * Returns a list of PublicKeys that exists in the local publicKeyRing
	 * 
	 * @return List<PGPPUblicKeys>
	 * @throws PGPException 
	 */
	public Collection<PGPPublicKey> getTrustedPublicKeys() throws PGPException {
		
		Iterator<PGPPublicKeyRing>    rIt = this.publicKeyRing.getKeyRings();
		Collection<PGPPublicKey> trustedPubKeys = new ArrayList<PGPPublicKey>();

		long ownerKeyID	= getSecretKey().getKeyID();
		long trustedKeyID;
		
        while (rIt.hasNext()) {
            PGPPublicKey pubKey = (PGPPublicKey)rIt.next().getPublicKey();
            trustedKeyID = pubKey.getKeyID();
            
            String userID = getUserID(pubKey);
            String keyID = Long.toHexString(pubKey.getKeyID());
            
            if( trustedKeyID != ownerKeyID) {
            	trustedPubKeys.add(pubKey);
            	logger.log(Level.FINE,"Found trusted userID: " + userID +", keyID: "+ keyID);
            }
        }

		return trustedPubKeys;
	}
	
	/**
	 * Returns a list of the User's IDs that exists in the local publicKeyRing
	 */
	
	public Collection<String> getTrustedUserIDs() {
		Iterator<PGPPublicKeyRing>    rIt = this.publicKeyRing.getKeyRings();
		Collection<String> trustedUserIDs = new ArrayList<String>();

		long ownerKeyID	= getSecretKey().getKeyID();
		long trustedKeyID;
		
        while (rIt.hasNext()) {
            PGPPublicKey pubKey = (PGPPublicKey)rIt.next().getPublicKey();
            
            trustedKeyID = pubKey.getKeyID();
            String userID = getUserID(pubKey);
            String keyID = Long.toHexString(pubKey.getKeyID());
            
            if( trustedKeyID != ownerKeyID) {
            	trustedUserIDs.add(userID);
            	logger.log(Level.FINE,"Found trusted userID: " + userID +", keyID: "+ keyID);
            }
        }

		return trustedUserIDs;

	}
	
	/**
	 * Returns the PGPPublicKey for the given the user name or e-mail
	 * 
	 * @param userID
	 * @return PGPPublicKey
	 * @throws PGPException 
	 */
	public PGPPublicKey getPublicKeyByUserID(String userID) throws PGPException {

		Iterator<PGPPublicKeyRing> keyRings = this.publicKeyRing.getKeyRings(userID, true);
		
		PGPPublicKey pubKey;
		if (keyRings.hasNext()) {
			pubKey = keyRings.next().getPublicKey();
			logger.log(Level.FINE, "Found key " + Long.toHexString(pubKey.getKeyID()) + " when searching for "+ userID);
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
				logger.log(Level.FINE, "Creating directory" + this.keyRingPath + ".");
			} else {
				logger.log(Level.FINE, "Directory already exist, using it.");
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
			nspe.printStackTrace();
		} catch (PGPException pe) {
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
			fnfe.printStackTrace();
		} catch (IOException ioe) {
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
			nsae.printStackTrace();
		} catch (NoSuchProviderException nspe) {
			nspe.printStackTrace();
		}
	
		dsaKeyPairGenerator.initialize(1024);
	
	    // gerando o par de chaves
	    KeyPair dsaKeyPair = dsaKeyPairGenerator.generateKeyPair();
	    PGPKeyPair pgpKeyPair = null;
		try {
			pgpKeyPair = new PGPKeyPair(PGPPublicKey.DSA, dsaKeyPair, new Date());
		} catch (PGPException pe) {
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
		logger.log(Level.FINE, "Returning private key "+ Long.toHexString(secKey.getKeyID()));
		return secKey;
	}
	
	public void savePublicKeyRing() throws IOException {
		OutputStream pubring = new FileOutputStream(this.pubringFilePath);
		this.publicKeyRing.encode(pubring);
	}


}
