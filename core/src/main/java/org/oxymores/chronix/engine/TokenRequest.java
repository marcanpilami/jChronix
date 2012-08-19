package org.oxymores.chronix.engine;

import java.io.Serializable;
import java.util.UUID;

import org.joda.time.DateTime;

public class TokenRequest implements Serializable {
	private static final long serialVersionUID = -7250533108215592799L;

	public enum TokenRequestType {
		REQUEST, RELEASE, RENEW, AGREE
	}

	public TokenRequestType type;
	public UUID applicationID;
	public UUID tokenID;
	public UUID placeID;
	public UUID stateID;
	public UUID requestingNodeID;
	public UUID pipelineJobID;
	public DateTime requestedAt;

	public boolean local = true;
}
