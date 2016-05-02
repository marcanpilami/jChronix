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
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.joda.time.DateTime;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.app.Token;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.engine.data.TokenRequest;
import org.oxymores.chronix.engine.data.TokenRequest.TokenRequestType;
import org.sql2o.Connection;

public class TokenReservation implements Serializable
{
    private static final long serialVersionUID = 4126830397920944723L;
    protected static final int UUID_LENGTH = 36;

    private Long id;

    @NotNull
    private UUID applicationId;

    @NotNull
    private UUID placeId;

    @NotNull
    private UUID stateId;

    @NotNull
    private UUID tokenId;

    @NotNull
    private UUID pipelineJobId;

    // ExecutionNodeId
    @NotNull
    private UUID requestedBy;

    private DateTime requestedOn;
    private DateTime grantedOn;
    private DateTime renewedOn;

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
        tr.applicationID = this.applicationId;
        tr.placeID = this.placeId;
        tr.tokenID = this.tokenId;
        tr.stateID = this.stateId;
        tr.requestedAt = new DateTime(this.grantedOn);
        tr.requestingNodeID = this.requestedBy;
        tr.pipelineJobID = this.pipelineJobId;
        tr.local = false;
        return tr;
    }

    //
    // ///////////////////////////////////////////
    // ///////////////////////////////////////////
    // Stupid get/set
    public UUID getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId)
    {
        this.applicationId = applicationId;
    }

    public UUID getPlaceId()
    {
        return placeId;
    }

    public void setPlaceId(UUID placeId)
    {
        this.placeId = placeId;
    }

    public UUID getStateId()
    {
        return stateId;
    }

    public void setStateId(UUID stateId)
    {
        this.stateId = stateId;
    }

    public UUID getTokenId()
    {
        return tokenId;
    }

    public void setTokenId(UUID tokenId)
    {
        this.tokenId = tokenId;
    }

    public UUID getPipelineJobId()
    {
        return pipelineJobId;
    }

    public void setPipelineJobId(UUID pipelineJobId)
    {
        this.pipelineJobId = pipelineJobId;
    }

    public UUID getRequestedBy()
    {
        return requestedBy;
    }

    public void setRequestedBy(UUID requestedBy)
    {
        this.requestedBy = requestedBy;
    }

    public DateTime getRequestedOn()
    {
        return requestedOn;
    }

    public void setRequestedOn(DateTime requestedOn)
    {
        this.requestedOn = requestedOn;
    }

    public DateTime getGrantedOn()
    {
        return grantedOn;
    }

    public void setGrantedOn(DateTime grantedOn)
    {
        this.grantedOn = grantedOn;
    }

    public DateTime getRenewedOn()
    {
        return renewedOn;
    }

    public void setRenewedOn(DateTime renewedOn)
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
    public Application getApplication(ChronixContextMeta ctx)
    {
        return ctx.getApplication(this.applicationId);
    }

    public Place getPlace(ChronixContextMeta ctx)
    {
        return ctx.getEnvironment().getPlace(this.placeId);
    }

    public State getState(ChronixContextMeta ctx)
    {
        return this.getApplication(ctx).getState(this.stateId);
    }

    public Token getToken(ChronixContextMeta ctx)
    {
        return this.getApplication(ctx).getToken(this.stateId);
    }

    public Long getId()
    {
        return this.id;
    }

    public void insertOrUpdate(Connection conn)
    {
        int i = conn.createQuery("UPDATE TokenReservation SET grantedOn=:grantedOn, pending=:pending, renewedOn=:renewedOn WHERE id=:id")
                .bind(this).executeUpdate().getResult();
        if (i == 0)
        {
            conn.createQuery("INSERT INTO TokenReservation(applicationId, grantedOn, localRenew, pending, pipelineJobId, placeId, "
                    + "renewedOn, requestedBy, requestedOn, stateId, tokenId) "
                    + "VALUES(:applicationId, :grantedOn, :localRenew, :pending, :pipelineJobId, :placeId, :renewedOn, "
                    + ":requestedBy, :requestedOn, :stateId, :tokenId)").bind(this).executeUpdate();
        }
    }

    public void delete(Connection conn)
    {
        conn.createQuery("DELETE FROM TokenReservation WHERE id=:id").addParameter("id", this.getId()).executeUpdate();
    }
}
