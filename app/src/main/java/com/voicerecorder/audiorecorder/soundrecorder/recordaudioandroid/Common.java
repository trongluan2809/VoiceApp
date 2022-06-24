package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid;

import com.github.axet.audiorecorder.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class Common {
    public static String EXTRA_AUDIO_URI = "com.voicerecorder.audiorecorder.soundrecorder.AUDIO_URI";
    public static String EXTRA_BUNDLE = "com.voicerecorder.audiorecorder.soundrecorder.BUNDLE";

    public static void initRemoteConfig(OnCompleteListener listener) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(listener);
    }

    public static String getRemoteConfigAdUnit(String adUnitId) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        return mFirebaseRemoteConfig.getString(adUnitId);
    }

}
