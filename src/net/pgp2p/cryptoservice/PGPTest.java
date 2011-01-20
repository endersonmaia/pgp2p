package net.pgp2p.cryptoservice;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;


import net.pgp2p.signservice.PGPSigner;
import net.pgp2p.verifyservice.PGPVerify;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPUtil;
import org.osgi.framework.hooks.service.FindHook;

public class PGPTest {
	
	private static List<String> consultados = new ArrayList();
	private static boolean entrou = false;
	
	private static String findDir(PGPPublicKey pubKey, String basePath) {
		Iterator i = pubKey.getUserIDs();
		
		String userID = null;
		String username = null;
		while(i.hasNext()){
			userID = (String)i.next();
		}
		
		username = userID.substring(0, userID.indexOf(" "));
		return basePath + username;
	}
	
	/**
	 * Procura no destino alguém que conhece a origem.
	 * 
	 * @param PGPPublicKey origem
	 * @param PGPPublicKey destino
	 * @return boolean
	 * @throws PGPException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */

	private static boolean findWhoTrusts(
			PGPVerify origem, 
			PGPVerify destino) 
	throws	PGPException, 
			FileNotFoundException, 
			IOException {
		
				
		if ( destino.isTrusted(origem.manager.getPublicKey()) ) {
			System.out.println("OK: " + destino.manager.getUserID() + " TEM a chave pública de " + origem.manager.getUserID());
			return true;
		} else {
			System.out.println("ERRO: " + destino.manager.getUserID() + " NÃO TEM a chave pública de " + origem.manager.getUserID());
		}
			

		Iterator destinoTrusted = destino.manager.getTrustedPublicKeys().iterator();
		PGPVerify confiavel;
		String baseDir = "./sandbox/gnupg-test/";
		String dir;
		
		while (destinoTrusted.hasNext()) {
			dir = findDir((PGPPublicKey)destinoTrusted.next(), baseDir);
			PGPManager confiavelManager = new PGPManager(dir); 
			confiavel = new PGPVerify(confiavelManager);

			System.out.println("OK: vai buscar com " + confiavel.manager.getUserID());
			
			// Se algum confiável na origem já tem a chave do confiável ignora
			// para fazer a consulta na origem mesmo.
			if (origem.manager.getTrustedPublicKeys().contains(confiavel.manager.getPublicKey()) ) {
				System.out.println(confiavel.manager.getUserID() +" existe na raiz, ignorando.");
				continue;
			}
			
			//TODO - ignorar usuários já consultados
			// Marca a entrada na recursiviade
			if ( entrou ) {
				consultados.add(destino.manager.getUserID());
				if ( consultados.contains(confiavel.manager.getUserID()) ) {
					System.out.println(confiavel.manager.getUserID() +" já consultado, ignorando.");
					continue;
				}
			} else {
				entrou = true;
			}

			if (findWhoTrusts(origem, confiavel))
				return true;
			
		}

		return false;
	}
	
	private static void testVerify(String sOrigem, String sDestino) {
		try {
			PGPManager origemManager = new PGPManager("./sandbox/gnupg-test/" + sOrigem);
			PGPManager destinoManager = new PGPManager("./sandbox/gnupg-test/" + sDestino);
			
			PGPVerify origem = new PGPVerify(origemManager);
			PGPVerify destino= new PGPVerify(destinoManager);
			
			System.out.println(origem.manager.getUserID() + " tentando se autenticar com " + destino.manager.getUserID());
			if ( findWhoTrusts(origem, destino) ) {			
				System.out.println("OK");
			} else {
				System.out.println("ERROR");
			}					
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("FIM");
		}
	}
	
	private static void testSign() {
		
		try {
			PGPManager assinanteManager = new PGPManager("./sandbox/gnupg-test/bono");
			PGPManager assinadoManager = new PGPManager("./sandbox/gnupg-test/paty");
			
			PGPSigner signer = new PGPSigner(assinanteManager);
			
			signer.sign(assinadoManager.getPublicKey());

			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

	
	public static void main(String[] args) {
		
		//testVerify("maria", "antonio");
		//testSign();

	}

}
