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
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;
import org.oxymores.chronix.core.EnvironmentObject;
import org.oxymores.chronix.core.validation.ExecutionNodeIsolation;

@ExecutionNodeIsolation
public class ExecutionNode extends EnvironmentObject
{
    private static final long serialVersionUID = 2115315700815310189L;

    private Integer wsPort = 1790;
    private Integer jmxRegistryPort = 1788;
    private Integer jmxServerPort = 1789;

    private List<ExecutionNodeConnection> connectsTo;
    private List<ExecutionNodeConnection> connectionParameters;
    private List<Place> placesHosted; // TODO: still used?
    private ExecutionNode computingNode = null;

    @NotNull
    @Range(min = 0, max = 100000)
    private Integer x, y;

    public ExecutionNode()
    {
        super();
        connectsTo = new ArrayList<>();
        placesHosted = new ArrayList<>();
        connectionParameters = new ArrayList<>();
    }

    public void addHostedPlace(Place place)
    {
        if (!this.placesHosted.contains(place))
        {
            this.getPlacesHosted().add(place);
            place.setNode(this);
        }
    }

    public String getBrokerName()
    {
        return this.name.toUpperCase();
    }

    public Boolean isHosted()
    {
        return this.getComputingNode() == null;
    }

    public ExecutionNode getComputingNode()
    {
        return this.computingNode == null ? this : this.computingNode;
    }

    public <T> List<T> getConnectionParameters(Class<T> connectionType)
    {
        List<T> res = new ArrayList<>();
        for (ExecutionNodeConnection c : this.getConnectionParameters())
        {
            if (c.getClass().isAssignableFrom(connectionType))
            {
                res.add((T) c);
            }
        }
        return res;
    }

    public <T> List<T> getConnectsTo(Class<T> connectionType)
    {
        List<T> res = new ArrayList<>();
        for (ExecutionNodeConnection c : this.getConnectsTo())
        {
            if (c.getClass().isAssignableFrom(connectionType))
            {
                res.add((T) c);
            }
        }
        return res;
    }

    public <T> void connectTo(ExecutionNode target, Class<T> connectionType)
    {
        List<T> prms = target.getConnectionParameters(connectionType);
        if (prms.size() != 1)
        {
            throw new IllegalArgumentException(
                    "using the short form of connect requires the target to have one and only one connection method of the given type");
        }
        this.getConnectsTo().add((ExecutionNodeConnection) prms.get(0));
    }

    public List<ExecutionNode> getCanReceiveFrom()
    {
        List<ExecutionNode> res = new ArrayList<>();
        for (ExecutionNodeConnection local : this.connectionParameters)
        {
            for (ExecutionNode n : this.environment.getNodesList())
            {
                if (n == this)
                {
                    continue;
                }
                for (ExecutionNodeConnection conn : n.getConnectsTo())
                {
                    if (conn == local)
                    {
                        res.add(n);
                    }
                }
            }
        }
        return res;
    }

    public void connectTo(ExecutionNodeConnection target)
    {
        this.getConnectsTo().add(target);
    }

    public void addConnectionMethod(ExecutionNodeConnection conn)
    {
        this.connectionParameters.add(conn);
    }

    public Integer getJmxRegistryPort()
    {
        return jmxRegistryPort;
    }

    public void setJmxRegistryPort(Integer jmxRegistryPort)
    {
        this.jmxRegistryPort = jmxRegistryPort;
    }

    public Integer getJmxServerPort()
    {
        return jmxServerPort;
    }

    public void setJmxServerPort(Integer jmxServerPort)
    {
        this.jmxServerPort = jmxServerPort;
    }

    public List<ExecutionNodeConnection> getConnectsTo()
    {
        return connectsTo;
    }

    public void setConnectsTo(List<ExecutionNodeConnection> connectsTo)
    {
        this.connectsTo = connectsTo;
    }

    public List<ExecutionNodeConnection> getConnectionParameters()
    {
        return connectionParameters;
    }

    public void setConnectionParameters(List<ExecutionNodeConnection> connectionParameters)
    {
        this.connectionParameters = connectionParameters;
    }

    public void setPlacesHosted(List<Place> placesHosted)
    {
        this.placesHosted = placesHosted;
    }

    public void setComputingNode(ExecutionNode computingNode)
    {
        this.computingNode = computingNode;
    }

    public Integer getWsPort()
    {
        return wsPort;
    }

    public void setWsPort(Integer wsPort)
    {
        this.wsPort = wsPort;
    }

    public List<Place> getPlacesHosted()
    {
        return placesHosted;
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
}
