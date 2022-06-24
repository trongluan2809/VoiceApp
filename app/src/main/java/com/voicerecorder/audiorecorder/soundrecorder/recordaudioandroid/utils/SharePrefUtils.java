package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharePrefUtils {
    private static SharedPreferences mSharePref;

    public static void init(Context context) {
        if (mSharePref == null) {
            mSharePref = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }
    public static int  getCountOpenApp(Context context) {
        SharedPreferences pre = context.getSharedPreferences("dataAudio", Context.MODE_PRIVATE);
        return pre.getInt("counts", 0);
    }
    public static void increaseCountOpenApp(Context context) {
        SharedPreferences pre = context.getSharedPreferences("dataAudio", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putInt("counts", pre.getInt("counts", 0) + 1);
        editor.apply();
    }

    public static int  getCountOpenFirstHelp(Context context) {
        SharedPreferences pre = context.getSharedPreferences("dataAudio", Context.MODE_PRIVATE);
        return pre.getInt("first", 0);
    }
    public static void increaseCountFirstHelp(Context context) {
        SharedPreferences pre = context.getSharedPreferences("dataAudio", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putInt("first", pre.getInt("first", 0) + 1);
        editor.apply();
    }
    public static String getEncoding(Context context) {
        SharedPreferences pre = context.getSharedPreferences("dataAudio", Context.MODE_PRIVATE);
        return pre.getString("encoding", "mp3");
        //mp3,m4a,wma,wav
    }
    public static void changeEncoding(Context context,String type) {
        SharedPreferences pre = context.getSharedPreferences("dataAudio", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putString("encoding", type);
        editor.apply();
    }

    public static void setPrefHighQuality(Context context, boolean isEnabled) {
        SharedPreferences pre = context.getSharedPreferences("dataAudio", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pre.edit();
        editor.putBoolean("PREF_HIGH_QUALITY", isEnabled);
        editor.apply();
    }

    public static boolean getPrefHighQuality(Context context) {
        SharedPreferences preferences =  context.getSharedPreferences("dataAudio", Context.MODE_PRIVATE);
        return preferences.getBoolean("PREF_HIGH_QUALITY", false);
    }

    public static String getInfo(Context context){
       return  (getPrefHighQuality(context)?"24 bit":"16 bit") + " | " + getEncoding(context);
    }

}
