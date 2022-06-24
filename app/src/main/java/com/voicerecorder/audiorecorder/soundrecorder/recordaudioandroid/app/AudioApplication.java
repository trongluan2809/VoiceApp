package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.View;

import androidx.preference.PreferenceManager;

import com.amazic.ads.util.Admod;
import com.amazic.ads.util.AppOpenManager;
import com.github.axet.androidlibrary.app.NotificationManagerCompat;
import com.github.axet.androidlibrary.widgets.NotificationChannelCompat;
import com.github.axet.androidlibrary.widgets.RemoteNotificationCompat;
import com.github.axet.audiorecorder.R;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities.MainActivity;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities.SplashActivity;
import com.voicerecorder.axet.audiolibrary.app.MainApplication;
import com.voicerecorder.axet.audiolibrary.encoders.FormatM4A;
import com.voicerecorder.axet.audiolibrary.encoders.FormatOGG;

import java.util.Locale;

public class AudioApplication extends MainApplication {
    public static final String PREFERENCE_CONTROLS = "controls";
    public static final String PREFERENCE_TARGET = "target";
    public static final String PREFERENCE_FLY = "fly";
    public static final String PREFERENCE_SOURCE = "bluetooth";
    public static final String PREFERENCE_VERSION = "version";
    public static final String PREFERENCE_NEXT = "next";

    public NotificationChannelCompat channelStatus;
    public NotificationChannelCompat channelPersistent;
    public RecordingStorage recording;
    public EncodingStorage encodings;


    public static AudioApplication from(Context context) {
        return (AudioApplication) MainApplication.from(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setLocale(this, "en");
        channelStatus = new NotificationChannelCompat(this, "status", "Status", NotificationManagerCompat.IMPORTANCE_LOW);
        channelPersistent = new NotificationChannelCompat(this, "persistent", "Persistent", NotificationManagerCompat.IMPORTANCE_LOW);
        encodings = new EncodingStorage(this);





        switch (getVersion(PREFERENCE_VERSION, R.xml.pref_general)) {
            case -1:
                SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor edit = shared.edit();
                if (!FormatOGG.supported(this)) {
                    edit.putString(AudioApplication.PREFERENCE_ENCODING, FormatM4A.EXT);
                }
                edit.putInt(PREFERENCE_VERSION, 4);
                edit.apply();
                break;
            case 0:
                version_0_to_1();
                version_1_to_2();
                break;
            case 1:
                version_1_to_2();
                break;
            case 2:
                version_2_to_3();
                break;
            case 3:
                version_3_to_4();
                break;
        }
    }

    public static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    void version_0_to_1() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = shared.edit();
        edit.putFloat(PREFERENCE_VOLUME, shared.getFloat(PREFERENCE_VOLUME, 0) + 1); // update volume from 0..1 to 0..1..4
        edit.putInt(PREFERENCE_VERSION, 1);
        edit.apply();
    }

    void show(String title, String text) {
        PendingIntent main = PendingIntent.getService(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteNotificationCompat.Builder builder = new RemoteNotificationCompat.Builder(this, R.layout.notifictaion);
        builder.setViewVisibility(R.id.notification_record, View.GONE);
        builder.setViewVisibility(R.id.notification_pause, View.GONE);
        builder.setTheme(AudioApplication.getTheme(this, R.style.RecThemeLight, R.style.RecThemeDark))
                .setImageViewTint(R.id.icon_circle, builder.getThemeColor(R.attr.colorButtonNormal))
                .setTitle(title)
                .setText(text)
                .setMainIntent(main)
                .setChannel(channelStatus)
                .setSmallIcon(R.drawable.ic_notification_ring);
        NotificationManagerCompat nm = NotificationManagerCompat.from(this);
        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    @SuppressLint("RestrictedApi")
    void version_1_to_2() {
        Locale locale = Locale.getDefault();
        if (locale.toString().startsWith("ru")) {
            String title = "Программа переименована";
            String text = "'Аудио Рекордер' -> '" + getString(R.string.app_name) + "'";
            show(title, text);
        }
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = shared.edit();
        edit.putInt(PREFERENCE_VERSION, 2);
        edit.apply();
    }

    @SuppressLint("RestrictedApi")
    void version_2_to_3() {
        Locale locale = Locale.getDefault();
        if (locale.toString().startsWith("tr")) {
            String title = "Application renamed";
            String text = "'VoiceRecorder' -> '" + getString(R.string.app_name) + "'";
            show(title, text);
        }
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = shared.edit();
        edit.putInt(PREFERENCE_VERSION, 3);
        edit.apply();
    }

    @SuppressLint("RestrictedApi")
    void version_3_to_4() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = shared.edit();
        edit.remove(PREFERENCE_SORT);
        edit.putInt(PREFERENCE_VERSION, 4);
        edit.apply();
    }
}
