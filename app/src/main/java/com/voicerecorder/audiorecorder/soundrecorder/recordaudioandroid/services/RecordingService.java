package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.github.axet.androidlibrary.app.AlarmManager;
import com.github.axet.androidlibrary.app.ProximityShader;
import com.github.axet.androidlibrary.preferences.OptimizationPreferenceCompat;
import com.github.axet.androidlibrary.services.PersistentService;
import com.github.axet.androidlibrary.widgets.RemoteNotificationCompat;
import com.github.axet.androidlibrary.widgets.RemoteViewsCompat;
import com.voicerecorder.axet.audiolibrary.app.Storage;
import com.github.axet.audiorecorder.R;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities.MainActivity;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities.RecordingActivity;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.AudioApplication;

import java.io.File;

/**
 * Sometimes RecordingActivity started twice when launched from lockscreen.
 * We need service and keep recording into Application object.
 */
public class RecordingService extends PersistentService {
    public static final String TAG = RecordingService.class.getSimpleName();

    public static final int NOTIFICATION_RECORDING_ICON = 1;

    public static String SHOW_ACTIVITY = RecordingService.class.getCanonicalName() + ".SHOW_ACTIVITY";
    public static String PAUSE_BUTTON = RecordingService.class.getCanonicalName() + ".PAUSE_BUTTON";
    public static String RECORD_BUTTON = RecordingService.class.getCanonicalName() + ".RECORD_BUTTON";

    static {
        OptimizationPreferenceCompat.REFRESH = AlarmManager.MIN1;
    }

    Storage storage; // for storage path

    public static void startIfPending(Context context) { // if recording pending or controls enabled
        Storage storage = new Storage(context);
        if (storage.recordingPending()) {
            final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
            String f = shared.getString(AudioApplication.PREFERENCE_TARGET, "");
            String d;
           /* if (f.startsWith(ContentResolver.SCHEME_CONTENT)) {
                Uri u = Uri.parse(f);
                d = Storage.getDocumentName(context, u);
            } else if (f.startsWith(ContentResolver.SCHEME_FILE)) {
                Uri u = Uri.parse(f);
                File file = Storage.getFile(u);
                d = file.getName();
            } else {
                File file = new File(f);
                d = file.getName();
            }
            startService(context, d, false, null);*/
            try{
                if (f.startsWith(ContentResolver.SCHEME_CONTENT)) {
                    Uri u = Uri.parse(f);
                    d = Storage.getDocumentName(context, u);
                } else if (f.startsWith(ContentResolver.SCHEME_FILE)) {
                    Uri u = Uri.parse(f);
                    File file = Storage.getFile(u);
                    d = file.getName();
                } else {
                    File file = new File(f);
                    d = file.getName();
                }
                startService(context, d, false, null);
            }catch (Exception exception){
                exception.printStackTrace();
            }
            return;
        }
    }

    public static void start(Context context) { // start persistent icon service
        start(context, new Intent(context, RecordingService.class));
    }

    public static void startService(Context context, String targetFile, boolean recording, String duration) { // start recording / pause service
        start(context, new Intent(context, RecordingService.class)
                .putExtra("targetFile", targetFile)
                .putExtra("recording", recording)
                .putExtra("duration", duration)
        );
    }

    public static void stopRecording(Context context) {
        stop(context);
    }

    public static void stop(Context context) {
        stop(context, new Intent(context, RecordingService.class));
    }

    public RecordingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onCreateOptimization() {
        storage = new Storage(this);
        optimization = new OptimizationPreferenceCompat.ServiceReceiver(this, NOTIFICATION_RECORDING_ICON, null, AudioApplication.PREFERENCE_NEXT) {
            Intent notificationIntent; // speed up, update notification text without calling notify()

            @Override
            public void onCreateIcon(Service service, int id) {
                icon = new OptimizationPreferenceCompat.OptimizationIcon(service, id, key) {
                    @Override
                    public void updateIcon() {
                        icon.updateIcon(new Intent());
                    }

                    @Override
                    public void updateIcon(Intent intent) {
                        super.updateIcon(intent);
                        notificationIntent = intent;
                    }

                    @SuppressLint("RestrictedApi")
                    public Notification build(Intent intent) {
                        String targetFile = intent.getStringExtra("targetFile");
                        boolean recording = intent.getBooleanExtra("recording", false);
                        boolean encoding = false;
                        String duration = intent.getStringExtra("duration");

                        PendingIntent main;

                        RemoteNotificationCompat.Builder builder;

                        String title;
                        String text;
                        if (recording)
                            title = getString(R.string.recording_title);
                        else
                            title = getString(R.string.pause_title);
                        if (duration != null) {
                            title += " (" + duration + ")";
                            if (recording && notificationIntent != null && notificationIntent.hasExtra("duration") && notificationIntent.getBooleanExtra("recording", false)) {
                                try {
                                    RemoteViews a = new RemoteViews(getPackageName(), icon.notification.contentView.getLayoutId());
                                    a.setTextViewText(R.id.title, title);
                                    RemoteViewsCompat.mergeRemoteViews(icon.notification.contentView, a);
                                    if (Build.VERSION.SDK_INT >= 16 && icon.notification.bigContentView != null) {
                                        a = new RemoteViews(getPackageName(), icon.notification.bigContentView.getLayoutId());
                                        a.setTextViewText(R.id.title, title);
                                        RemoteViewsCompat.mergeRemoteViews(icon.notification.bigContentView, a);
                                    }
                                    return icon.notification;
                                } catch (RuntimeException e) {
                                    Log.d(TAG, "merge failed", e);
                                }
                            }
                        }
                        text = ".../" + targetFile;
                        builder = new RemoteNotificationCompat.Builder(context, R.layout.notifictaion);
                        builder.setViewVisibility(R.id.notification_record, View.GONE);
                        builder.setViewVisibility(R.id.notification_pause, View.VISIBLE);
                        main = PendingIntent.getService(context, 0, new Intent(context, RecordingService.class)
                                .setAction(SHOW_ACTIVITY)
                                .putExtra("targetFile", targetFile)
                                .putExtra("recording", recording), PendingIntent.FLAG_UPDATE_CURRENT);

                        PendingIntent pe = PendingIntent.getService(context, 0,
                                new Intent(context, RecordingService.class).setAction(PAUSE_BUTTON),
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        PendingIntent re = PendingIntent.getService(context, 0,
                                new Intent(context, RecordingService.class).setAction(RECORD_BUTTON),
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        if (encoding) {
                            builder.setViewVisibility(R.id.notification_pause, View.GONE);
                            title = getString(R.string.encoding_title);
                        }

                        builder.setOnClickPendingIntent(R.id.notification_pause, pe);
                        builder.setOnClickPendingIntent(R.id.notification_record, re);
                        builder.setImageViewResource(R.id.notification_pause, !recording ? R.drawable.ic_play : R.drawable.ic_pause);
                        builder.setContentDescription(R.id.notification_pause, getString(!recording ? R.string.record_button : R.string.pause_button));

                        builder.setTheme(AudioApplication.getTheme(context, R.style.RecThemeLight, R.style.RecThemeDark))
                                .setChannel(AudioApplication.from(context).channelStatus)
                                .setImageViewTint(R.id.icon_circle, builder.getThemeColor(R.attr.colorButtonNormal))
                                .setTitle(title)
                                .setText(text)
                                .setWhen(icon.notification)
                                .setMainIntent(main)
                                .setAdaptiveIcon(R.drawable.ic_notification_ring_adaptive)
                                .setSmallIcon(R.drawable.ic_notification_ring)
                                .setOngoing(true);

                        return builder.build();
                    }
                };
                icon.create();
            }

            @Override
            public boolean isOptimization() {
                return true; // we not using optimization preference
            }
        };
        optimization.create();
    }

    @Override
    public void onStartCommand(Intent intent) {
        String a = intent.getAction();
        if (a == null) {
            optimization.icon.updateIcon(intent);
        } else if (a.equals(PAUSE_BUTTON)) {
            Intent i = new Intent(RecordingActivity.PAUSE_BUTTON);
            sendBroadcast(i);
        } else if (a.equals(RECORD_BUTTON)) {
            RecordingActivity.startActivity(this, false);
        } else if (a.equals(SHOW_ACTIVITY)) {
            ProximityShader.closeSystemDialogs(this);
            if (intent.getStringExtra("targetFile") == null)
                MainActivity.startActivity(this);
            else
                RecordingActivity.startActivity(this, !intent.getBooleanExtra("recording", false));
        }
    }
}
