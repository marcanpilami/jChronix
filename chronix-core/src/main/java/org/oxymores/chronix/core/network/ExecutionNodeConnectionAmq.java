package org.oxymores.chronix.core.network;

public class ExecutionNodeConnectionAmq extends ExecutionNodeConnection
{
    private static final long serialVersionUID = 3903963749812501283L;

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
