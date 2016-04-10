package org.oxymores.chronix.api.source;

/**
 * An {@link EventSourceProvider} can implement this (empty) interface to signal that it allows its event source to receive arbitrary
 * parameters in addition to the fields specified by {@link EventSourceProvider#getFields()}.<br>
 * These are additional key/value (string/string) parameters. For example, a shell command will accept arbitrary parameters to add to its
 * command line. These parameters benefit from the same interpolation and mutualisation as standard fields. Their keys can by empty, void or
 * non unique (and they can have the same key as a field).
 */
public interface OptionAllowsParameters
{

}
