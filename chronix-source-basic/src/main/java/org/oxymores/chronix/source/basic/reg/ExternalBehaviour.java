package org.oxymores.chronix.source.basic.reg;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;
import org.oxymores.chronix.source.basic.dto.External;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

@Component(immediate = true, service = EventSourceProvider.class)
public class ExternalBehaviour extends EventSourceProvider
{
    @Override
    public String getSourceName()
    {
        return "external";
    }

    @Override
    public String getSourceDescription()
    {
        return "represents an incoming event from an external system that is provided through a command line call.";
    }

    @Override
    public void serialise(File targetFile, Collection<? extends EventSource> instances)
    {
        List<External> sources = new ArrayList<>();
        for (EventSource d : instances)
        {
            sources.add((External) d);
        }
        XStream xmlUtility = new XStream(new StaxDriver());
        File target = new File(targetFile.getAbsolutePath() + "/external.xml");

        try (FileOutputStream fos = new FileOutputStream(target))
        {
            xmlUtility.toXML(sources, fos);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not save externals to file", e);
        }
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry cb)
    {
        if (!sourceFile.isDirectory() || sourceFile.list().length == 0)
        {
            return;
        }
        File file = new File(sourceFile.getAbsolutePath() + "/external.xml");
        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(this.getClass().getClassLoader());

        @SuppressWarnings("unchecked")
        List<External> res = (List<External>) xmlUtility.fromXML(file);
        for (External c : res)
        {
            cb.registerSource(c);
        }
    }
}
