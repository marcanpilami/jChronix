package org.oxymores.chronix.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ResOrder")
public class ResOrder
{
    private String operation;
    private boolean success = false;
    private String message = "";

    public ResOrder(String operation, boolean ok, String message)
    {
        this.operation = operation;
        this.success = ok;
        this.message = message;
    }

    @SuppressWarnings("unused")
    private ResOrder()
    {

    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}
