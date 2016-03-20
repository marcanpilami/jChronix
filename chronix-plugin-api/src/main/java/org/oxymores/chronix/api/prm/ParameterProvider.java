package org.oxymores.chronix.api.prm;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import org.oxymores.chronix.api.exception.ModelUpdateException;
import org.oxymores.chronix.api.exception.SerializationException;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;

public interface ParameterProvider
{
    ///////////////////////////////////////////////////////////////////////////
    // Identity Card
    /**
     * The name of the parameter type. Short string with less than 30 characters, like "shell command result".
     */
    public abstract String getName();

    /**
     * A description (fuller than what is given in {@link #getName()}) for when the user asks for details about the plugin.
     */
    public abstract String getDescription();

    ///////////////////////////////////////////////////////////////////////////
    // Serialisation
    /**
     * This method is called when the production plan is being serialised to disk. The implementation is required to write one and only one
     * file at the designated path. If the file already exists, it must be overwritten.<br>
     * The implementation is free to use any serialisation method. The use of XStream is however recommended as this bundle is always
     * present for the engine needs.<br>
     * <br>
     * 
     * @param targetFile
     */
    public void serialise(File targetFile, Collection<? extends Parameter> instances);

    /**
     * The reverse method of {@link #serialise(File, Collection)}. <br>
     * <br>
     * <strong>This method is supposed to cope with model version upgrades</strong>. That is, if the given <code>File</code> contains
     * serialised objects related to a previous version of the model, this method will either successfully convert them to the latest
     * version or throw a {@link ModelUpdateException} <br>
     * <br>
     * If any, the deserialised sources should be converted to an object implementing {@link EventSource} and registered through the
     * {@link EventSourceRegistry}
     * 
     * @throws ModelUpdateException
     *             if the plugin is unable to update the model to the latest version
     * @throws SerializationException
     *             for all other errors
     * @param sourceFile
     *            a directory containing the serialised data.
     * @param reg
     *            for registering the deserialised items inside the engine.
     * @return
     */
    public abstract Set<? extends Parameter> deserialise(File sourceFile);
}
