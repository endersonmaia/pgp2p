package net.pgp2p.test;
import net.pgp2p.networkhandler.ADHOCPeer;

public class MariaPeer  {
  public static void main(String[] args) throws Exception {
    new ADHOCPeer("maria", true).start();
  }
}
