package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.WindowCallbackWrapper;
import androidx.multidex.BuildConfig;

import com.amazic.ads.util.AppOpenManager;
import com.github.axet.androidlibrary.activities.AppCompatThemeActivity;
import com.github.axet.androidlibrary.services.FileProvider;
import com.github.axet.androidlibrary.services.StorageProvider;
import com.github.axet.androidlibrary.sound.AudioTrack;
import com.github.axet.androidlibrary.sound.Headset;
import com.github.axet.androidlibrary.widgets.ErrorDialog;
import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.androidlibrary.widgets.PopupWindowCompat;
import com.github.axet.audiorecorder.R;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.AudioApplication;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.RecordingStorage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.Storage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.services.BluetoothReceiver;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.services.ControlsService;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.services.EncodingService;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.services.RecordingService;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;
import com.voicerecorder.axet.audiolibrary.app.RawSamples;
import com.voicerecorder.axet.audiolibrary.app.Sound;
import com.voicerecorder.axet.audiolibrary.widgets.PitchView;

import java.io.File;
import java.nio.ShortBuffer;
import java.util.Locale;
import java.util.Objects;

public class RecordingActivity extends AppCompatThemeActivity {
    public static final String TAG = RecordingActivity.class.getSimpleName();
    public static final int RESULT_START = 1;

    public static final String[] PERMISSIONS_AUDIO = new String[]{
            Manifest.permission.RECORD_AUDIO
    };

    public static final String ERROR = RecordingActivity.class.getCanonicalName() + ".ERROR";
    public static final String START_PAUSE = RecordingActivity.class.getCanonicalName() + ".START_PAUSE";
    public static final String PAUSE_BUTTON = RecordingActivity.class.getCanonicalName() + ".PAUSE_BUTTON";
    public static final String ACTION_FINISH_RECORDING = BuildConfig.APPLICATION_ID + ".STOP_RECORDING";

    public static final String START_RECORDING = RecordingService.class.getCanonicalName() + ".START_RECORDING";
    public static final String STOP_RECORDING = RecordingService.class.getCanonicalName() + ".STOP_RECORDING";

    private PhoneStateChangeListener pscl = new PhoneStateChangeListener();
    private Headset headset;
    private Intent recordSoundIntent = null;

    boolean start = true; // do we need to start recording immidiatly?

    private AudioTrack play; // current play sound track

    //
    EditText edt;
    //
    private TextView title;
    private TextView time;
    private String duration;
    private TextView state;
    private ImageButton pause;
    private View done;
    private PitchView pitch;

    private ScreenReceiver screen;

    private RecordingStorage recording;
    private File encoding;

    private RecordingReceiver receiver;

    private MainActivity.ProgressHandler progress;

    private AlertDialog muted;
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == RecordingStorage.PINCH)
                pitch.add((Double) msg.obj);
            if (msg.what == RecordingStorage.UPDATESAMPLES)
                updateSamples((Long) msg.obj);
            if (msg.what == RecordingStorage.MUTED) {
                if (Build.VERSION.SDK_INT >= 28)
                    muted = RecordingActivity.startActivity(RecordingActivity.this, getString(R.string.mic_muted_error), getString(R.string.mic_muted_pie));
                else
                    muted = RecordingActivity.startActivity(RecordingActivity.this, "Error", getString(R.string.mic_muted_error));
            }

            if (msg.what == RecordingStorage.UNMUTED) {
                if (muted != null) {
                    AutoClose run = new AutoClose(muted);
                    run.run();
                    muted = null;
                }
            }

            if (msg.what == RecordingStorage.END) {
                pitch.drawEnd();
                if (!recording.interrupt.get()) {
                    stopRecording(getString(R.string.recording_status_pause));
                    String text = "Error reading from stream";
                    if (Build.VERSION.SDK_INT >= 28)
                        muted = RecordingActivity.startActivity(RecordingActivity.this, text, getString(R.string.mic_muted_pie));
                    else
                        muted = RecordingActivity.startActivity(RecordingActivity.this, getString(R.string.mic_muted_error), text);
                }
            }
        }
    };

    public static void startActivity(Context context, boolean pause) {
        Intent i = new Intent(context, RecordingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (pause)
            i.setAction(RecordingActivity.START_PAUSE);
        context.startActivity(i);
    }

    public static AlertDialog startActivity(final AppCompatActivity a, final String title, final String msg) {
        Runnable run = () -> {
            Intent i = new Intent(a, RecordingActivity.class);
            i.setAction(RecordingActivity.ERROR);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra("error", title);
            i.putExtra("msg", msg);
            a.startActivity(i);
        };
        if (a.isFinishing()) {
            run.run();
            return null;
        }
        try {
            AlertDialog muted = new ErrorDialog(a, msg).setTitle(title).show();
            Intent i = new Intent(a, RecordingActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            a.startActivity(i);
            return muted;
        } catch (Exception e) {
            Log.d(TAG, "startActivity", e);
            run.run();
            return null;
        }
    }

    public static void stopRecording(Context context) {
        context.sendBroadcast(new Intent(ACTION_FINISH_RECORDING));
    }

    public class AutoClose implements Runnable {
        int count = 5;
        AlertDialog d;
        Button button;

        public AutoClose(AlertDialog muted, int count) {
            this(muted);
            this.count = count;
        }

        public AutoClose(AlertDialog muted) {
            d = muted;
            button = d.getButton(DialogInterface.BUTTON_NEUTRAL);
            Window w = d.getWindow();
            touchListener(w);
        }

        @SuppressWarnings("RestrictedApi")
        public void touchListener(final Window w) {
            final Window.Callback c = w.getCallback();
            w.setCallback(new WindowCallbackWrapper(c) {
                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {
                    onUserInteraction();
                    return c.dispatchKeyEvent(event);
                }

                @Override
                public boolean dispatchTouchEvent(MotionEvent event) {
                    Rect rect = PopupWindowCompat.getOnScreenRect(w.getDecorView());
                    if (rect.contains((int) event.getRawX(), (int) event.getRawY()))
                        onUserInteraction();
                    return c.dispatchTouchEvent(event);
                }
            });
        }

        public void onUserInteraction() {
            Button b = d.getButton(DialogInterface.BUTTON_NEUTRAL);
            b.setVisibility(View.GONE);
            handler.removeCallbacks(this);
        }

        @Override
        public void run() {
            if (isFinishing())
                return;
            if (!d.isShowing())
                return;
            if (count <= 0) {
                d.dismiss();
                return;
            }
            button.setText(d.getContext().getString(R.string.auto_close, count));
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(v -> {
            });
            count--;
            handler.postDelayed(this, 1000);
        }
    }

    class RecordingReceiver extends BluetoothReceiver {
        @Override
        public void onConnected() {
            if (recording.thread == null) {
                if (isRecordingReady())
                    startRecording();
            }
        }

        @Override
        public void onDisconnected() {
            if (recording.thread != null) {
                stopRecording(getString(R.string.hold_by_bluetooth));
                super.onDisconnected();
            }
        }

        @Override
        public void onReceive(final Context context, Intent intent) {
            super.onReceive(context, intent);
            String a = intent.getAction();
            if (a == null)
                return;
            if (a.equals(PAUSE_BUTTON)) {
                pauseButton();
                return;
            }
            if (a.equals(ACTION_FINISH_RECORDING)) {
                done.performClick();
                return;
            }
            Headset.handleIntent(headset, intent);
        }
    }

    class PhoneStateChangeListener extends PhoneStateListener {
        public boolean wasRinging;
        public boolean pausedByCall;

        @Override
        public void onCallStateChanged(int s, String incomingNumber) {
            switch (s) {
                case TelephonyManager.CALL_STATE_RINGING:
                    wasRinging = true;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    wasRinging = true;
                    if (recording.thread != null) {
                        stopRecording(getString(R.string.hold_by_call));
                        pausedByCall = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (pausedByCall) {
                        if (receiver.isRecordingReady())
                            startRecording();
                    }
                    wasRinging = false;
                    pausedByCall = false;
                    break;
            }
        }
    }

    public String toMessage(Throwable e) {
        return ErrorDialog.toMessage(e);
    }

    public void Error(Throwable e) {
        Log.e(TAG, "error", e);
        Error(recording.storage.getTempRecording(), toMessage(e));
    }

    public void Error(File in, Throwable e) {
        Log.e(TAG, "error", e);
        Error(in, toMessage(e));
    }

    public void Error(File in, String msg) {
        ErrorDialog builder = new ErrorDialog(this, msg);
        builder.setOnCancelListener(dialog -> finish());
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> finish());
        if (in.length() > 0) {
            builder.setNeutralButton(R.string.save_as_wav, (dialog, which) -> {
                final OpenFileDialog d = new OpenFileDialog(RecordingActivity.this, OpenFileDialog.DIALOG_TYPE.FOLDER_DIALOG);
                d.setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
                    File to = new File(d.getCurrentPath(), Storage.getName(RecordingActivity.this, recording.targetUri));
                    recording.targetUri = Uri.fromFile(to);
                    EncodingService.saveAsWAV(RecordingActivity.this, recording.storage.getTempRecording(), to, recording.getInfo());
                });
                d.show();
            });
        }
        builder.show();
    }

    @Override
    public int getAppTheme() {
        return AudioApplication.getTheme(this, R.style.RecThemeLight_NoActionBar, R.style.RecThemeDark_NoActionBar);
    }

    public static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set Language
        SystemUtil.setLocale(getBaseContext());


        showLocked(getWindow());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_recording);

        AppOpenManager.getInstance().disableAppResumeWithActivity(RecordingActivity.class);

        pitch = findViewById(R.id.recording_pitch);
        time = findViewById(R.id.recording_time);
        state = findViewById(R.id.recording_state);
        title = findViewById(R.id.txtAudioTitle);

        screen = new ScreenReceiver();
        screen.registerReceiver(this);

        receiver = new RecordingReceiver();
        receiver.filter.addAction(PAUSE_BUTTON);
        receiver.filter.addAction(ACTION_FINISH_RECORDING);
        receiver.registerReceiver(this);

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        if (shared.getBoolean(AudioApplication.PREFERENCE_CALL, false)) {
            TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(pscl, PhoneStateListener.LISTEN_CALL_STATE);
        }

        final View cancel = findViewById(R.id.recording_cancel);
        cancel.setOnClickListener(v -> {
            cancel.setClickable(false);
            cancelDialog(() -> {
                stopRecording();
                if (shared.getBoolean(AudioApplication.PREFERENCE_FLY, false)) {
                    try {
                        if (recording.e != null) {
                            recording.e.close();
                            recording.e = null;
                        }
                    } catch (RuntimeException e) {
                        Error(e);
                    }
                    try {
                        Storage.delete(RecordingActivity.this, recording.targetUri);
                    } catch (Exception e) {
                    }
                }
                Storage.delete(recording.storage.getTempRecording());
                ivCancel();
            }, () -> cancel.setClickable(true));
        });

        pause = findViewById(R.id.recording_pause);
        pause.setOnClickListener(v -> pauseButton());

        done = findViewById(R.id.recording_done);

        done.setOnClickListener(v -> {
            String msg;
            msg = getString(R.string.recording_status_encoding);
            stopRecording(msg);
            done.setClickable(false);
            doneDialog(() -> {
                String str = recording.targetUri.toString();
                int one = str.lastIndexOf('/');
                int two = str.lastIndexOf('.');
                String subStr = str.substring(one + 1, two);
                String strTitle = title.getText().toString().trim();
                int numberStrTitle = strTitle.lastIndexOf('.');
                String subStrTitle = strTitle.substring(0, numberStrTitle);
                String edtStr = edt.getText().toString().trim();

                if (edtStr.equals("")) {
                    Toast.makeText(this, "This field must not be blank!", Toast.LENGTH_SHORT).show();
                } else if (edtStr.length() > 25) {
                    Toast.makeText(this, "No more 25 characters", Toast.LENGTH_SHORT).show();
                } else {
                    if (edtStr.contains("?") || edtStr.contains(":") || edtStr.contains("<") || edtStr.contains(">")
                            || edtStr.contains("\\") || edtStr.contains("/")) {
                        Toast.makeText(this, "File name can not contain the following characters: </ \\ :*?>", Toast.LENGTH_SHORT).show();
                    } else if (edtStr.equals(strTitle)) {
                        Toast.makeText(RecordingActivity.this, "File name already exists!", Toast.LENGTH_SHORT).show();
                    } else {
                        recording.targetUri = Uri.parse(recording.targetUri.toString().replace(subStr, edtStr));
                        Log.e("xxxx edtStr", edtStr.toString());
                        Log.e("xxxx recording", recording.targetUri.toString().toString());
                        try {
                            if (!new File(recording.targetUri.getPath()).exists()) {
                                encoding(() -> {
                                    if (recordSoundIntent != null) {
                                        Log.e("vxxxx", StorageProvider.getProvider().toString());
                                        recordSoundIntent.setDataAndType(StorageProvider.getProvider().share(recording.targetUri), Storage.getTypeByExt(Storage.getExt(RecordingActivity.this, recording.targetUri)));
                                        FileProvider.grantPermissions(RecordingActivity.this, recordSoundIntent, FileProvider.RW);
                                    }
                                    showAdsAndFinish(new File(recording.targetUri.getPath()).getName());
                                });

                            } else {
                                Toast.makeText(RecordingActivity.this, "File name already exists!", Toast.LENGTH_SHORT).show();
                            }
                        } catch (RuntimeException e) {
                            Error(e);
                        }
                    }
                }
            }, () -> done.setClickable(true));
        });

        onCreateRecording();

        Intent intent = getIntent();
        String a = intent.getAction();
        if (a != null && a.equals(START_PAUSE)) {
            start = false;
            stopRecording(getString(R.string.recording_status_pause));
        }
        onIntent(intent);
    }

    private void ivCancel() {
        if (recordSoundIntent != null) {
            if (recordSoundIntent.getData() == null)
                setResult(RESULT_CANCELED);
            else
                setResult(Activity.RESULT_OK, recordSoundIntent);
            super.finish();
        } else {
            super.finish();
            MainActivity.startActivity(this);
        }
    }

    private void showAdsAndFinish(String fileName) {

        startActivity(new Intent(RecordingActivity.this, SuccessActivity.class).putExtra("FILE_NAME", fileName));
        RecordingActivity.this.finish();

    }

    public void onCreateRecording() {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = getIntent();
        String a = intent.getAction();

        AudioApplication app = AudioApplication.from(this);
        try {
            if (app.recording == null) {
                Uri abc = null;
                Uri targetUri = null;
                Storage storage = new Storage(this);
                if (a != null && a.equals(MediaStore.Audio.Media.RECORD_SOUND_ACTION)) {
                    if (storage.recordingPending()) {
                        String file = shared.getString(AudioApplication.PREFERENCE_TARGET, null);
                        if (file != null) // else pending recording comes from intent recording, resume recording
                            throw new RuntimeException("finish pending recording first");
                    }
                    targetUri = storage.getNewIntentRecording();
                    recordSoundIntent = new Intent();
                } else {
                    if (storage.recordingPending()) {
                        String file = shared.getString(AudioApplication.PREFERENCE_TARGET, null);
                        if (file != null) {
                            if (file.startsWith(ContentResolver.SCHEME_CONTENT))
                                targetUri = Uri.parse(file);
                            else if (file.startsWith(ContentResolver.SCHEME_FILE))
                                targetUri = Uri.parse(file);
                            else
                                targetUri = Uri.fromFile(new File(file));
                        }
                    }
                    if (targetUri == null)
                        targetUri = storage.getNewFile();
                    SharedPreferences.Editor editor = shared.edit();
                    editor.putString(AudioApplication.PREFERENCE_TARGET, targetUri.toString());
                    editor.apply();
                }
                Log.d(TAG, "create recording at: " + targetUri);
                app.recording = new RecordingStorage(this, pitch.getPitchTime(), targetUri);
            }
            recording = app.recording;
            synchronized (recording.handlers) {
                recording.handlers.add(handler);
            }
        } catch (RuntimeException e) {
//            Toast.Error(this, e);
            finish();
            return;
        }
        sendBroadcast(new Intent(START_RECORDING));
        title.setText(Storage.getName(this, recording.targetUri));
        recording.updateBufferSize(false);
//        edit(false, false);
        loadSamples();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        onIntent(intent);
    }

    public void onIntent(Intent intent) {
        String a = intent.getAction();
        if (a != null && a.equals(ERROR))
            muted = new ErrorDialog(this, intent.getStringExtra("msg")).setTitle(intent.getStringExtra("title")).show();
    }

    void loadSamples() {
        File f = recording.storage.getTempRecording();
        if (!f.exists()) {
            recording.samplesTime = 0;
            updateSamples(recording.samplesTime);
            return;
        }

        RawSamples rs = new RawSamples(f);
        recording.samplesTime = rs.getSamples() / Sound.getChannels(this);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int count = pitch.getMaxPitchCount(metrics.widthPixels);

        short[] buf = new short[count * recording.samplesUpdateStereo];
        long cut = recording.samplesTime * Sound.getChannels(this) - buf.length;

        if (cut < 0)
            cut = 0;

        rs.open(cut, buf.length);
        int len = rs.read(buf);
        rs.close();

        if (recording.samplesUpdateStereo == 0) recording.samplesUpdateStereo = 1;
        pitch.clear(cut / recording.samplesUpdateStereo);
        int lenUpdate = len / recording.samplesUpdateStereo * recording.samplesUpdateStereo; // cut right overs (leftovers from right)
        for (int i = 0; i < lenUpdate; i += recording.samplesUpdateStereo) {
            double dB = RawSamples.getDB(buf, i, recording.samplesUpdateStereo);
            pitch.add(dB);
        }
        updateSamples(recording.samplesTime);

        int diff = len - lenUpdate;
        if (diff > 0) {
            recording.dbBuffer = ShortBuffer.allocate(recording.samplesUpdateStereo);
            recording.dbBuffer.put(buf, lenUpdate, diff);
        }
    }

    void pauseButton() {
        if (recording.thread != null) {
            receiver.errors = false;
            stopRecording(getString(R.string.recording_status_pause));
            receiver.stopBluetooth();
            headset(true, false);
        } else {
            receiver.errors = true;
            receiver.stopBluetooth(); // reset bluetooth
//            editCut();
            if (receiver.isRecordingReady()) {
                startRecording();
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        recording.updateBufferSize(false);

        if (start) { // start once
            start = false;
            if (Storage.permitted(this, PERMISSIONS_AUDIO, RESULT_START)) { // audio perm
                if (receiver.isRecordingReady())
                    startRecording();
                else
                    stopRecording(getString(R.string.hold_by_bluetooth));
            }
        }

        boolean r = recording.thread != null;

        RecordingService.startService(this, Storage.getName(this, recording.targetUri), r, duration);

        if (r) {
            pitch.record();
        }

        if (progress != null)
            progress.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        recording.updateBufferSize(true);
        pitch.stop();
        if (progress != null)
            progress.onPause();
    }

    @SuppressLint({"ClickableViewAccessibility", "UseCompatLoadingForDrawables"})
    void stopRecording(String status) {
        setState(status);
        pause.setImageResource(R.drawable.ic_voice);
        pause.setBackground(getDrawable(R.drawable.ripple_image_button_gradient));
        pause.setContentDescription(getString(R.string.record_button));

        stopRecording();

        RecordingService.startService(this, Storage.getName(this, recording.targetUri), false, duration);

    }

    void stopRecording() {
        if (recording != null) // not possible, but some devices do not call onCreate
            recording.stopRecording();
        AudioApplication.from(this).recording = null;
        handler.removeCallbacks(receiver.connected);
        pitch.stop();
        sendBroadcast(new Intent(STOP_RECORDING));
    }

    void setState(String s) {
        state.setText(s);
    }

    @Override
    public void onBackPressed() {
        cancelDialog(() -> {
            stopRecording();
            Storage.delete(recording.storage.getTempRecording());
            ivCancel();
        }, null);
    }

    void cancelDialog(final Runnable run, final Runnable cancel) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView = inflater.inflate(R.layout.alert_dialog_custom, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setView(dialogView);

        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button btnOk = dialogView.findViewById(com.github.axet.audiolibrary.R.id.btnOk);
        Button btnCancel = dialogView.findViewById(com.github.axet.audiolibrary.R.id.btnCancel);

        Objects.requireNonNull(btnOk).setOnClickListener(v -> {
            alertDialog.cancel();
            run.run();
        });

        Objects.requireNonNull(btnCancel).setOnClickListener(v -> alertDialog.cancel());

        alertDialog.setOnDismissListener(dialog -> {
            if (cancel != null)
                cancel.run();
        });
        alertDialog.show();
    }

    void doneDialog(final Runnable run, final Runnable cancel) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView = inflater.inflate(R.layout.alert_dialog_custom_done, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setView(dialogView);

        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Button btnOk = dialogView.findViewById(com.github.axet.audiolibrary.R.id.btnOk);
        Button btnCancel = dialogView.findViewById(com.github.axet.audiolibrary.R.id.btnCancel);
        edt = dialogView.findViewById(com.github.axet.audiolibrary.R.id.edtTitle);
        String strTitle = title.getText().toString();
        int numberStrTitle = strTitle.lastIndexOf('.');
        String subStrTitle = strTitle.substring(0, numberStrTitle);
        edt.setText(subStrTitle);
        Objects.requireNonNull(btnOk).setOnClickListener(v -> {
            alertDialog.cancel();
            run.run();
        });

        Objects.requireNonNull(btnCancel).setOnClickListener(v -> alertDialog.cancel());

        alertDialog.setOnDismissListener(dialog -> {
            if (cancel != null)
                cancel.run();
        });
        alertDialog.show();
    }
    //

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopRecording();
        receiver.stopBluetooth();
        headset(false, false);

        if (muted != null) {
            muted.dismiss();
            muted = null;
        }

        if (screen != null) {
            screen.close();
            screen = null;
        }

        if (receiver != null) {
            receiver.close();
            receiver = null;
        }

        if (progress != null) {
            progress.close();
            progress = null;
        }

        RecordingService.stopRecording(this);
        ControlsService.startIfEnabled(this);

        if (pscl != null) {
            TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(pscl, PhoneStateListener.LISTEN_NONE);
            pscl = null;
        }

        if (play != null) {
            play.release();
            play = null;
        }

        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = shared.edit();
        editor.remove(AudioApplication.PREFERENCE_TARGET);
        editor.apply();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    void startRecording() {
        try {
            pause.setImageResource(R.drawable.ic_pause_recorder);
            pause.setBackground(getDrawable(R.drawable.ripple_image_button_gradient_v2));
            pause.setContentDescription(getString(R.string.pause_button));

            pitch.record();

            setState(getString(R.string.recording_status_recording));

            headset(true, true);

            recording.startRecording();

            RecordingService.startService(this, Storage.getName(this, recording.targetUri), true, duration);
            ControlsService.hideIcon(this);
        } catch (RuntimeException e) {
            finish();
        }
    }

    void updateSamples(long samplesTime) {
        if (recording.sampleRate == 0) recording.sampleRate = 1;
        long ms = samplesTime / recording.sampleRate * 1000;
        duration = AudioApplication.formatDuration(this, ms);
        time.setText(duration);
        boolean r = recording.thread != null;
        if (r)
            setState(getString(R.string.recording_status_recording)); // update 'free' during recording
        RecordingService.startService(this, Storage.getName(this, recording.targetUri), r, duration);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RESULT_START:
                if (Storage.permitted(this, permissions)) {
                    if (receiver.isRecordingReady())
                        startRecording();
                } else {
                    try {
                        Toast.makeText(this, R.string.not_permitted, Toast.LENGTH_SHORT).show();
                    } catch (Exception exception) {
                    }
                    finish();
                }
        }
    }

    void encoding(final Runnable done) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(RecordingActivity.this);
        if (shared.getBoolean(AudioApplication.PREFERENCE_FLY, false)) { // keep encoder open if encoding on fly enabled
            try {
                if (recording.e != null) {
                    recording.e.close();
                    recording.e = null;
                }
            } catch (RuntimeException e) {
                Error(e);
                return;
            }
        }

        final Runnable last = () -> {
            SharedPreferences.Editor edit = shared.edit();
            edit.putString(AudioApplication.PREFERENCE_LAST, Storage.getName(RecordingActivity.this, recording.targetUri));
            edit.apply();
            done.run();
        };

        final File in = recording.storage.getTempRecording();

        if (!in.exists() || in.length() == 0) {
            last.run();
            return;
        }

        if (recordSoundIntent != null) {
            if (progress != null)
                progress.close();
            progress = new MainActivity.ProgressHandler(Looper.getMainLooper()) {
                @Override
                public void onUpdate(Uri targetUri, RawSamples.Info info) {
                    super.onUpdate(targetUri, info);
                    if (progress == null) {
                        show(targetUri, info);
                        progress.setCancelable(false);
                    }
                }

                @Override
                public void onDone(Uri targetUri) {
                    super.onDone(targetUri);
                    if (targetUri.equals(recording.targetUri))
                        done.run();
                }

                @Override
                public void onExit() {
                    super.onExit();
                    done.run();
                }

                @Override
                public void onError(File in, RawSamples.Info info, Throwable e) {
                    if (in.equals(encoding))
                        RecordingActivity.this.Error(encoding, e); // show error for current encoding
                    else
                        Error(in, info, e); // show error for any encoding
                }
            };
            progress.registerReceiver(this);
        } else {
            done.run();
        }
        encoding = EncodingService.startEncoding(this, in, recording.targetUri, recording.getInfo());
    }

//    @Override
//    public void finish() {
//        if (recordSoundIntent != null) {
//            if (recordSoundIntent.getData() == null)
//                setResult(RESULT_CANCELED);
//            else
//                setResult(Activity.RESULT_OK, recordSoundIntent);
//            super.finish();
//        } else {
//            super.finish();
//            MainActivity.startActivity(this);
//        }
//    }

    public void headset(boolean b, final boolean recording) {
        if (b) {
            if (headset == null) {
                headset = new Headset() {
                    {
                        actions = Headset.ACTIONS_MAIN;
                    }

                    @Override
                    public void onPlay() {
                        pauseButton();
                    }

                    @Override
                    public void onPause() {
                        pauseButton();
                    }

                    @Override
                    public void onStop() {
                        pauseButton();
                    }
                };
                headset.create(this, RecordingActivity.RecordingReceiver.class);
            }
            headset.setState(recording);
        } else {
            if (headset != null) {
                headset.close();
                headset = null;
            }
        }
    }

}
