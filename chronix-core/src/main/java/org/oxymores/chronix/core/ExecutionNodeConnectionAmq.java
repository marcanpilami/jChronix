package org.oxymores.chronix.core;

public class ExecutionNodeConnectionAmq extends ExecutionNodeConnection
{
    private String dns;
    private Integer qPort;

    public String getDns()
    {
        return dns;
    }

    public void setDns(String dns)
    {
        this.dns = dns;
    }

    public Integer getqPort()
    {
        return qPort;
    }

    public void setqPort(Integer qPort)
    {
        this.qPort = qPort;
    }
}
