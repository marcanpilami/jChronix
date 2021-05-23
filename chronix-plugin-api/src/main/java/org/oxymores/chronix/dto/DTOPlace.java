package org.oxymores.chronix.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DTOPlace implements Serializable
{
    private static final long serialVersionUID = -1920825063526366141L;

    private UUID id;
    private String name, prop1, prop2, prop3, prop4;
    private List<UUID> memberOf = new ArrayList<>();
    private UUID nodeid;

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    public DTOPlace()
    {
        // JB convention
    }

    public DTOPlace(String name, UUID onExecutionNodeId, UUID id)
    {
        if (id == null)
        {
            this.id = UUID.randomUUID();
        }
        else
        {
            this.id = id;
        }
        this.name = name;
        this.nodeid = onExecutionNodeId;
    }

    public DTOPlace(String name, DTOExecutionNode onNode, UUID id)
    {
        this(name, onNode.getId(), id);
    }

    public DTOPlace(String name, DTOExecutionNode onNode)
    {
        this(name, onNode.getId(), null);
    }

    public DTOPlace(String name, UUID onExecutionNodeId, String prop1, String prop2, String prop3, String prop4, UUID id)
    {
        this(name, onExecutionNodeId, id);
        this.prop1 = prop1;
        this.prop2 = prop2;
        this.prop3 = prop3;
        this.prop4 = prop4;
    }

    public DTOPlace(String name, DTOExecutionNode onNode, String prop1, String prop2, String prop3, String prop4, UUID id)
    {
        this(name, onNode.getId(), prop1, prop2, prop3, prop4, id);
    }

    public DTOPlace(String name, DTOExecutionNode onNode, String prop1, String prop2, String prop3, String prop4)
    {
        this(name, onNode, prop1, prop2, prop3, prop4, null);
    }

    public DTOPlace(String name, UUID onExecutionNodeId, String prop1, String prop2, String prop3, String prop4)
    {
        this(name, onExecutionNodeId, prop1, prop2, prop3, prop4, null);
    }

    // Add to lists
    public DTOPlace addMemberOfGroup(UUID id)
    {
        this.memberOf.add(id);
        return this;
    }

    public DTOPlace addMemberOfGroup(DTOPlaceGroup... pgs)
    {
        for (DTOPlaceGroup pg : pgs)
        {
            this.addMemberOfGroup(pg.getId());
        }
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stupid GET/SET
    ///////////////////////////////////////////////////////////////////////////

    public UUID getId()
    {
        return id;
    }

    protected void setId(UUID id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    protected void setName(String name)
    {
        this.name = name;
    }

    public String getProp1()
    {
        return prop1;
    }

    protected void setProp1(String prop1)
    {
        this.prop1 = prop1;
    }

    public String getProp2()
    {
        return prop2;
    }

    protected void setProp2(String prop2)
    {
        this.prop2 = prop2;
    }

    public String getProp3()
    {
        return prop3;
    }

    protected void setProp3(String prop3)
    {
        this.prop3 = prop3;
    }

    public String getProp4()
    {
        return prop4;
    }

    protected void setProp4(String prop4)
    {
        this.prop4 = prop4;
    }

    public List<UUID> getMemberOf()
    {
        return memberOf;
    }

    void setMemberOf(List<UUID> memberOf)
    {
        this.memberOf = memberOf;
    }

    public UUID getNodeid()
    {
        return nodeid;
    }

    public void setNodeid(UUID nodeid)
    {
        this.nodeid = nodeid;
    }
}
