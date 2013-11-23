/*
 * Taken from http://stackoverflow.com/a/6163701
 */
package org.oxymores.chronix.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;
import org.oxymores.chronix.exceptions.ChronixException;

public final class WinRegistry
{
    private static Logger log = Logger.getLogger(WinRegistry.class);

    public static final int HKEY_CURRENT_USER = 0x80000001;
    public static final int HKEY_LOCAL_MACHINE = 0x80000002;
    public static final int REG_SUCCESS = 0;
    public static final int REG_NOTFOUND = 2;
    public static final int REG_ACCESSDENIED = 5;

    private static final int KEY_READ = 0x20019;
    private static Preferences userRoot = Preferences.userRoot();
    private static Preferences systemRoot = Preferences.systemRoot();
    private static Class<? extends Preferences> userClass = userRoot.getClass();
    private static Method regOpenKey = null;
    private static Method regCloseKey = null;
    private static Method regQueryValueEx = null;
    private static Method regEnumValue = null;
    private static Method regQueryInfoKey = null;
    private static Method regEnumKeyEx = null;
    private static Method regCreateKeyEx = null;
    private static Method regSetValueEx = null;
    private static Method regDeleteKey = null;
    private static Method regDeleteValue = null;

    static
    {
        try
        {
            regOpenKey = userClass.getDeclaredMethod("WindowsRegOpenKey", new Class[] { int.class, byte[].class, int.class });
            regOpenKey.setAccessible(true);
            regCloseKey = userClass.getDeclaredMethod("WindowsRegCloseKey", new Class[] { int.class });
            regCloseKey.setAccessible(true);
            regQueryValueEx = userClass.getDeclaredMethod("WindowsRegQueryValueEx", new Class[] { int.class, byte[].class });
            regQueryValueEx.setAccessible(true);
            regEnumValue = userClass.getDeclaredMethod("WindowsRegEnumValue", new Class[] { int.class, int.class, int.class });
            regEnumValue.setAccessible(true);
            regQueryInfoKey = userClass.getDeclaredMethod("WindowsRegQueryInfoKey1", new Class[] { int.class });
            regQueryInfoKey.setAccessible(true);
            regEnumKeyEx = userClass.getDeclaredMethod("WindowsRegEnumKeyEx", new Class[] { int.class, int.class, int.class });
            regEnumKeyEx.setAccessible(true);
            regCreateKeyEx = userClass.getDeclaredMethod("WindowsRegCreateKeyEx", new Class[] { int.class, byte[].class });
            regCreateKeyEx.setAccessible(true);
            regSetValueEx = userClass.getDeclaredMethod("WindowsRegSetValueEx", new Class[] { int.class, byte[].class, byte[].class });
            regSetValueEx.setAccessible(true);
            regDeleteValue = userClass.getDeclaredMethod("WindowsRegDeleteValue", new Class[] { int.class, byte[].class });
            regDeleteValue.setAccessible(true);
            regDeleteKey = userClass.getDeclaredMethod("WindowsRegDeleteKey", new Class[] { int.class, byte[].class });
            regDeleteKey.setAccessible(true);
        }
        catch (RuntimeException e)
        {
            log.error("Error initializing the Windows Registry handler", e);
        }
        catch (NoSuchMethodException e)
        {
            log.error("The Java implementation used has no access to the Windows registry", e);
        }
    }

    private WinRegistry()
    {

    }

    /**
     * Read a value from key and value name
     * 
     * @param hkey
     *            HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key
     * @param valueName
     * @return the value
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static String readString(Integer hkey, String key, String valueName) throws ChronixException
    {
        if (hkey == HKEY_LOCAL_MACHINE)
        {
            return readString(systemRoot, hkey, key, valueName);
        }
        else if (hkey == HKEY_CURRENT_USER)
        {
            return readString(userRoot, hkey, key, valueName);
        }
        else
        {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    // =====================

    private static String readString(Preferences root, Integer hkey, String key, String value) throws ChronixException
    {
        try
        {
            int[] handles = (int[]) regOpenKey.invoke(root, new Object[] { hkey, toCstr(key), KEY_READ });
            if (handles[1] != REG_SUCCESS)
            {
                return null;
            }
            byte[] valb = (byte[]) regQueryValueEx.invoke(root, new Object[] { handles[0], toCstr(value) });
            regCloseKey.invoke(root, new Object[] { handles[0] });
            return valb != null ? new String(valb).trim() : null;
        }
        catch (Exception e)
        {
            throw new ChronixException("Could not read registry string", e);
        }
    }

    // utility
    private static byte[] toCstr(String str)
    {
        byte[] result = new byte[str.length() + 1];

        for (int i = 0; i < str.length(); i++)
        {
            result[i] = (byte) str.charAt(i);
        }
        result[str.length()] = 0;
        return result;
    }
}