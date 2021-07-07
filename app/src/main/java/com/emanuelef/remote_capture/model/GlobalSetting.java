package com.emanuelef.remote_capture.model;

import android.content.Context;
import android.content.SharedPreferences;

public class GlobalSetting {
    public static int UD ;
    public static String PORT;
    public static String PROT;
    public static int UT;
    public static boolean UA;
    public static String SP_KEY_UD = "global_setting_upload_duration";
    public static String SP_KEY_PORT = "global_setting_port";
    public static String SP_KEY_PROT = "global_setting_protocol";
    public static String SP_KEY_UT = "global_setting_upload_target";
    public static String SP_KEY_UA = "global_setting_upload_active";

    public static void setPORT(String PORT) {
        GlobalSetting.PORT = PORT;
    }

    public static void setPROT(String PROT) {
        GlobalSetting.PROT = PROT;
    }

    public static void setUD(int UD) {
        GlobalSetting.UD = UD;
    }

    public static void setUT(int UT) {
        GlobalSetting.UT = UT;
    }

    public static void setUA(boolean UA) {
        GlobalSetting.UA = UA;
    }


    public static void SaveGlobalSetting(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences(
                Prefs.SHARED_PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SP_KEY_UD,UD);
        editor.putInt(SP_KEY_UT,UT);
        editor.putString(SP_KEY_PORT,PORT);
        editor.putString(SP_KEY_PROT,PROT);
        editor.putBoolean(SP_KEY_UA,UA);
        editor.apply();
    }

    public static void LoadGlobalSetting(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences(
                Prefs.SHARED_PREF_KEY, Context.MODE_PRIVATE);
        UD = sharedPref.getInt(SP_KEY_UD,60);
        UT = sharedPref.getInt(SP_KEY_UT,1);
        PORT = sharedPref.getString(SP_KEY_PORT,"");
        PROT = sharedPref.getString(SP_KEY_PROT,"");
        UA = sharedPref.getBoolean(SP_KEY_UA,false);
    }
}
