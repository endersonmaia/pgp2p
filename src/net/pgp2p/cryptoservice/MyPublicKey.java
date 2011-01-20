package net.pgp2p.cryptoservice;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.PublicKey;
import java.util.Date;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;

public class MyPublicKey extends PGPPublicKey implements Externalizable {

	public MyPublicKey(int algorithm, PublicKey pubKey, Date time)
			throws PGPException {
		super(PGPPublicKey.DSA, pubKey, new Date());
		
		
	}

	@Override
	public void readExternal(ObjectInput arg0) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeExternal(ObjectOutput arg0) throws IOException {
		// TODO Auto-generated method stub

	}

}
