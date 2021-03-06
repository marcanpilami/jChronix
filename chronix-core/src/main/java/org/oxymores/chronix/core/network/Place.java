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
package org.oxymores.chronix.core.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.oxymores.chronix.core.EnvironmentObject;
import org.oxymores.chronix.core.app.PlaceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Place extends EnvironmentObject
{
    private static final long serialVersionUID = 4736385443687921653L;
    private static final Logger log = LoggerFactory.getLogger(Place.class);

    protected String property1, property2, property3, property4;
    private List<UUID> memberOfIds = new ArrayList<>();

    private Object readResolve()
    {
        if (memberOfIds == null)
        {
            memberOfIds = new ArrayList<>();
        }
        return this;
    }

    @NotNull
    protected ExecutionNode node;

    public Place()
    {
        super();
    }

    public String getProperty1()
    {
        return property1;
    }

    public void setProperty1(String property1)
    {
        this.property1 = property1;
    }

    public String getProperty2()
    {
        return property2;
    }

    public void setProperty2(String property2)
    {
        this.property2 = property2;
    }

    public String getProperty3()
    {
        return property3;
    }

    public void setProperty3(String property3)
    {
        this.property3 = property3;
    }

    public String getProperty4()
    {
        return property4;
    }

    public void setProperty4(String property4)
    {
        this.property4 = property4;
    }

    public ExecutionNode getNode()
    {
        return node;
    }

    public void setNode(ExecutionNode node)
    {
        this.node = node;
        node.addHostedPlace(this);
    }

    public List<UUID> getMemberOfIds()
    {
        return new ArrayList<>(memberOfIds);
    }

    void setMemberOfIds(List<UUID> memberOfIds)
    {
        this.memberOfIds = memberOfIds;
    }

    public void addGroupMembership(PlaceGroup g)
    {
        if (!this.memberOfIds.contains(g.getId()))
        {
            this.memberOfIds.add(g.getId());
        }
    }

    public void addGroupMembership(UUID groupId)
    {
        if (!this.memberOfIds.contains(groupId))
        {
            this.memberOfIds.add(groupId);
        }
    }
}
