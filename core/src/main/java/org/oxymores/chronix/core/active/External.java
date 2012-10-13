package org.oxymores.chronix.core.active;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oxymores.chronix.core.ActiveNodeBase;

public class External extends ActiveNodeBase {

	private static final long serialVersionUID = 3722102490089534147L;

	public String machineRestriction, accountRestriction;
	public String regularExpression;

	public String getRegularExpression() {
		return regularExpression;
	}

	public void setRegularExpression(String regularExpression) {
		this.regularExpression = regularExpression;
	}

	public String getCalendarString(String data) {
		if (regularExpression == null || regularExpression == "")
			return null;

		Pattern p = Pattern.compile(regularExpression);
		Matcher m = p.matcher(data);

		if (m.find())
			return m.group(1);
		else
			return null;
	}

}
