package net.pgp2p.test;
import net.pgp2p.networkhandler.ADHOCPeer;

public class JosePeer  {
  public static void main(String[] args) throws Exception {
    new ADHOCPeer("jose", true).start();
  }
}
