package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DTOPlace
{
    private UUID id;
    private String name, prop1, prop2, prop3, prop4;
    private List<String> memberOf = new ArrayList<>();
    private String nodeid;

    // Add to lists
    public void addMemberOfGroup(UUID id)
    {
        this.memberOf.add(id.toString());
    }

    // Stupid GET/SET
    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getProp1()
    {
        return prop1;
    }

    public void setProp1(String prop1)
    {
        this.prop1 = prop1;
    }

    public String getProp2()
    {
        return prop2;
    }

    public void setProp2(String prop2)
    {
        this.prop2 = prop2;
    }

    public String getProp3()
    {
        return prop3;
    }

    public void setProp3(String prop3)
    {
        this.prop3 = prop3;
    }

    public String getProp4()
    {
        return prop4;
    }

    public void setProp4(String prop4)
    {
        this.prop4 = prop4;
    }

    public List<String> getMemberOf()
    {
        return memberOf;
    }

    void setMemberOf(List<String> memberOf)
    {
        this.memberOf = memberOf;
    }

    public String getNodeid()
    {
        return nodeid;
    }

    public void setNodeid(String nodeid)
    {
        this.nodeid = nodeid;
    }
}
