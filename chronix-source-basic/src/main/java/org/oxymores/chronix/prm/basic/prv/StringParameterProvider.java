package org.oxymores.chronix.prm.basic.prv;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.exception.SerializationException;
import org.oxymores.chronix.api.prm.Parameter;
import org.oxymores.chronix.api.prm.ParameterProvider;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

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
        return "a simple string stored in the plan definition";
    }

    @Override
    public void serialise(File targetFile, Collection<? extends Parameter> instances)
    {
        List<Parameter> prms = new ArrayList<>();
        prms.addAll(instances);

        XStream xmlUtility = new XStream(new StaxDriver());
        File target = new File(targetFile.getAbsolutePath() + "/" + this.getClass().getName() + ".xml");

        try (FileOutputStream fos = new FileOutputStream(target))
        {
            xmlUtility.toXML(prms, fos);
        }
        catch (Exception e)
        {
            throw new SerializationException("Could not save chains to file", e);
        }
    }

    @Override
    public Set<Parameter> deserialise(File sourceFile)
    {
        Set<Parameter> res = new HashSet<>();
        if (!sourceFile.isDirectory() || sourceFile.list().length == 0)
        {
            return res;
        }
        File source = new File(sourceFile.getAbsolutePath() + "/" + this.getClass().getName() + ".xml");
        XStream xmlUtility = new XStream(new StaxDriver());
        xmlUtility.setClassLoader(this.getClass().getClassLoader());

        @SuppressWarnings("unchecked")
        List<Parameter> g = (List<Parameter>) xmlUtility.fromXML(source);
        for (Parameter c : g)
        {
            res.add(c);
        }
        return res;
    }

}
