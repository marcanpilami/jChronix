package org.oxymores.chronix.agent.command.api;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A runner is an element that does "actual work": launch a shell command, launches a SQL procedure...<br>
 * It receives a detailed instruction from the engine as a {@link CommandDescription} and at the end of run is expected to return a
 * {@link CommandResult}.
 */
@ProviderType
public interface CommandRunner
{
    /**
     * Does the actual work (shell command, SQL stored proc...)
     * 
     * @param rd
     *            a property bag to be interpreted by the runner as a command to run.
     * @return the result.
     */
    public CommandResult run(CommandDescription rd);
}
