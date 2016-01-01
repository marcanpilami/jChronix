package org.oxymores.chronix.engine.modularity.runner;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A runner is an element that does "actual work": launch a shell command, launches a SQL procedure...<br>
 * It receives a detailed instruction from the engine as a {@link RunDescription} and at the end of run is expected to return a
 * {@link RunResult}.
 */
@ProviderType
public interface RunnerApi
{
    /**
     * Does the actual work (shell command, SQL stored proc...)
     * 
     * @param rd
     *            a property bag to be interpreted by the runner as a command to run.
     * @return the result.
     */
    public RunResult run(RunDescription rd);
}
