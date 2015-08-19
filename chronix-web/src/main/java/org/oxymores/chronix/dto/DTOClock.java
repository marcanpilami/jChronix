package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DTOClock
{
    private String name;
    private String description;
    private String id;

    private List<Date> nextOccurrences = new ArrayList<Date>();
    private List<String> rulesADD = new ArrayList<String>(), rulesEXC = new ArrayList<String>();

    public void addNo(Date d)
    {
        this.nextOccurrences.add(d);
    }

    public void addRuleAdd(UUID id)
    {
        this.rulesADD.add(id.toString());
    }

    public void addRuleExc(UUID id)
    {
        this.rulesADD.add(id.toString());
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public List<Date> getNextOccurrences()
    {
        return nextOccurrences;
    }

    void setNextOccurrences(List<Date> nextOccurrences)
    {
        this.nextOccurrences = nextOccurrences;
    }

    public List<String> getRulesADD()
    {
        return rulesADD;
    }

    void setRulesADD(List<String> rulesADD)
    {
        this.rulesADD = rulesADD;
    }

    public List<String> getRulesEXC()
    {
        return rulesEXC;
    }

    void setRulesEXC(List<String> rulesEXC)
    {
        this.rulesEXC = rulesEXC;
    }
}
