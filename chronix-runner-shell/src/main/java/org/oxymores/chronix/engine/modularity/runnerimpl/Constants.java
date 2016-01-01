package org.oxymores.chronix.engine.modularity.runnerimpl;

final class Constants
{
    private Constants()
    {}

    public static enum SHELL {
        POWERSHELL, CMD, BASH, SH, KSH
    }

    public static final String PLUGIN_POWERSHELL = "org.oxymores.chronix.runner.shell.powershell";
    public static final String PLUGIN_WINCMD = "org.oxymores.chronix.runner.shell.wincmd";
    public static final String PLUGIN_BASH = "org.oxymores.chronix.runner.shell.bash";
    public static final String PLUGIN_KSH = "org.oxymores.chronix.runner.shell.ksh";
    public static final String PLUGIN_SH = "org.oxymores.chronix.runner.shell.sh";
}
