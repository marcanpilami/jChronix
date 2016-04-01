package org.oxymores.chronix.source.chain.prv;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.EventSource;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRegistry;
import org.oxymores.chronix.source.chain.dto.Chain;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

@Component(immediate = true, service = EventSourceProvider.class)
public class ChainBehaviour extends EventSourceProvider
{
    @Override
    public String getSourceName()
    {
        return "chain";
    }

    @Override
    public String getSourceDescription()
    {
        return "a reusable piece of production plan";
    }

    @Override
    public List<Class<? extends EventSource>> getExposedDtoClasses()
    {
        List<Class<? extends EventSource>> res = new ArrayList<>();
        res.add(Chain.class);
        return res;
    }

    @Override
    public void serialise(File targetFile, Collection<? extends EventSource> instances)
    {
        List<Chain> chains = new ArrayList<>();
        for (EventSource d : instances)
        {
            chains.add((Chain) d);
        }
        XStream xmlUtility = new XStream(new StaxDriver());
        File target = new File(targetFile.getAbsolutePath() + "/chains.xml");

        try (FileOutputStream fos = new FileOutputStream(target))
        {
            xmlUtility.toXML(chains, fos);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not save chains to file", e);
        }
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry cb)
    {
        if (!sourceFile.isDirectory() || sourceFile.list().length == 0)
        {
            return;
        }
        File file = new File(sourceFile.getAbsolutePath() + "/chains.xml");
        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(ChainBehaviour.class.getClassLoader());

        List<Chain> res = (List<Chain>) xmlUtility.fromXML(file);
        for (Chain c : res)
        {
            cb.registerSource(c);
        }
    }

}
