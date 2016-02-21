package org.oxymores.chronix.core.engine.api;

public interface ChronixEngine
{
    public void start();

    public void stop();
    
    public void waitOperational();
    
    public void waitShutdown();
}
