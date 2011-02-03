package net.pgp2p.test;
import net.pgp2p.networkhandler.ADHOCPeer;

public class PedroPeer  {
  public static void main(String[] args) throws Exception {
    new ADHOCPeer("pedro", true).start();
  }
}
