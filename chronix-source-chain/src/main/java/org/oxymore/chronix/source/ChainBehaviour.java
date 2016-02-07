package org.oxymore.chronix.source;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymore.chronix.chain.dto.DTOChain;
import org.oxymores.chronix.core.source.api.DTO;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.JobDescription;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

@Component(immediate = true, service = EventSourceBehaviour.class)
public class ChainBehaviour extends EventSourceBehaviour
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
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        // A chain only starts its own start event source. The actual RunResult is sent by the end source, so all we need here is to return
        // at once.
        return null;
    }

    @Override
    public List<Class<? extends DTO>> getExposedDtoClasses()
    {
        List<Class<? extends DTO>> res = new ArrayList<>();
        res.add(DTOChain.class);
        return res;
    }

    @Override
    public void serialize(File targetFile, Collection<? extends DTO> instances)
    {
        List<DTOChain> chains = new ArrayList<>();
        for (DTO d : instances)
        {
            chains.add((DTOChain) d);
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
    public void deserialize(File sourceFile, EngineCallback cb)
    {
        if (!sourceFile.isDirectory() || sourceFile.list().length == 0)
        {
            return;
        }
        File file = new File(sourceFile.getAbsolutePath() + "/chains.xml");
        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(ChainBehaviour.class.getClassLoader());

        List<DTOChain> res = (List<DTOChain>) xmlUtility.fromXML(file);
        for (DTOChain c : res)
        {
            cb.registerSource(c);
        }
    }

}
