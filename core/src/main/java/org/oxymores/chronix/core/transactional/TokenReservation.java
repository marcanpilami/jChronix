package org.oxymores.chronix.core.transactional;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Entity;

import org.joda.time.DateTime;
import org.oxymores.chronix.engine.TokenRequest;
import org.oxymores.chronix.engine.TokenRequest.TokenRequestType;

@Entity
public class TokenReservation implements Serializable {
	private static final long serialVersionUID = 4126830397920944723L;

	public String applicationId;
	public String placeId;
	public String stateId;
	public String tokenId;
	public String pipelineJobId;

	public String requestedBy; // ExecutionNodeId

	public Date requestedOn;
	public Date grantedOn;
	public Date renewedOn;
	public boolean localRenew = false;
	public boolean pending = false;

	public TokenRequest getRenewalRequest() {
		TokenRequest tr = getRequest();
		tr.type = TokenRequestType.RENEW;
		return tr;
	}

	public TokenRequest getReleaseRequest() {
		TokenRequest tr = getRequest();
		tr.type = TokenRequestType.RELEASE;
		return tr;
	}

	public TokenRequest getAgreeRequest() {
		TokenRequest tr = getRequest();
		tr.type = TokenRequestType.AGREE;
		return tr;
	}

	private TokenRequest getRequest() {
		TokenRequest tr = new TokenRequest();
		tr.applicationID = UUID.fromString(this.applicationId);
		tr.placeID = UUID.fromString(this.placeId);
		tr.tokenID = UUID.fromString(this.tokenId);
		tr.stateID = UUID.fromString(this.stateId);
		tr.requestedAt = new DateTime(this.grantedOn);
		tr.requestingNodeID = UUID.fromString(this.requestedBy);
		tr.pipelineJobID = UUID.fromString(this.pipelineJobId);
		tr.local = false;
		return tr;
	}
}
