package com.emanuelef.remote_capture.model;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;

import java.util.ArrayList;

public class GlobalSetting {
    public static int UD ;
    public static String FILTER = "";
    public static int UT;
    public static boolean UA;
    public static String SP_KEY_UD = "global_setting_upload_duration";
    public static String SP_KEY_FILTER = "global_setting_filter";
    public static String SP_KEY_UT = "global_setting_upload_target";
    public static String SP_KEY_UA = "global_setting_upload_active";
    public static int PROOFD = 180;
    public static ArrayList<Byte> PROT_FILTER = new ArrayList<>();
    public static JSONArray SMART_FILTER;

    public static void setUD(int UD) {
        GlobalSetting.UD = Math.max(UD, 60);
    }

    public static void setUT(int UT) {
        GlobalSetting.UT = UT;
    }

    public static void setUA(boolean UA) {
        GlobalSetting.UA = UA;
    }

    public static void setFILTER(String FILTER) {
        GlobalSetting.FILTER = FILTER;
    }

    public static void SaveGlobalSetting(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences(
                Prefs.SHARED_PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(SP_KEY_UD,UD);
        editor.putInt(SP_KEY_UT,UT);
        editor.putString(SP_KEY_FILTER,FILTER);
        editor.putBoolean(SP_KEY_UA,UA);
        editor.apply();
    }

    public static void LoadGlobalSetting(Context context){
        SharedPreferences sharedPref = context.getSharedPreferences(
                Prefs.SHARED_PREF_KEY, Context.MODE_PRIVATE);
        UD = sharedPref.getInt(SP_KEY_UD,60);
        UT = sharedPref.getInt(SP_KEY_UT,1);
        FILTER = sharedPref.getString(SP_KEY_FILTER,"");
        UA = sharedPref.getBoolean(SP_KEY_UA,false);
    }
}
