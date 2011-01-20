package net.jxta.impl.membership.pgp;

import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.Enumeration;
import java.util.logging.Logger;

import net.jxta.credential.AuthenticationCredential;
import net.jxta.credential.Credential;
import net.jxta.document.Advertisement;
import net.jxta.document.Element;
import net.jxta.exception.PeerGroupException;
import net.jxta.exception.ProtocolNotSupportedException;
import net.jxta.id.ID;
import net.jxta.impl.membership.passwd.PasswdMembershipService;
import net.jxta.membership.Authenticator;
import net.jxta.membership.MembershipService;
import net.jxta.peergroup.PeerGroup;
import net.jxta.platform.ModuleSpecID;
import net.jxta.service.Service;


/**
 * Provides a membership service bases on a PGP Web of Trust.
 * 
 * @author Enderson Maia <endersonmaia@gmail.com>
 *
 */
public class PGPMembershipService implements MembershipService {
	
    /**
     *  Log4J Logger
     */
    private static final Logger LOG = Logger.getLogger(PasswdMembershipService.class.getName());
    
    /**
     * Well known service specification identifier: pgp membership
     */
    public final static ModuleSpecID pgpMembershipSpecID = (ModuleSpecID) ID.create(
            URI.create(ID.URIEncodingName + ":" + ID.URNNamespace + ":uuid-DeadBeefDeafBabaFeedBabe000000050307"));

	@Override
	public void addPropertyChangeListener(PropertyChangeListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addPropertyChangeListener(String arg0,
			PropertyChangeListener arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public Authenticator apply(AuthenticationCredential arg0)
			throws PeerGroupException, ProtocolNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<Credential> getCurrentCredentials()
			throws PeerGroupException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Credential getDefaultCredential() throws PeerGroupException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Credential join(Authenticator arg0) throws PeerGroupException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Credential makeCredential(Element arg0) throws PeerGroupException,
			Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePropertyChangeListener(String arg0,
			PropertyChangeListener arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resign() throws PeerGroupException {
		// TODO Auto-generated method stub

	}

	@Override
	public Advertisement getImplAdvertisement() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Service getInterface() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(PeerGroup arg0, ID arg1, Advertisement arg2)
			throws PeerGroupException {
		// TODO Auto-generated method stub

	}

	@Override
	public int startApp(String[] arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void stopApp() {
		// TODO Auto-generated method stub

	}

}
