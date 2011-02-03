package net.pgp2p.test;
import net.pgp2p.networkhandler.ADHOCPeer;

public class CristinaPeer  {
  public static void main(String[] args) throws Exception {
    new ADHOCPeer("cristina", true).start();
  }
}
