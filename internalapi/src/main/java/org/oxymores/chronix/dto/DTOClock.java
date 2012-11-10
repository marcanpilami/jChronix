package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.Date;

public class DTOClock {
	public String name;
	public String description;
	public String id;
	
	public ArrayList<Date> nextOccurrences;
	public ArrayList<String> rulesADD, rulesEXC;
}
