package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.UUID;

public class DTOApplication {

	public String name;
	public String id; 

	public ArrayList<DTOPlace> places;
	public ArrayList<DTOPlaceGroup> groups;
	public ArrayList<DTOExecutionNode> nodes;
	public ArrayList<DTOShellCommand> elements;
	public ArrayList<DTOParameter> parameters;
	
	protected UUID marsu;
}
