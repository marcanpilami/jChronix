package org.oxymores.chronix.core.context;

import java.util.HashMap;
import java.util.Map;

/**
 * A static manager of contexts. Used by the OSGi services that should share a context. Should never be used by the engine itself, as the
 * engine has its own context.
 */
public class ContextHandler
{
    private static transient Map<String, ChronixContextMeta> allMeta = new HashMap<>();
    private static transient Map<String, ChronixContextTransient> allDb = new HashMap<>();

    public static ChronixContextMeta getMeta(String dbPath)
    {
        String key = dbPath;
        if (allMeta.get(key) != null)
        {
            return allMeta.get(key);
        }
        synchronized (allMeta)
        {
            if (allMeta.get(key) != null)
            {
                return allMeta.get(key);
            }
            allMeta.put(key, new ChronixContextMeta(key));
            return allMeta.get(key);
        }
    }

    public static ChronixContextTransient getDb(String db1, String db2)
    {
        String key = db1 + db2;
        if (allDb.get(key) != null)
        {
            return allDb.get(key);
        }
        synchronized (allDb)
        {
            if (allDb.get(key) != null)
            {
                return allDb.get(key);
            }
            allDb.put(key, new ChronixContextTransient(db1, db2));
            return allDb.get(key);
        }
    }

    public static void resetCtx()
    {
        synchronized (allMeta)
        {
            allMeta.clear();
        }
        synchronized (allDb)
        {
            allDb.clear();
        }
    }
}
