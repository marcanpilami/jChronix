/**
 * @author Marc-Antoine Gouillart
 *
 * See the NOTICE file distributed with this work for information regarding
 * copyright ownership. This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.oxymores.chronix.web.api;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.core.engine.api.DTOApplicationShort;
import org.oxymores.chronix.core.engine.api.PlanAccessService;
import org.oxymores.chronix.dto.DTOEnvironment;
import org.oxymores.chronix.dto.DTOValidationError;
import org.oxymores.chronix.engine.modularity.web.RestServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles all the metadata related services. JSON only.
 */
@Component(service = ServiceMeta.class)
@JaxrsResource
@Path("/meta")
public class ServiceMeta implements RestServiceApi
{
    private static final Logger log = LoggerFactory.getLogger(ServiceMeta.class);

    private PlanAccessService api;

    @Reference
    public void setApi(PlanAccessService api)
    {
        this.api = api;
    }

    public void unsetApi()
    {
        this.api = null;
    }

    public ServiceMeta()
    {
        // Default constructor - for OSGI construction
    }

    public ServiceMeta(PlanAccessService api)
    {
        // Constructor for non-OSGI servlet containers
        this.api = api;
    }

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello()
    {
        log.debug("Ping service was called");
        return "houba hop";
    }

    @GET
    @Path("environment")
    @Produces(MediaType.APPLICATION_JSON)
    public DTOEnvironment getEnvironment()
    {
        return api.getEnvironment();
    }

    @POST
    @Path("environment")
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveEnvironmentDraft(DTOEnvironment e)
    {
        api.saveEnvironmentDraft(e);
    }

    @POST
    @Path("liveenvironment")
    @Consumes(MediaType.APPLICATION_JSON)
    public void promoteEnvironmentDraft()
    {
        api.promoteEnvironmentDraft("commit web");
    }

    @POST
    @Path("environment/newdemo")
    @Produces(MediaType.APPLICATION_JSON)
    public DTOEnvironment createTestEnvironment()
    {
        return api.createMinimalEnvironment();
    }

    @GET
    @Path("app/{appid}")
    @Produces(MediaType.APPLICATION_JSON)
    public DTOApplication getApplication(@PathParam("appid") UUID id)
    {
        log.info("getApplication was called");
        try
        {
            return api.getApplication(id);
        }
        catch (Exception e)
        {
            log.error("hhh", e);
            throw e;
        }
    }

    @POST
    @Path("app")
    @Consumes(MediaType.APPLICATION_JSON)
    public void saveApplicationDraft(DTOApplication app)
    {
        api.saveApplicationDraft(app);
    }

    @POST
    @Path("liveapp")
    @Consumes(MediaType.APPLICATION_JSON)
    public void promoteApplicationDraft(DTOApplication app)
    {
        api.promoteApplicationDraft(app.getId(), "commit message");
    }

    @POST
    @Path("xappunstage")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resetApplicationDraft(DTOApplication app)
    {
        api.resetApplicationDraft(app);
    }

    // TODO: save this.
    /*
     * @POST
     * 
     * @Path("rrule/test")
     * 
     * @Produces(MediaType.APPLICATION_JSON)
     * 
     * @Consumes(MediaType.APPLICATION_JSON) public DTOResultClock getNextRRuleOccurrences(DTORRule rule) { return
     * api.testRecurrenceRule(rule); }
     */

    @GET
    @Path("app")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DTOApplicationShort> getAllApplications()
    {
        log.info("getAllApplications was called");
        try
        {
            return api.getAllApplications();
        }
        catch (Exception e)
        {
            log.error("hhh", e);
            throw e;
        }
    }

    @GET
    @Path("newapp")
    @Produces(MediaType.APPLICATION_JSON)
    public DTOApplication createMinimalApplication()
    {
        return api.createMinimalApplication();
    }

    @POST
    @Path("app/newdemo")
    @Produces(MediaType.APPLICATION_JSON)
    public DTOApplication createTestApplication()
    {
        return api.createTestApplication();
    }

    @POST
    @Path("app/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<DTOValidationError> validateApplication(DTOApplication app)
    {
        return api.validateApplication(app);
    }

    @POST
    @Path("environment/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<DTOValidationError> validateEnvironment(DTOEnvironment nn)
    {
        return api.validateEnvironment(nn);
    }
}
