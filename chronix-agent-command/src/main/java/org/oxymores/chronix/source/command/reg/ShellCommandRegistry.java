package org.oxymores.chronix.source.command.reg;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;
import org.oxymores.chronix.source.command.dto.ShellCommand;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

@Component(immediate = false, service = EventSourceProvider.class)
public class ShellCommandRegistry extends EventSourceProvider
{
    @Override
    public String getSourceName()
    {
        return "Shell command";
    }

    @Override
    public String getSourceDescription()
    {
        return "a command that can be run against a variety of Unix and Windows shells";
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry reg)
    {
        if (!sourceFile.isDirectory() || sourceFile.list().length == 0)
        {
            return;
        }
        File file = new File(sourceFile.getAbsolutePath() + "/commands.xml");
        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(ShellCommandRegistry.class.getClassLoader());

        @SuppressWarnings("unchecked")
        List<ShellCommand> res = (List<ShellCommand>) xmlUtility.fromXML(file);
        for (ShellCommand c : res)
        {
            reg.registerSource(c);
        }
    }

    @Override
    public void serialise(File targetFile, Collection<? extends EventSource> instances)
    {
        List<ShellCommand> sources = new ArrayList<>();
        for (EventSource d : instances)
        {
            sources.add((ShellCommand) d);
        }
        XStream xmlUtility = new XStream(new StaxDriver());
        File target = new File(targetFile.getAbsolutePath() + "/commands.xml");

        try (FileOutputStream fos = new FileOutputStream(target))
        {
            xmlUtility.toXML(sources, fos);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not save commands to file", e);
        }
    }
}
