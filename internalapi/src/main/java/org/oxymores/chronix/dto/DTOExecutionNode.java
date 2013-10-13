package org.oxymores.chronix.dto;

import java.util.ArrayList;

public class DTOExecutionNode
{
	public String id, dns, osusername, ospassword, certFilePath;
	public boolean isConsole, isSimpleRunner;
	public int qPort, wsPort, remoteExecPort, jmxPort, x, y;
	public ArrayList<String> toTCP, toRCTRL, fromTCP, fromRCTRL;
	public ArrayList<String> places;

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

	public ArrayList<String> getToTCP()
	{
		return toTCP;
	}

	public void setToTCP(ArrayList<String> toTCP)
	{
		this.toTCP = toTCP;
	}

	public ArrayList<String> getToRCTRL()
	{
		return toRCTRL;
	}

	public void setToRCTRL(ArrayList<String> toRCTRL)
	{
		this.toRCTRL = toRCTRL;
	}

	public ArrayList<String> getFromTCP()
	{
		return fromTCP;
	}

	public void setFromTCP(ArrayList<String> fromTCP)
	{
		this.fromTCP = fromTCP;
	}

	public ArrayList<String> getFromRCTRL()
	{
		return fromRCTRL;
	}

	public void setFromRCTRL(ArrayList<String> fromRCTRL)
	{
		this.fromRCTRL = fromRCTRL;
	}

	public ArrayList<String> getPlaces()
	{
		return places;
	}

	public void setPlaces(ArrayList<String> places)
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

}
