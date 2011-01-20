package net.jxta.hdam;

import Peer;
import junit.framework.TestCase;

public class PeerTest extends TestCase {
	
	Peer peerOne;
	Peer peerTwo;
	
	public void setUp() {
		
		peerOne = new Peer("peerOne");
		peerTwo = new Peer("peerTwo");
	}

	public void testPeerShouldHaveAName() {
		assertEquals(peerOne.getName(), "peerOne");
	}
	
	public void testPeerShouldSendAMessage() {
		Object message = new String("My Message");
		peerOne.sendMessage(message, peerTwo);
		
		assertEquals(message, peerTwo.getLastMessage());
	}
	
}
