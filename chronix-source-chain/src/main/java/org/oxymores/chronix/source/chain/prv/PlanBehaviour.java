package org.oxymores.chronix.source.chain.prv;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;
import org.oxymores.chronix.source.chain.dto.Plan;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

@Component(immediate = true, service = EventSourceProvider.class)
public class PlanBehaviour extends EventSourceProvider
{
    @Override
    public String getSourceName()
    {
        return "plan";
    }

    @Override
    public String getSourceDescription()
    {
        return "the base container of a production plan";
    }

    @Override
    public List<Class<? extends EventSource>> getExposedDtoClasses()
    {
        List<Class<? extends EventSource>> res = new ArrayList<>();
        res.add(Plan.class);
        return res;
    }

    @Override
    public void serialise(File targetFile, Collection<? extends EventSource> instances)
    {
        List<Plan> chains = new ArrayList<>();
        for (EventSource d : instances)
        {
            chains.add((Plan) d);
        }
        XStream xmlUtility = new XStream(new StaxDriver());
        File target = new File(targetFile.getAbsolutePath() + "/plans.xml");

        try (FileOutputStream fos = new FileOutputStream(target))
        {
            xmlUtility.toXML(chains, fos);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not save plans to file", e);
        }
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry cb)
    {
        if (!sourceFile.isDirectory() || sourceFile.list().length == 0)
        {
            return;
        }
        File file = new File(sourceFile.getAbsolutePath() + "/plans.xml");
        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(PlanBehaviour.class.getClassLoader());

        List<Plan> res = (List<Plan>) xmlUtility.fromXML(file);
        for (Plan c : res)
        {
            cb.registerSource(c);
        }
    }

}
