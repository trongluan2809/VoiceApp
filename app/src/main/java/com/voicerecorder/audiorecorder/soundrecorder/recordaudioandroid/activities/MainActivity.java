package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import static com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.Common.EXTRA_AUDIO_URI;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.amazic.ads.util.Admod;
import com.amazic.ads.util.AppOpenManager;
import com.github.axet.androidlibrary.activities.AppCompatThemeActivity;
import com.github.axet.androidlibrary.widgets.ErrorDialog;
import com.github.axet.androidlibrary.widgets.OpenFileDialog;
import com.github.axet.androidlibrary.widgets.SearchView;
import com.github.axet.audiorecorder.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.Common;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.AudioApplication;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.EncodingStorage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.Recordings;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.Storage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.services.ControlsService;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.services.EncodingService;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.services.RecordingService;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;
import com.voicerecorder.axet.audiolibrary.app.MainApplication;
import com.voicerecorder.axet.audiolibrary.app.RawSamples;
import com.voicerecorder.axet.audiolibrary.encoders.FormatWAV;

import org.json.JSONException;

import java.io.File;
import java.util.Locale;

public class MainActivity extends AppCompatThemeActivity {
    public final static String TAG = MainActivity.class.getSimpleName();
    public static final int RESULT_PERMS = 1;
    private SharedPreferences shared;
    private FloatingActionButton fab, folder;
    private RecyclerView rvRecorded;
    private Recordings recordings;
    private Storage storage;
    private ScreenReceiver receiver;
    private EncodingDialog encoding;
    private LinearLayout llEmpty;

    private static boolean isOpenResume = true;

    AlertDialog alertDialog;

    private ImageView ivListAll, ivListStar, ivSelectStar, ivSelectAll;
    private ImageButton ivRecord;


    public static void startActivity(Context context) {
        context.startActivity(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
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
        Common.changeColor(this);
        setContentView(R.layout.activity_main);

        // Set Language
        SystemUtil.setLocale(getBaseContext());

        ivListAll = findViewById(R.id.iv_list_all);
        ivSelectAll = findViewById(R.id.iv_select_all);
        ivSelectStar = findViewById(R.id.iv_select_star);
        ivListStar = findViewById(R.id.iv_list_star);
        ivRecord = findViewById(R.id.iv_record);


        if (!isPermissionGranted()) {
            takePermission();
        }
        Admod.getInstance().setOpenActivityAfterShowInterAds(true);
        shared = PreferenceManager.getDefaultSharedPreferences(this);
        storage = new Storage(this);

        String path = shared.getString(MainApplication.PREFERENCE_STORAGE, "");

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more_vertical));
        toolbar.setTitle(getString(R.string.app_name2));
        setSupportActionBar(toolbar);

        fab = findViewById(R.id.fab);

        ivRecord.setOnClickListener(view -> {
            recordings.select(-1);
            finish();
            RecordingActivity.startActivity(MainActivity.this, false);
        });

        rvRecorded = findViewById(R.id.rvRecorded);
        recordings = new Recordings(this, rvRecorded) {
            @Override
            public boolean getPrefCall() {
                final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
                return shared.getBoolean(AudioApplication.PREFERENCE_CALL, false);
            }

            @Override
            public void showDialog(AlertDialog.Builder e) {
                AlertDialog d = e.create();
                showDialogLocked(d.getWindow());
                d.show();
            }
        };


        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager2 = findViewById(R.id.viewPager2);
        RvRecordedAdapter rvAdapter = new RvRecordedAdapter(recordings);
        viewPager2.setAdapter(rvAdapter);

        ivSelectAll.setVisibility(View.VISIBLE);
        ivListAll.setImageResource(R.drawable.ic_list_all);

        ivSelectStar.setVisibility(View.INVISIBLE);
        ivListStar.setImageResource(R.drawable.ic_list_star_01);

        viewPager2.setCurrentItem(0);

        ivListAll.setOnClickListener(v -> {
            recordings.setRvFilter(true);

            ivSelectAll.setVisibility(View.VISIBLE);
            ivListAll.setImageResource(R.drawable.ic_list_all);

            ivSelectStar.setVisibility(View.INVISIBLE);
            ivListStar.setImageResource(R.drawable.ic_list_star_01);

            viewPager2.setCurrentItem(0);
        });

        ivListStar.setOnClickListener(v -> {
            recordings.setRvFilter(false);

            ivSelectAll.setVisibility(View.INVISIBLE);
            ivListAll.setImageResource(R.drawable.ic_list_all_01);

            ivSelectStar.setVisibility(View.VISIBLE);
            ivListStar.setImageResource(R.drawable.ic_list_star);

            viewPager2.setCurrentItem(1);
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 0) {
                    recordings.setRvFilter(true);
                    ivSelectAll.setVisibility(View.VISIBLE);
                    ivListAll.setImageResource(R.drawable.ic_list_all);

                    ivSelectStar.setVisibility(View.INVISIBLE);
                    ivListStar.setImageResource(R.drawable.ic_list_star_01);
                } else {
                    recordings.setRvFilter(false);
                    ivSelectAll.setVisibility(View.INVISIBLE);
                    ivListAll.setImageResource(R.drawable.ic_list_all_01);

                    ivSelectStar.setVisibility(View.VISIBLE);
                    ivListStar.setImageResource(R.drawable.ic_list_star);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

//        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
//            if (position == 0) {
//                tab.setText(getString(R.string.all));
//                tab.setIcon(R.drawable.ic_list);
//            } else {
//                tab.setText(getString(R.string.star));
//                tab.setIcon(R.drawable.ic_star);
//            }
//        }).attach();

        recordings.setRvFilter(true);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                recordings.setRvFilter(tab.getPosition() == 0);

                //add gan event
                Log.d("checkAll", recordings.toolbarFilterAll + "");
                if (recordings.toolbarFilterAll && recordings.sizeAll >= 3) {
                    Bundle bundleAll = new Bundle();
                    bundleAll.putString("clickAll", "clickTabAll");

                } else if (!recordings.toolbarFilterAll && recordings.sizeStar >= 2) {
                    Bundle bundleStar = new Bundle();
                    bundleStar.putString("clickStar", "clickTabStar");

                }
                //end
                // nó set true false để hiển thị
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        receiver = new ScreenReceiver() {
            @Override
            public void onScreenOff() {
                boolean p = storage.recordingPending();
                boolean c = shared.getBoolean(AudioApplication.PREFERENCE_CONTROLS, false);
                if (!p && !c)
                    return;
                super.onScreenOff();
            }
        };
        receiver.registerReceiver(this);
        encoding = new EncodingDialog(Looper.getMainLooper());
        encoding.registerReceiver(this);

        RecordingService.startIfPending(this);
        EncodingService.startIfPending(this);
        ControlsService.startIfEnabled(this);

        try {
            new Recordings.ExoLoader(this, false);
        } catch (Exception e) {
            Log.e(TAG, "error", e);
        }
    }


    //quang add
    void feedbackDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView = inflater.inflate(R.layout.alert_dialog_feedback, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setView(dialogView);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        EditText edtSub = dialogView.findViewById(R.id.edtSubject);
        EditText edtMes = dialogView.findViewById(R.id.edtMessage);
        Button btnSend = dialogView.findViewById(R.id.btnSend);
        String subStr = "[VoiceRecorder - TVApp] - User feedback";
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = "v20210725@gmail.com";
                String sub = edtSub.getText().toString();
                String mes = edtMes.getText().toString();
                StringBuilder body = new StringBuilder();
                body.append("-----------------------------------------\n");
                body.append(" Device: " + Build.MANUFACTURER + "- " + Build.MODEL + "(" + Build.DEVICE + ")" + "\n");
                body.append("-----------------------------------------\n");
                body.append(" Function you want: " + sub + "\n");
                body.append(" Detail about function: " + mes);

                //
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                String uriText =
                        "mailto:" + email + "?subject=" + subStr + "&body=" + body.toString();
                emailIntent.setData(Uri.parse(uriText.trim()));
                Uri uri = Uri.parse(uriText);
                Intent sendIntent = new Intent(Intent.ACTION_SENDTO);
                sendIntent.setData(uri);
                startActivity(Intent.createChooser(sendIntent, "Send Email"));

            }
        });
        alertDialog.show();

    }
    //

    void checkPending() {
        if (storage.recordingPending()) {
            finish();
            RecordingActivity.startActivity(this, true);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle resultBundle = intent.getBundleExtra("com.github.audiolibrary.BUNDLE");
        if (resultBundle == null) return;

        boolean isOpenTrimmer = resultBundle.getBoolean("android.intent.extra.BOOLEAN", false);
        Uri uri = resultBundle.getParcelable("com.github.audiolibrary.URI");
        if (isOpenTrimmer && uri != null) {
            Intent i = new Intent(this, TrimAudioActivity.class);
            Bundle bundle = new Bundle();
            bundle.putParcelable(EXTRA_AUDIO_URI, uri);
            i.putExtra(Common.EXTRA_BUNDLE, bundle);
            showAdsAndStartTrimActivity(i);
        }
    }

    private void showAdsAndStartTrimActivity(Intent intent) {

        startActivity(intent);

    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }

        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem search = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) search.getActionView();
        searchView.setQueryHint(getString(R.string.menu_search) + "...");
        ((EditText) searchView.findViewById(androidx.appcompat.R.id.search_src_text))
                .setHintTextColor(ResourcesCompat.getColor(getResources(), R.color.white_A50, null));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                recordings.search(newText);
                return false;
            }
        });
        searchView.setOnCloseListener(() -> {
            recordings.searchClose();
            return true;
        });

        recordings.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (recordings.onOptionsItemSelected(this, item))
            return true;
        switch (item.getItemId()) {
            case R.id.action_settings:
                showSettingsActivity();
                return true;
            /*case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;*/
            case R.id.action_feedback:
                AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity.class);

                feedbackDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSettingsActivity() {
        Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        encoding.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (com.voicerecorder.axet.audiolibrary.app.Storage.openResume) {
            AppOpenManager.getInstance().enableAppResume();
            com.voicerecorder.axet.audiolibrary.app.Storage.openResume = false;
        }
        if (getEventUpdate()) {
            Bundle bundleStar = new Bundle();
            bundleStar.putString("update_data", "update_Data");

            setEventUpdate(false);
            Log.e("xxxx", "update_data mFirebaseAnalytics");
        }
        AppOpenManager.getInstance().enableAppResumeWithActivity(MainActivity.class);

        encoding.onResume();
        invalidateOptionsMenu();

        try {
            storage.migrateLocalStorage();
        } catch (Exception e) {
            ErrorDialog.Error(this, e);
        }

        final String last = shared.getString(AudioApplication.PREFERENCE_LAST, "");
        recordings.load(!last.isEmpty(), null);

        if (recordings.getItemCount() == 0 && llEmpty != null) {
            runOnUiThread(() -> {
                llEmpty.setVisibility(View.GONE);
            });
        }

        checkPending();

        updateHeader();

    }

    int getLastRecording(String last) {
        for (int i = 0; i < recordings.getItemCount(); i++) {
            Storage.RecordingUri f = recordings.getItem(i);
            if (f.name.equals(last)) {
                SharedPreferences.Editor edit = shared.edit();
                edit.putString(AudioApplication.PREFERENCE_LAST, "");
                edit.apply();
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RESULT_PERMS:
                if (Storage.permitted(MainActivity.this, permissions)) {
                    try {
                        storage.migrateLocalStorage();
                    } catch (RuntimeException e) {
                        ErrorDialog.Error(MainActivity.this, e);
                    }
                    recordings.load(false, null);
                    checkPending();
                } else {
                    Toast.makeText(this, getText(R.string.notper), Toast.LENGTH_SHORT).show();
                }
                break;
            case 101:
                boolean readExternalStorage = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                boolean writeExternalStorage = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                if (readExternalStorage && writeExternalStorage) {
                    try {
                        Toast.makeText(this, getString(R.string.per_granted), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.per_granted), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle(getString(R.string.per_granted));
                    alertDialog.setCancelable(false);
                    alertDialog.setMessage(getString(R.string.toast_per));
                    alertDialog.setButton(-1, (CharSequence) getString(R.string.go_to_setting), new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        public void onClick(DialogInterface dialogInterface, int i) {
                            AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity.class);
                            isOpenResume = false;
                            alertDialog.dismiss();
                            requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"}, 101);
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getApplicationContext().getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    });
                    alertDialog.show();
                }
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        recordings.close();
        receiver.close();
        encoding.close();
    }

    void updateHeader() {
        Uri uri = storage.getStoragePath();
        long free = Storage.getFree(this, uri);
        long sec = Storage.average(this, free);
        TextView text = findViewById(R.id.space_left);
        text.setText(AudioApplication.formatFree(this, free, sec));
    }


    public class RvRecordedAdapter extends RecyclerView.Adapter<RvRecordedAdapter.ViewHolder> {
        private final Recordings recordings;

        public RvRecordedAdapter(Recordings recordings) {
            this.recordings = recordings;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fl_recorded, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.rvList.setAdapter(recordings.empty);
            holder.rvList.setLayoutManager(new LinearLayoutManager(holder.rvList.getContext()));
            holder.rvList.addItemDecoration(new DividerItemDecoration(holder.rvList.getContext(), DividerItemDecoration.VERTICAL));

            recordings.setEmptyView(holder.llEmpty);
            if (position == 0) MainActivity.this.llEmpty = holder.llEmpty;
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            RecyclerView rvList;
            AppCompatImageView imgEmpty;
            LinearLayout llEmpty;

            public ViewHolder(View view) {
                super(view);
                rvList = view.findViewById(R.id.rvList);
                imgEmpty = view.findViewById(R.id.imgEmpty);
                llEmpty = view.findViewById(R.id.ll_empty);
            }
        }
    }

    public static class SpeedInfo extends com.github.axet.wget.SpeedInfo {
        public Sample getLast() {
            if (samples.size() == 0)
                return null;
            return samples.get(samples.size() - 1);
        }

        public long getDuration() {
            if (start == null || getRowSamples() < 2)
                return 0;
            return getLast().now - start.now;
        }
    }

    public static class ProgressEncoding extends ProgressDialog {
        public static int DURATION = 5000;
        public long pause;
        public long resume;
        public long samplesPause; // encoding progress on pause
        public long samplesResume; // encoding progress on resume
        SpeedInfo current;
        SpeedInfo foreground;
        SpeedInfo background;
        LinearLayout view;
        View speed;
        TextView text;
        View warning;
        RawSamples.Info info;

        public ProgressEncoding(Context context, RawSamples.Info info) {
            super(context);
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            setIndeterminate(false);
            setMax(100);
            setTitle(R.string.encoding_title);
            this.info = info;
        }

        @Override
        public void setView(View v) {
            view = new LinearLayout(getContext());
            view.setOrientation(LinearLayout.VERTICAL);
            super.setView(view);
            view.addView(v);
            LayoutInflater inflater = LayoutInflater.from(getContext());
            speed = inflater.inflate(R.layout.encoding_speed, view);
            text = speed.findViewById(R.id.speed);
        }

        public void onPause(long cur) {
            pause = System.currentTimeMillis();
            samplesPause = cur;
            resume = 0;
            samplesResume = 0;
            if (background == null)
                background = new SpeedInfo();
            background.start(cur);
        }

        public void onResume(long cur) {
            resume = System.currentTimeMillis();
            samplesResume = cur;
            if (foreground == null)
                foreground = new SpeedInfo();
            foreground.start(cur);
        }

        public void setProgress(long cur, long total) {
            long max = total / info.hz / info.channels;
            setMax(max > Integer.MAX_VALUE ? (int) (max / (Long.MAX_VALUE / Integer.MAX_VALUE)) : (int) max);
            if (current == null) {
                current = new SpeedInfo();
                current.start(cur);
            } else {
                current.step(cur);
            }
            if (pause == 0 && resume == 0) { // foreground
                if (foreground == null) {
                    foreground = new SpeedInfo();
                    foreground.start(cur);
                } else {
                    foreground.step(cur);
                }
            }
            if (pause != 0 && resume == 0) // background
                background.step(cur);
            if (pause != 0 && resume != 0) { // resumed from background
                long diffreal = resume - pause; // real time
                long diffenc = (samplesResume - samplesPause) * 1000 / info.hz / info.channels; // encoding time
                if (diffreal > 0 && diffenc < diffreal && warning == null) { // paused
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    warning = inflater.inflate(R.layout.optimization, view);
                }
                if (diffreal > 0 && diffenc >= diffreal && warning == null && foreground != null && background != null) {
                    if (foreground.getDuration() > DURATION && background.getDuration() > DURATION) {
                        long r = foreground.getAverageSpeed() / background.getAverageSpeed();
                        if (r > 1) { // slowed down by twice or more
                            LayoutInflater inflater = LayoutInflater.from(getContext());
                            warning = inflater.inflate(R.layout.slow, view);
                        }
                    }
                }
            }
            text.setText(AudioApplication.formatSize(getContext(), current.getAverageSpeed() * info.bps / Byte.SIZE) + getContext().getString(R.string.per_second));
            super.setProgress(total == 0 ? 0 : (int) (cur * getMax() / total));
        }
    }

    public static class ProgressHandler extends Handler {
        Context context;
        ProgressEncoding progress;
        long cur;
        long total;
        Storage storage;
        EncodingStorage encodings;

        public ProgressHandler(Looper looper) {
            super(looper);
        }

        public void registerReceiver(Context context) {
            this.context = context;
            storage = new Storage(context);
            encodings = AudioApplication.from(context).encodings;
            synchronized (encodings.handlers) {
                encodings.handlers.add(this);
            }
        }

        public void close() {
            synchronized (encodings.handlers) {
                encodings.handlers.remove(this);
                removeCallbacksAndMessages(null);
            }
        }

        public String printEncodings(Uri targetUri) {
            final long progress = cur * 100 / total;
            String p = " (" + progress + "%)";
            String str = "";
            for (File f : encodings.keySet()) {
                EncodingStorage.Info n = encodings.get(f);
                String name = Storage.getName(context, n.targetUri);
                str += "- " + name;
                if (n.targetUri.equals(targetUri))
                    str += p;
                str += "\n";
            }
            str = str.trim();
            return str;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == EncodingStorage.UPDATE) {
                Intent intent = (Intent) msg.obj;
                cur = intent.getLongExtra("cur", -1);
                total = intent.getLongExtra("total", -1);
                final Uri targetUri = intent.getParcelableExtra("targetUri");
                final RawSamples.Info info;
                try {
                    info = new RawSamples.Info(intent.getStringExtra("info"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                if (progress != null)
                    progress.setProgress(cur, total);
                onUpdate(targetUri, info);
            }
            if (msg.what == EncodingStorage.DONE) {
                Intent intent = (Intent) msg.obj;
                final Uri targetUri = intent.getParcelableExtra("targetUri");
                if (progress != null) {
                    progress.dismiss();
                    progress = null;
                }
                onDone(targetUri);
            }
            if (msg.what == EncodingStorage.EXIT) {
                if (progress != null) {
                    progress.dismiss();
                    progress = null;
                }
                onExit();
            }
            if (msg.what == EncodingStorage.ERROR) {
                Intent intent = (Intent) msg.obj;
                if (progress != null) {
                    progress.dismiss();
                    progress = null;
                }
                File in = (File) intent.getSerializableExtra("in");
                RawSamples.Info info;
                try {
                    info = new RawSamples.Info(intent.getStringExtra("info"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                Throwable e = (Throwable) intent.getSerializableExtra("e");
                onError(in, info, e);
            }
        }

        public void onUpdate(Uri targetUri, RawSamples.Info info) {
        }

        public void onError(File in, RawSamples.Info info, Throwable e) {
            Error(in, info, e);
            RecordingService.startIfPending(context);
        }

        public void onExit() {
            hide();
            RecordingService.startIfPending(context);
        }

        public void onDone(Uri targetUri) {
            RecordingService.startIfPending(context);
        }

        public void show(Uri targetUri, RawSamples.Info info) {
            progress = new ProgressEncoding(context, info);
            progress.setMessage(".../" + Storage.getName(context, targetUri));
            progress.show();
            progress.setProgress(cur, total);
        }

        public void onPause() {
            if (progress != null)
                progress.onPause(cur);
        }

        public void onResume() {
            if (progress != null)
                progress.onResume(cur);
        }

        public void Error(final File in, final RawSamples.Info info, Throwable e) {
            ErrorDialog builder = new ErrorDialog(context, ErrorDialog.toMessage(e));
            builder.setOnCancelListener(dialog -> {
            });
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            });
            if (in.length() > 0) {
                builder.setNeutralButton(R.string.save_as_wav, (dialog, which) -> {
                    final OpenFileDialog d = new OpenFileDialog(context, OpenFileDialog.DIALOG_TYPE.FOLDER_DIALOG);
                    d.setPositiveButton(android.R.string.ok, (dialog1, which1) -> {
                        File to = storage.getNewFile(d.getCurrentPath(), FormatWAV.EXT);
                        EncodingService.saveAsWAV(context, in, to, info);
                    });
                    d.show();
                });
            }
            builder.show();
        }

        public void hide() {
            if (progress != null) {
                progress.dismiss();
                progress = null;
            }
        }
    }

    public class EncodingDialog extends ProgressHandler {
        Snackbar snackbar;

        public EncodingDialog(Looper looper) {
            super(looper);
        }

        @Override
        public void onUpdate(final Uri targetUri, final RawSamples.Info info) {
            super.onUpdate(targetUri, info);
            if (snackbar == null || !snackbar.isShownOrQueued()) {
                snackbar = Snackbar.make(fab, printEncodings(targetUri), Snackbar.LENGTH_LONG);
                snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);
                snackbar.getView().setOnClickListener(v -> {
                    show(targetUri, info);
                    EncodingService.startIfPending(context);
                });
            } else {
                snackbar.setText(printEncodings(targetUri));
            }
            snackbar.show();
        }

        @Override
        public void onDone(Uri targetUri) {
            super.onDone(targetUri);
            recordings.load(false, null);
            if (snackbar != null && snackbar.isShownOrQueued()) {
                snackbar.setText(printEncodings(targetUri));
                snackbar.setDuration(Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        }

        @Override
        public void hide() {
            super.hide();
            if (snackbar != null) {
                snackbar.dismiss();
                snackbar = null;
            }
        }
    }


    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private void takePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
    }

    private boolean isPermissionGranted() {

        int readExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int writeExternalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return readExternalStoragePermission == PackageManager.PERMISSION_GRANTED && writeExternalStoragePermission == PackageManager.PERMISSION_GRANTED;
    }

    // Vì ko gắn event đc trong lib nên đẩy ra biến Preferences để check và bắn event
    // true : có copy data
    // false: ko copy data
    public void setEventUpdate(boolean active) {
        SharedPreferences sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("updateData", active);
        editor.commit();
    }

    public boolean getEventUpdate() {
        SharedPreferences sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        boolean active = sharedPref.getBoolean("updateData", false);
        return active;
    }

    @Override
    public void onBackPressed() {
        int numberBack = getSharedPreferences("PREF_NAME", MODE_PRIVATE).getInt("NUMBER_BACK", 0);
        boolean isRated = getSharedPreferences("PREF_NAME", MODE_PRIVATE).getBoolean("KEY_RATED", false);
        if (!isRated) {
            if (numberBack == 1 || numberBack == 3 || numberBack == 5 || numberBack == 7 || numberBack == 9) {
                showNativeBack();
            } else {
                showNativeBack();
            }
        } else {
            showNativeBack();
        }

    }

    private void showNativeBack() {
        Dialog dialog = new Dialog(this);
        View viewRename = LayoutInflater.from(this).inflate(R.layout.dialog_go_back, null, false);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(viewRename);
        int w2 = (int) (getResources().getDisplayMetrics().widthPixels * 0.8);
        int h2 = ViewGroup.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setLayout(w2, h2);

        dialog.show();

        Button button = (Button) dialog.findViewById(R.id.bt_cancel);
        button.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                dialog.dismiss();
            }
        });
        Button button2 = (Button) dialog.findViewById(R.id.bt_yes);
        button2.setOnClickListener(new View.OnClickListener() {
            public final void onClick(View view) {
                finishAffinity();
            }
        });

    }
}