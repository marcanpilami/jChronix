package org.oxymores.chronix.api.source2.validators;

import java.util.ArrayList;
import java.util.List;

import org.oxymores.chronix.api.source2.EventSourceFieldValidator;
import org.oxymores.chronix.api.source2.ValidationFailure;

public class NotNull implements EventSourceFieldValidator
{
    // Evil singleton pattern: may the Lord have mercy on our souls.
    private static NotNull _instance;

    public static synchronized NotNull getInstance()
    {
        if (_instance == null)
        {
            _instance = new NotNull();
        }
        return _instance;
    }

    private NotNull()
    {}

    @Override
    public List<ValidationFailure> validate(String value)
    {
        List<ValidationFailure> res = new ArrayList<ValidationFailure>(1);
        if (value == null)
        {
            res.add(new ValidationFailure());
        }
        return res;
    }

}
