package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DTOExecutionNode
{
    private String id, dns, osusername, ospassword, certFilePath, name;
    private boolean isConsole, isSimpleRunner;
    private int qPort, wsPort, remoteExecPort, jmxPort, x, y;
    private List<String> toTCP = new ArrayList<String>(), toRCTRL = new ArrayList<String>(), fromTCP = new ArrayList<String>(),
            fromRCTRL = new ArrayList<String>();
    private List<String> places = new ArrayList<String>();

    // Add to list
    public void addToTcp(UUID id)
    {
        this.toTCP.add(id.toString());
    }

    public void addFromTcp(UUID id)
    {
        this.fromTCP.add(id.toString());
    }

    public void addToRctrl(UUID id)
    {
        this.toRCTRL.add(id.toString());
    }

    public void addFromRctrl(UUID id)
    {
        this.fromRCTRL.add(id.toString());
    }

    public void addPlace(UUID id)
    {
        this.places.add(id.toString());
    }

    // Stupid GET/SET
    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
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

    public String getCertFilePath()
    {
        return certFilePath;
    }

    public void setCertFilePath(String certFilePath)
    {
        this.certFilePath = certFilePath;
    }

    public boolean isConsole()
    {
        return isConsole;
    }

    public void setConsole(boolean isConsole)
    {
        this.isConsole = isConsole;
    }

    public int getqPort()
    {
        return qPort;
    }

    public void setqPort(int qPort)
    {
        this.qPort = qPort;
    }

    public int getWsPort()
    {
        return wsPort;
    }

    public void setWsPort(int wsPort)
    {
        this.wsPort = wsPort;
    }

    public int getRemoteExecPort()
    {
        return remoteExecPort;
    }

    public void setRemoteExecPort(int remoteExecPort)
    {
        this.remoteExecPort = remoteExecPort;
    }

    public int getJmxPort()
    {
        return jmxPort;
    }

    public void setJmxPort(int jmxPort)
    {
        this.jmxPort = jmxPort;
    }

    public int getX()
    {
        return x;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public int getY()
    {
        return y;
    }

    public void setY(int y)
    {
        this.y = y;
    }

    public List<String> getToTCP()
    {
        return toTCP;
    }

    void setToTCP(List<String> toTCP)
    {
        this.toTCP = toTCP;
    }

    public List<String> getToRCTRL()
    {
        return toRCTRL;
    }

    void setToRCTRL(List<String> toRCTRL)
    {
        this.toRCTRL = toRCTRL;
    }

    public List<String> getFromTCP()
    {
        return fromTCP;
    }

    void setFromTCP(List<String> fromTCP)
    {
        this.fromTCP = fromTCP;
    }

    public List<String> getFromRCTRL()
    {
        return fromRCTRL;
    }

    void setFromRCTRL(List<String> fromRCTRL)
    {
        this.fromRCTRL = fromRCTRL;
    }

    public List<String> getPlaces()
    {
        return places;
    }

    void setPlaces(List<String> places)
    {
        this.places = places;
    }

    public boolean isSimpleRunner()
    {
        return isSimpleRunner;
    }

    public void setSimpleRunner(boolean isSimpleRunner)
    {
        this.isSimpleRunner = isSimpleRunner;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
