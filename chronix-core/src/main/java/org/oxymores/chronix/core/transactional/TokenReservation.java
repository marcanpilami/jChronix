/**
 * By Marc-Antoine Gouillart, 2012
 * 
 * See the NOTICE file distributed with this work for 
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file 
 * except in compliance with the License. You may obtain 
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.oxymores.chronix.core.transactional;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.joda.time.DateTime;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.engine.data.TokenRequest;
import org.oxymores.chronix.engine.data.TokenRequest.TokenRequestType;

@Entity
public class TokenReservation implements Serializable
{
    private static final long serialVersionUID = 4126830397920944723L;
    protected static final int UUID_LENGTH = 36;

    @Column(length = UUID_LENGTH)
    private String applicationId;

    @Column(length = UUID_LENGTH)
    private String placeId;

    @Column(length = UUID_LENGTH)
    private String stateId;

    @Column(length = UUID_LENGTH)
    private String tokenId;

    @Column(length = UUID_LENGTH)
    private String pipelineJobId;

    // ExecutionNodeId
    @Column(length = UUID_LENGTH)
    private String requestedBy;

    private Date requestedOn;
    private Date grantedOn;
    private Date renewedOn;

    private boolean localRenew = false;
    private boolean pending = false;

    // ///////////////////////////////////////////
    // Helpers to create tokens
    public TokenRequest getRenewalRequest()
    {
        TokenRequest tr = getRequest();
        tr.type = TokenRequestType.RENEW;
        return tr;
    }

    public TokenRequest getReleaseRequest()
    {
        TokenRequest tr = getRequest();
        tr.type = TokenRequestType.RELEASE;
        return tr;
    }

    public TokenRequest getAgreeRequest()
    {
        TokenRequest tr = getRequest();
        tr.type = TokenRequestType.AGREE;
        return tr;
    }

    private TokenRequest getRequest()
    {
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

    //
    // ///////////////////////////////////////////

    // ///////////////////////////////////////////
    // Stupid get/set
    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(String applicationId)
    {
        this.applicationId = applicationId;
    }

    public String getPlaceId()
    {
        return placeId;
    }

    public void setPlaceId(String placeId)
    {
        this.placeId = placeId;
    }

    public String getStateId()
    {
        return stateId;
    }

    public void setStateId(String stateId)
    {
        this.stateId = stateId;
    }

    public String getTokenId()
    {
        return tokenId;
    }

    public void setTokenId(String tokenId)
    {
        this.tokenId = tokenId;
    }

    public String getPipelineJobId()
    {
        return pipelineJobId;
    }

    public void setPipelineJobId(String pipelineJobId)
    {
        this.pipelineJobId = pipelineJobId;
    }

    public String getRequestedBy()
    {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy)
    {
        this.requestedBy = requestedBy;
    }

    public Date getRequestedOn()
    {
        return requestedOn;
    }

    public void setRequestedOn(Date requestedOn)
    {
        this.requestedOn = requestedOn;
    }

    public Date getGrantedOn()
    {
        return grantedOn;
    }

    public void setGrantedOn(Date grantedOn)
    {
        this.grantedOn = grantedOn;
    }

    public Date getRenewedOn()
    {
        return renewedOn;
    }

    public void setRenewedOn(Date renewedOn)
    {
        this.renewedOn = renewedOn;
    }

    public boolean isLocalRenew()
    {
        return localRenew;
    }

    public void setLocalRenew(boolean localRenew)
    {
        this.localRenew = localRenew;
    }

    public boolean isPending()
    {
        return pending;
    }

    public void setPending(boolean pending)
    {
        this.pending = pending;
    }

    //
    // ///////////////////////////////////////////

    public Application getApplication(ChronixContext ctx)
    {
        return ctx.getApplication(this.applicationId);
    }

    public Place getPlace(ChronixContext ctx)
    {
        return ctx.getNetwork().getPlace(UUID.fromString(this.placeId));
    }

    public State getState(ChronixContext ctx)
    {
        return this.getApplication(ctx).getState(UUID.fromString(this.stateId));
    }

    public Token getToken(ChronixContext ctx)
    {
        return this.getApplication(ctx).getToken(UUID.fromString(this.stateId));
    }
}
