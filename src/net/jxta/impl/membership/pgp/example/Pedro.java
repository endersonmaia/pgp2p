package net.jxta.impl.membership.pgp.example;

import net.jxta.platform.NetworkManager;

public class Pedro extends RendezvousPeer {

	public static final int tcpPort = 9722;
	public static final String name = "Pedro";
	public static NetworkManager networkManager;

	public Pedro() {
		super(name, tcpPort);
	}
	
	public static void main(String[] args) {
		RendezvousPeer rdvPedro = new Pedro();
		rdvPedro.start(rdvPedro);
	}
		
}
