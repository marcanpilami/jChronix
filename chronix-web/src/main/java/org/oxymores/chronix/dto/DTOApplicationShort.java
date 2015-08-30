package org.oxymores.chronix.dto;

import java.util.Date;

public class DTOApplicationShort
{
    private String id, name, description, latestVersionComment;
    private int version;
    private boolean draft = false;
    private Date latestSave;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getVersion()
    {
        return this.version;
    }

    public void setVersion(int v)
    {
        this.version = v;
    }

    public boolean isDraft()
    {
        return draft;
    }

    public void setDraft(boolean draft)
    {
        this.draft = draft;
    }

    public Date getLatestSave()
    {
        return latestSave;
    }

    public void setLatestSave(Date latestSave)
    {
        this.latestSave = latestSave;
    }

    public String getLatestVersionComment()
    {
        return latestVersionComment;
    }

    public void setLatestVersionComment(String latestVersionComment)
    {
        this.latestVersionComment = latestVersionComment;
    }
}
