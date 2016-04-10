package org.oxymores.chronix.api.source2;

import java.util.List;

public interface EventSourceFieldValidator
{
    /**
     * If the returned list is empty, then item is valid. Otherwise, the list is expected to contain all validation errors (not only the
     * first errors encountered).
     */
    public List<ValidationFailure> validate(String value);
}
