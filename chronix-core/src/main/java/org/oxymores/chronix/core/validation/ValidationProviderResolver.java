package org.oxymores.chronix.core.validation;

import java.util.ArrayList;
import java.util.List;

import javax.validation.spi.ValidationProvider;

import org.hibernate.validator.HibernateValidator;

/**
 * This provider is needed because the standard validation provider resolver only looks inside the current class loader. This does not work
 * with OSGI, so this is an explicit reference. <br>
 * Note this is a strong reference to Hibernate Validator. A better way would be to do a service lookup.
 */
public class ValidationProviderResolver implements javax.validation.ValidationProviderResolver
{
    // private boolean isOsgi = FrameworkUtil.getBundle(ValidationProviderResolver.class) != null;

    @Override
    public List<ValidationProvider<?>> getValidationProviders()
    {
        List<ValidationProvider<?>> providers = new ArrayList<>(1);
        providers.add(new HibernateValidator());
        return providers;
    }

}
