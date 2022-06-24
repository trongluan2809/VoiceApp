package com.voicerecorder.axet.audiolibrary.trimmer.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class Utility {
    public static final String AUDIO_FORMAT = ".wav";

    //audio mime type in which file after trim will be saved.
    public static final String AUDIO_MIME_TYPE = "audio/wav";

    public static long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

    public static void log(String msg) {
        Log.i("===", msg);
    }

    // Function to check and request permission.
    public static boolean checkPermission(Activity activity, String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
            return false;
        } else {
            return true;
        }
    }
}
