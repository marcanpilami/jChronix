package org.oxymores.chronix.core.engine.api;

import java.util.List;
import java.util.UUID;

import org.osgi.annotation.versioning.ProviderType;
import org.oxymores.chronix.dto.DTOApplication;
import org.oxymores.chronix.dto.DTOApplicationShort;
import org.oxymores.chronix.dto.DTOEnvironment;
import org.oxymores.chronix.dto.DTORRule;
import org.oxymores.chronix.dto.DTOResultClock;
import org.oxymores.chronix.dto.DTOValidationError;

/**
 * This service gives access to all allowed operations on a production plan: creating a new application, modifying an application, ...
 * 
 */
@ProviderType
public interface PlanAccessService
{
    /**
     * Returns a new application which has all the needed technical items to work, but is otherwise empty (no chains, no shell commands,
     * ...). <strong>This is the only supported way to create a new application through the API.</strong> <br>
     * The new application is not saved anywhere by this method so it will be lost if the returned object is not explicitly saved (for
     * example with {@link #saveApplicationDraft(DTOApplication)}).
     */
    public DTOApplication2 createMinimalApplication();

    /**
     * Returns a new demo application. For demo & documentation purposes. For all other purposes, use {@link #createMinimalApplication()}
     * instead. <br>
     * This application is not saved anywhere by this method so it will be lost if the returned object is not explicitly saved (for example
     * with {@link #saveApplicationDraft(DTOApplication)}).<br>
     * No client should ever rely on the result of this method being something it expects. A modification of this method does not change the
     * version of the overall service.
     */
    public DTOApplication2 createTestApplication();

    /**
     * Get a short description of all the applications which exist in the local node context.<br>
     * To get the full details of an application, use {@link #getApplication(String)}
     */
    public List<DTOApplicationShort> getAllApplications();

    /**
     * Get the full description of an application identified by its ID. The returned object can then be modified and given to the save
     * method. <br>
     * <ul>
     * <li>if a draft exists for this application ID, the draft content is returned</li>
     * <li>if no draft exist AND the application exists (it is active), the active version is returned</li>
     * <li>if no active version NOR draft, an exception is raised</li>
     * </ul>
     * The ID of an application can be found with the help of {@link #getAllApplications()}.
     * 
     * @param id
     *            must be a valid UUID
     * @return
     */
    public DTOApplication2 getApplication(UUID id);

    /**
     * The environment contains all the metadata that is not directly inside an application: the network, the places, ...
     */
    public DTOEnvironment getEnvironment();

    /**
     * Helper method that will return a list of the next few occurrences for a recurrence rule.
     */
    //public DTOResultClock testRecurrenceRule(DTORRule rule);

    /**
     * <strong>Data loss danger</strong><br>
     * Will remove the next version draft for the designated application. Only the next version draft is removed - if there is an active
     * version of the application, it is left untouched. Next call to getApplication will retrieve the running version (if any), and next
     * call to {@link #saveApplicationDraft(DTOApplication)} will create a new draft.
     */
    public void resetApplicationDraft(DTOApplication2 app);

    /**
     * Will save the application object to disk, inside a special "next version draft" file. Subsequent calls to
     * {@link #getApplication(String)} will return this saved object.<br>
     * It is possible to save invalid application object (as per {@link #validateApplication(DTOApplication)})<br>
     * The draft file is not versioned, and is always fully replaced on each call to this method: <strong>the last call to this method
     * always win - there is no concurrent editing of an application</strong>
     */
    public void saveApplicationDraft(DTOApplication2 app);

    /**
     * Sends the draft of an application to all scheduler nodes. It becomes active as soon as the nodes receive it. <br>
     * Remember to save your changes inside the draft with {@link #saveDraft(DTOApplication)} before calling this method! <br>
     * The application saved inside the draft <strong>must be valid</strong> before calling this method (see
     * {@link #validateApplication(DTOApplication)}). If not, this method will throw an exception.<br>
     * This method is <strong>asynchronous</strong> - it returns as soon as the data has been sent to the other nodes, without waiting for
     * their answers. The draft (rendered useless as its data has become the active version of the application) is removed if the method is
     * successful.
     */
    public void promoteApplicationDraft(UUID id, String commitMessage);

    /**
     * Will save the environment object to disk, inside a special "next version draft" file. Subsequent calls to {@link #getEnvironment()}
     * will return this saved object.<br>
     * It is possible to save invalid environment object (as per {@link #validateEnvironment(DTOEnvironment)})<br>
     * The draft file is not versioned, and is always fully replaced on each call to this method: <strong>the last call to this method
     * always win - there is no concurrent editing of a draft environment</strong>
     */
    public void saveEnvironmentDraft(DTOEnvironment e);

    /**
     * Sends the latest saved environment draft to all nodes. It becomes active as soon as it is received by the nodes.<br>
     * Please note this will call {@link #validateEnvironment(DTOEnvironment)} and raise an exception if not valid. It is therefore
     * recommended to call {@link #validateEnvironment(DTOEnvironment)} before calling this method if you want to avoid exceptions (and
     * potentially create a better user experience)
     */
    public void promoteEnvironmentDraft(String commitMessage);

    /**
     * Creates a minimal environment with values extrapolated from the server's network environment (hostname, ...).<br>
     * <strong>This is the only supported way to create an environment object</strong>
     */
    public DTOEnvironment createMinimalEnvironment();

    /**
     * Checks for errors inside an application object.
     */
    public List<DTOValidationError> validateApplication(DTOApplication2 app);

    /**
     * Checks for errors inside an environment object.
     */
    public List<DTOValidationError> validateEnvironment(DTOEnvironment nn);

    /**
     * This will throw away all in-memory data and reload applications and network from the persisted storage.
     */
    public void resetCache();
}
