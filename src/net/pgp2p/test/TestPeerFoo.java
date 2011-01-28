package net.pgp2p.test;


import net.pgp2p.networkhandler.ADHOCPeer;

public class TestPeerFoo  {

	public static void main(String[] args) {
		new ADHOCPeer("PeerFoo", true).start();
	}

	
}
