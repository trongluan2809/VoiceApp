package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid;

import android.os.Bundle;

import com.google.android.gms.ads.AdValue;
import com.google.firebase.analytics.FirebaseAnalytics;

public class VAppUtility {
    public static void logAdAdmobValue(AdValue adValue, String adUnitId, String mediationAdapterClassName, FirebaseAnalytics mFirebaseAnalytics){
        Bundle bundle =new Bundle();
        bundle.putString("CurrencyCode",adValue.getCurrencyCode());
        bundle.putInt("PrecisionType",adValue.getPrecisionType());
        bundle.putLong("Value",adValue.getValueMicros());
        bundle.putString("AdUnitId", adUnitId);
        bundle.putString("AdNetwork",mediationAdapterClassName);
        mFirebaseAnalytics.logEvent("ad_admob_setOnPaidEventListener", bundle);
        bundle.clear();
    }
}
