package net.jxta.impl.membership.pgp.example;

import net.jxta.platform.NetworkManager;

public class Joao extends RendezvousPeer {


	public static final int tcpPort = 9723;
	public static final String name = "Joao";
	public static NetworkManager networkManager;

	public Joao() {
		super(name, tcpPort);
	}
	
	public static void main(String[] args) {
		RendezvousPeer rdvJoao = new Joao();
		rdvJoao.start(rdvJoao);
	}
}
