package org.oxymores.chronix.agent.command.api;

/**
 * A set of constants that can be used by runner plug-ins.
 */
public final class RunnerConstants
{
    private RunnerConstants()
    {}

    public static final int MAX_RETURNED_SMALL_LOG_LINES = 500;
    public static final int MAX_RETURNED_SMALL_LOG_CHARACTERS = 10000;
    public static final int MAX_RETURNED_BIG_LOG_LINES = 10000;
    public static final int MAX_RETURNED_BIG_LOG_END_LINES = 100;

    // A list of common shells. Plug-ins should obviously not limit themselves to this list!
    public static final String SHELL_POWERSHELL = "shell.powershell";
    public static final String SHELL_WINCMD = "shell.wincmd";
    public static final String SHELL_BASH = "shell.bash";
    public static final String SHELL_KSH = "shell.ksh";
    public static final String SHELL_ZSH = "shell.zsh";
    public static final String SHELL_CSH = "shell.csh";
    public static final String SHELL_SH = "shell.sh";
}
