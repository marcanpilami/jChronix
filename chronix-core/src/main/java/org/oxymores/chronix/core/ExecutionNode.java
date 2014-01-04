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

package org.oxymores.chronix.core;

import java.util.ArrayList;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Range;
import org.oxymores.chronix.core.validation.ExecutionNodeIsolation;

@ExecutionNodeIsolation
public class ExecutionNode extends ApplicationObject
{
    private static final long serialVersionUID = 2115315700815310189L;

    protected NodeType type;
    protected boolean console;

    protected String sshKeyFilePath;
    protected String sslKeyFilePath;

    protected String dns, osusername, ospassword;
    protected Integer qPort, wsPort, remoteExecPort, jmxPort;

    protected ArrayList<NodeLink> canSendTo, canReceiveFrom;
    protected ArrayList<Place> placesHosted;

    @NotNull
    @Range(min = 0, max = 100000)
    protected Integer x, y;

    public ExecutionNode()
    {
        super();
        canSendTo = new ArrayList<NodeLink>();
        canReceiveFrom = new ArrayList<NodeLink>();
        placesHosted = new ArrayList<Place>();
        jmxPort = 1788;
        this.wsPort = 1790;
        this.remoteExecPort = 1789;
    }

    @Override
    public String toString()
    {
        return String.format("%s:%s (%s)", dns, qPort, console ? "console" : "not console");
    }

    public NodeType getType()
    {
        return type;
    }

    public void setType(NodeType type)
    {
        this.type = type;
    }

    public String getSshKeyFilePath()
    {
        return sshKeyFilePath;
    }

    public void setSshKeyFilePath(String sshKeyFilePath)
    {
        this.sshKeyFilePath = sshKeyFilePath;
    }

    public String getSslKeyFilePath()
    {
        return sslKeyFilePath;
    }

    public void setSslKeyFilePath(String sslKeyFilePath)
    {
        this.sslKeyFilePath = sslKeyFilePath;
    }

    public String getDns()
    {
        return dns;
    }

    public void setDns(String dns)
    {
        this.dns = dns;
    }

    public String getOsusername()
    {
        return osusername;
    }

    public void setOsusername(String osusername)
    {
        this.osusername = osusername;
    }

    public String getOspassword()
    {
        return ospassword;
    }

    public void setOspassword(String ospassword)
    {
        this.ospassword = ospassword;
    }

    public Integer getqPort()
    {
        return qPort;
    }

    public void setqPort(Integer qPort)
    {
        this.qPort = qPort;
    }

    public Integer getWsPort()
    {
        return wsPort;
    }

    public void setWsPort(Integer wsPort)
    {
        this.wsPort = wsPort;
    }

    public Integer getJmxPort()
    {
        return this.jmxPort;
    }

    public void setJmxPort(Integer port)
    {
        this.jmxPort = port;
    }

    public Integer getRemoteExecPort()
    {
        return remoteExecPort;
    }

    public void setRemoteExecPort(Integer remoteExecPort)
    {
        this.remoteExecPort = remoteExecPort;
    }

    public ArrayList<NodeLink> getCanSendTo()
    {
        return canSendTo;
    }

    public ArrayList<NodeLink> getCanReceiveFrom()
    {
        return canReceiveFrom;
    }

    public ArrayList<Place> getPlacesHosted()
    {
        return placesHosted;
    }

    public void addCanSendTo(NodeLink nl)
    {
        if (!this.canSendTo.contains(nl))
        {
            this.canSendTo.add(nl);
            nl.setNodeFrom(this);
        }
    }

    public void addCanReceiveFrom(NodeLink nl)
    {
        if (!this.canReceiveFrom.contains(nl))
        {
            this.canReceiveFrom.add(nl);
            nl.setNodeTo(this);
        }
    }

    public Integer getX()
    {
        return x;
    }

    public void setX(Integer x)
    {
        this.x = x;
    }

    public Integer getY()
    {
        return y;
    }

    public void setY(Integer y)
    {
        this.y = y;
    }

    public void addHostedPlace(Place place)
    {
        if (!this.placesHosted.contains(place))
        {
            this.placesHosted.add(place);
            place.setNode(this);
        }
    }

    public String getBrokerName()
    {
        return (this.dns + this.qPort).toUpperCase();
    }

    public String getBrokerUrl()
    {
        return (this.dns + ":" + this.qPort).toUpperCase();
    }

    public Boolean isHosted()
    {
        for (NodeLink nl : this.canReceiveFrom)
        {
            if (nl.getMethod() == NodeConnectionMethod.RCTRL)
                return true;
        }
        return false;
    }

    public ExecutionNode getHost()
    {
        for (NodeLink nl : this.canReceiveFrom)
        {
            if (nl.getMethod() == NodeConnectionMethod.RCTRL)
                return nl.nodeFrom;
        }
        return this;
    }

    public void connectTo(ExecutionNode target, NodeConnectionMethod method)
    {
        NodeLink l1 = new NodeLink();
        l1.setMethod(method);
        l1.setNodeFrom(this);
        l1.setNodeTo(target);
    }

    public boolean isConsole()
    {
        return console;
    }

    public void setConsole(boolean console)
    {
        this.console = console;
    }
}
