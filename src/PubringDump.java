import java.io.*;

import java.security.Security;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;

import org.bouncycastle.util.encoders.Hex;

/**
 * Basic class which just lists the contents of the public key file passed
 * as an argument. If the file contains more than one "key ring" they are
 * listed in the order found.
 */
public class PubringDump 
{
    public static String getAlgorithm(
        int    algId)
    {
        switch (algId)
        {
        case PublicKeyAlgorithmTags.RSA_GENERAL:
            return "RSA_GENERAL";
        case PublicKeyAlgorithmTags.RSA_ENCRYPT:
            return "RSA_ENCRYPT";
        case PublicKeyAlgorithmTags.RSA_SIGN:
            return "RSA_SIGN";
        case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT:
            return "ELGAMAL_ENCRYPT";
        case PublicKeyAlgorithmTags.DSA:
            return "DSA";
        case PublicKeyAlgorithmTags.EC:
            return "EC";
        case PublicKeyAlgorithmTags.ECDSA:
            return "ECDSA";
        case PublicKeyAlgorithmTags.ELGAMAL_GENERAL:
            return "ELGAMAL_GENERAL";
        case PublicKeyAlgorithmTags.DIFFIE_HELLMAN:
            return "DIFFIE_HELLMAN";
        }

        return "unknown";
    }

    public static void main(String[] args)
        throws Exception
    {
        Security.addProvider(new BouncyCastleProvider());
        
        PGPUtil.setDefaultProvider("BC");

        //
        // Read the public key rings
        //
        PGPPublicKeyRingCollection    pubRings = new PGPPublicKeyRingCollection(
            PGPUtil.getDecoderStream(new FileInputStream("sandbox" + System.getProperty("file.separator") +
            												"gnupg-test" + System.getProperty("file.separator") +
            												"paty" + System.getProperty("file.separator") + 
            												"pubring.gpg")));

        Iterator    rIt = pubRings.getKeyRings();
            
        while (rIt.hasNext())
        {
            PGPPublicKeyRing    pgpPub = (PGPPublicKeyRing)rIt.next();

            try
            {
                pgpPub.getPublicKey();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                continue;
            }

            Iterator    it = pgpPub.getPublicKeys();
            while (it.hasNext())
            {
                PGPPublicKey    pgpKey = (PGPPublicKey)it.next();

                if (pgpKey.isMasterKey()) {
                	
                    // Exibe o ID do usuário
                	System.out.println( "== CHAVE PÚBLICA ==");
                  	System.out.println("User : " + pgpKey.getUserIDs().next());
                    System.out.print("Key ID: " + Long.toHexString(pgpKey.getKeyID()));
                } else {
                    System.out.print("Key ID: " + Long.toHexString(pgpKey.getKeyID()) + " (subkey)");
                }
                
                if (pgpKey.isEncryptionKey())
                	System.out.println(" (encription)");
                else
                	System.out.println("");
                
                // Mostra assinaturas
                Iterator ass = pgpKey.getSignatures();
                while(ass.hasNext())
                {
                	PGPSignature sig = (PGPSignature) ass.next();
                	if (sig.getKeyID() != pgpKey.getKeyID()) {
                		String assinante = "";
                		PGPSignature sign;
                		if (pgpPub.getPublicKey(sig.getKeyID()) != null) {
                            // Lista os assinantes
                            Iterator i = pgpPub.getPublicKey(sig.getKeyID()).getSignatures();
                            while(i.hasNext()) 
                            {
                            	sign = (PGPSignature) i.next();
                            	assinante = assinante + ", " + Long.toHexString(sign.getKeyID());
                            }
                		} else {
                			assinante = "null";
                		}
                    	System.out.println("            Assinado por: " + assinante);                		
                	}

                }
                
                System.out.println("            Algorithm: " + getAlgorithm(pgpKey.getAlgorithm()));
                System.out.println("            Fingerprint: " + new String(Hex.encode(pgpKey.getFingerprint())));
            }
            
            System.out.println("");
        }
    }
}