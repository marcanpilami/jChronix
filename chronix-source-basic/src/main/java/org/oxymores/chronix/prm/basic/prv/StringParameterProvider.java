package org.oxymores.chronix.prm.basic.prv;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.prm.ParameterProvider;
import org.oxymores.chronix.api.prm.ParameterResolutionRequest;
import org.oxymores.chronix.api.source.EventSourceField;

@Component
public class StringParameterProvider implements ParameterProvider
{
    @Override
    public String getName()
    {
        return "static string";
    }

    @Override
    public String getDescription()
    {
        return "a simple string stored in the plan definition. Test only.";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        List<EventSourceField> res = new ArrayList<>();
        res.add(new EventSourceField("value", "the value to use", null, true));
        return res;
    }

    @Override
    public String getValue(ParameterResolutionRequest job)
    {
        return job.getFields().get("value");
    }

}
