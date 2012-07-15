package org.oxymores.chronix.core.active;

import javax.jms.JMSException;

import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunDescription;
import org.oxymores.chronix.engine.Runner;

public class ShellParameter extends Parameter {

	private static final long serialVersionUID = 7528888158440570804L;

	@Override
	public void resolveValue(ChronixContext ctx, Runner sender, PipelineJob pj) {
		RunDescription rd = new RunDescription();
		rd.command = this.value;
		rd.Method = "Shell";
		rd.helperExecRequest = true;
		
		try {
			sender.getParameterValue(rd, pj, this.getId());
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
