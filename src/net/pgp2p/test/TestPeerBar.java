package net.pgp2p.test;


import net.pgp2p.networkhandler.ADHOCPeer;

public class TestPeerBar {
	public static void main(String[] args) {
		try {
			new ADHOCPeer("PeerBar", true).start().sendVerifyRequest("paty", msg)("PeerFoo");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
