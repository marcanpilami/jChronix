package org.oxymores.chronix.api.source2;

/**
 * An {@link EventSourceProvider} can implement this (empty) interface to signal that it allows its event source to receive arbitrary fields
 * in addition to the fields specified by {@link EventSourceProvider#getFields()}.<br>
 */
public interface OptionAllowsAdditionalFields
{

}
