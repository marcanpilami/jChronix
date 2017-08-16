package org.oxymores.chronix.engine;

public final class Constants
{
    private Constants()
    {

    }

    // ENGINE
    public static final int DEFAULT_NB_RUNNER = 1;

    // TOKENS
    public static final int MAX_TOKEN_VALIDITY_MN = 10;
    public static final int TOKEN_RENEWAL_MN = 5;
    public static final int TOKEN_AUTO_RENEWAL_LOOP_PERIOD_S = 60;

    // STATUS
    public static final String JI_STATUS_OVERRIDEN = "OVERRIDEN";
    public static final String JI_STATUS_DONE = "DONE";
    public static final String JI_STATUS_RUNNING = "RUNNING";
    public static final String JI_STATUS_CHECK_SYNC_CONDS = "CHECK_SYNC_CONDS";

    // ENV VARS
    public static final String ENV_AUTO_CHR_CALENDAR = "CHR_CALENDAR";
    public static final String ENV_AUTO_CHR_CHR_CALENDARID = "CHR_CALENDARID";
    public static final String ENV_AUTO_CHR_CHR_CALENDARDATE = "CHR_CALENDARDATE";

    // QUEUES
    public static final String Q_LOG = "Q.%s.LOG";
    public static final String Q_LOGFILE = "Q.%s.LOGFILE";
    public static final String Q_ENDOFJOB = "Q.%s.ENDOFJOB";
    public static final String Q_ORDER = "Q.%s.ORDER";
    public static final String Q_TOKEN = "Q.%s.TOKEN";
    public static final String Q_CALENDARPOINTER = "Q.%s.CALENDARPOINTER";
    public static final String Q_EVENT = "Q.%s.EVENT";
    public static final String Q_PJ = "Q.%s.PJ";
    public static final String Q_RUNNER = "Q.%s.RUNNER";
    public static final String Q_RUNNERMGR = "Q.%s.RUNNERMGR";
    public static final String Q_META = "Q.%s.APPLICATION";
    public static final String Q_BOOTSTRAP = "Q.CONSOLE.BOOTSTRAP";
}
