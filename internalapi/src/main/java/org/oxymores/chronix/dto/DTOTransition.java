package org.oxymores.chronix.dto;

public class DTOTransition {
	public String from, to;
	public Integer guard1;
	public String guard2, guard3;
	public String guard4;
	public String id;
	
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public Integer getGuard1() {
		return guard1;
	}
	public void setGuard1(Integer guard1) {
		this.guard1 = guard1;
	}
	public String getGuard2() {
		return guard2;
	}
	public void setGuard2(String guard2) {
		this.guard2 = guard2;
	}
	public String getGuard3() {
		return guard3;
	}
	public void setGuard3(String guard3) {
		this.guard3 = guard3;
	}
	public String getGuard4() {
		return guard4;
	}
	public void setGuard4(String guard4) {
		this.guard4 = guard4;
	}

}
