package rocks.paperwork.android.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores and reads app preferences
 */
public class HostPreferences
{
    public static final String HOST = "host";
    public static final String HASH = "hash";
    public static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";
    private static final String PREFERENCES_FILE = "prefs";

    public static void saveSharedSetting(Context context, String settingName, String settingValue)
    {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }

    public static String readSharedSetting(Context context, String settingName, String defaultValue)
    {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(settingName, defaultValue);
    }

    public static void clearPreferences(Context context)
    {
        context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE).edit().clear().commit();
    }

    public static boolean preferencesExist(Context context)
    {
        SharedPreferences sharedPref = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return (sharedPref != null && sharedPref.contains(HOST) && sharedPref.contains(HASH));
    }
}
