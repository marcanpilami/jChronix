package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.UUID;

import org.oxymores.chronix.core.ConfigNodeBase;
import org.oxymores.chronix.core.Parameter;

public class DTOApplication {

	protected String name;

	protected ArrayList<DTOPlace> places;
	protected ArrayList<DTOPlaceGroup> groups;
	protected ArrayList<DTOExecutionNode> nodes;
	protected ArrayList<ConfigNodeBase> elements;
	protected ArrayList<Parameter> parameters;
	
	protected UUID marsu;
}
