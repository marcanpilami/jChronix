package org.oxymores.chronix.dto;

public class DTOValidationError
{
    private String itemIdentification;
    private String errorPath;
    private String errorMessage;
    private String erroneousValue;
    private String itemType;

    public String getItemIdentification()
    {
        return itemIdentification;
    }

    public void setItemIdentification(String itemIdentification)
    {
        this.itemIdentification = itemIdentification;
    }

    public String getErrorPath()
    {
        return errorPath;
    }

    public void setErrorPath(String errorPath)
    {
        this.errorPath = errorPath;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getErroneousValue()
    {
        return erroneousValue;
    }

    public void setErroneousValue(String erroneousValue)
    {
        this.erroneousValue = erroneousValue;
    }

    public String getItemType()
    {
        return itemType;
    }

    public void setItemType(String itemType)
    {
        this.itemType = itemType;
    }
}
