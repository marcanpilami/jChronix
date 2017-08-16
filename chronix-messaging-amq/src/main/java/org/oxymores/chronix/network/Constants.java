package org.oxymores.chronix.network;

final class Constants
{
    private Constants()
    {}

    // GENERAL
    public static final int KB = 1024;
    public static final int MB = KB * 1024;

    // BROKER
    public static final int DEFAULT_BROKER_MEM_USAGE = 20 * MB;
    public static final int DEFAULT_BROKER_STORE_USAGE = 38 * MB;
    public static final int DEFAULT_BROKER_TEMP_USAGE = 38 * MB;
    public static final int DEFAULT_BROKER_NETWORK_CONNECTOR_TTL_S = 20;

    // ENGINE
    public static final int BROKER_PORT_FREEING_MS = 1000;
}
