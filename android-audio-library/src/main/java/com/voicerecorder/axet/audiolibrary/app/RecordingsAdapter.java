package com.voicerecorder.axet.audiolibrary.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.amazic.ads.util.AppOpenManager;
import com.github.axet.androidlibrary.animations.ExpandItemAnimator;
import com.github.axet.androidlibrary.app.AssetsDexLoader;
import com.github.axet.androidlibrary.app.ProximityShader;
import com.github.axet.androidlibrary.preferences.AboutPreferenceCompat;
import com.github.axet.androidlibrary.services.StorageProvider;
import com.github.axet.androidlibrary.sound.MediaPlayerCompat;
import com.github.axet.androidlibrary.sound.ProximityPlayer;
import com.github.axet.androidlibrary.widgets.CacheImagesAdapter;
import com.github.axet.androidlibrary.widgets.CacheImagesRecyclerAdapter;
import com.github.axet.androidlibrary.widgets.HeaderRecyclerAdapter;
import com.github.axet.audiolibrary.R;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.jakewharton.rxbinding4.view.RxView;
import com.voicerecorder.axet.audiolibrary.animations.RecordingAnimation;
import com.voicerecorder.axet.audiolibrary.encoders.Factory;
import com.voicerecorder.axet.audiolibrary.widgets.MoodbarView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RecordingsAdapter extends CacheImagesRecyclerAdapter<RecordingsAdapter.RecordingHolder> implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int REQUEST_OPEN_TRIMMER = 165;
    public static String TAG = RecordingsAdapter.class.getSimpleName();
    protected Handler handler;
    protected Context context;
    protected Storage storage;
    protected MediaPlayerCompat player;
    protected ProximityShader proximity;
    protected Runnable updatePlayer;
    protected int selected = -1;
    protected Thread thread;
    protected String filter;

    protected ViewGroup toolbar;
    public boolean toolbarFilterAll = true; // all or stars

    protected PhoneStateChangeListener pscl;

    protected Map<Uri, Storage.RecordingStats> cache = new ConcurrentHashMap<>();

    protected ArrayList<Storage.RecordingUri> items = new ArrayList<>();

    public ExpandItemAnimator animator;
    public HeaderRecyclerAdapter empty = new HeaderRecyclerAdapter(this);
    public int sizeAll,sizeStar;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive()");
            String a = intent.getAction();
            if (a.equals(Intent.ACTION_MEDIA_EJECT))
                handler.post(() -> load(false, null));
            if (a.equals(Intent.ACTION_MEDIA_MOUNTED))
                handler.post(() -> load(false, null));
            if (a.equals(Intent.ACTION_MEDIA_UNMOUNTED))
                handler.post(() -> load(false, null));
        }
    };

    public static long getDuration(final Context context, final Uri u) {

        final Object lock = new Object();
        final AtomicLong duration = new AtomicLong();
        final MediaPlayerCompat mp = MediaPlayerCompat.create(context, u);
        if (mp == null)
            return 0;
        mp.addListener(new MediaPlayerCompat.Listener() {
            @Override
            public void onReady() {
                synchronized (lock) {
                    duration.set(mp.getDuration());
                    lock.notifyAll();
                }
            }

            @Override
            public void onEnd() {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }

            @Override
            public void onError(Exception e) {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        });
        try {
            synchronized (lock) {
                mp.prepare();
                duration.set(mp.getDuration());
                if (duration.longValue() == 0)
                    lock.wait();
            }
            mp.release();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return duration.longValue();
    }

    public static File getCover(Context context, Storage.RecordingUri f) {
        return CacheImagesAdapter.cacheUri(context, f.uri);
    }

    public static Storage.RecordingStats getFileStats(Map<String, ?> prefs, Uri f) {
        String json = (String) prefs.get(MainApplication.getFilePref(f) + MainApplication.PREFERENCE_DETAILS_FS);
        if (json != null && !json.isEmpty())
            return new Storage.RecordingStats(json);
        return null;
    }

    public static void setFileStats(Context context, Uri f, Storage.RecordingStats fs) {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        String p = MainApplication.getFilePref(f) + MainApplication.PREFERENCE_DETAILS_FS;
        SharedPreferences.Editor editor = shared.edit();
        editor.putString(p, fs.save().toString());
        editor.apply();
    }

    public static class ExoLoader extends AssetsDexLoader.ThreadLoader {
        public static final Object lock = new Object();

        public ExoLoader(Context context, boolean block) {
            super(context, block, "exoplayer-core", "exoplayer-dash", "exoplayer-hsls", "exoplayer-smoothstreaming", "exoplayer-ui");
        }

        @Override
        public boolean need() {
            synchronized (lock) {
                return MediaPlayerCompat.classLoader == MediaPlayerCompat.class.getClassLoader();
            }
        }

        @Override
        public void done(ClassLoader l) {
            synchronized (lock) {
                MediaPlayerCompat.classLoader = l;
            }
        }
    }

    public static class SortByName implements Comparator<Storage.RecordingUri> {
        @Override
        public int compare(Storage.RecordingUri file, Storage.RecordingUri file2) {
            return file.name.compareTo(file2.name);
        }
    }

    public static class SortByDate implements Comparator<Storage.RecordingUri> {
        @Override
        public int compare(Storage.RecordingUri file, Storage.RecordingUri file2) {
            return Long.compare(file.last, file2.last);
        }
    }

    public static class RecordingHolder extends RecyclerView.ViewHolder {
        public TextView title, time, size, start, end;
        public View base, playerBase, edit, share;
        public SeekBar bar;
        private final ImageView trash, imgTrimAudio, play, star,down;

        public RecordingHolder(View v) {
            super(v);
            base = v.findViewById(R.id.recording_base);
            star = v.findViewById(R.id.recording_star);
            title = v.findViewById(R.id.txtAudioTitle);
            time = v.findViewById(R.id.recording_time);
            size = v.findViewById(R.id.recording_size);
            playerBase = v.findViewById(R.id.recording_player);
            play = v.findViewById(R.id.recording_player_play);
            start = v.findViewById(R.id.recording_player_start);
            bar = v.findViewById(R.id.recording_player_seek);
            end = v.findViewById(R.id.recording_player_end);
            edit = v.findViewById(R.id.recording_player_edit);
            share = v.findViewById(R.id.recording_player_share);
            trash = v.findViewById(R.id.recording_player_trash);
            down = v.findViewById(R.id.recording_player_down);
            imgTrimAudio = v.findViewById(R.id.recording_player_trim);
            //
        }
    }

    class PhoneStateChangeListener extends PhoneStateListener {
        public boolean wasRinging;
        public boolean pausedByCall;

        public RecordingHolder h;
        public Storage.RecordingUri f;

        public PhoneStateChangeListener(RecordingHolder h, final Storage.RecordingUri f) {
            this.h = h;
            this.f = f;
        }

        @Override
        public void onCallStateChanged(int s, String incomingNumber) {
            switch (s) {
                case TelephonyManager.CALL_STATE_RINGING:
                    wasRinging = true;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    wasRinging = true;
                    if (player != null && player.getPlayWhenReady()) {
                        playerPause(h, f);
                        pausedByCall = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (pausedByCall) {
                        if (player != null && !player.getPlayWhenReady())
                            playerPause(h, f);
                    }
                    wasRinging = false;
                    pausedByCall = false;
                    break;
            }
        }
    }

    public RecordingsAdapter(Context context, final RecyclerView list) {
        super(context);
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
        this.storage = new Storage(context);
        this.animator = new ExpandItemAnimator() {
            @Override
            public Animation apply(RecyclerView.ViewHolder h, boolean animate) {
                if (selected == h.getAdapterPosition())
                    return RecordingAnimation.apply(list, h.itemView, true, animate);
                else
                    return RecordingAnimation.apply(list, h.itemView, false, animate);
            }
        };
        list.setItemAnimator(animator);
        list.addOnScrollListener(animator.onScrollListener);
        load();
        IntentFilter ff = new IntentFilter();
        ff.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        ff.addAction(Intent.ACTION_MEDIA_MOUNTED);
        ff.addAction(Intent.ACTION_MEDIA_EJECT);
        context.registerReceiver(receiver, ff);
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        shared.registerOnSharedPreferenceChangeListener(this);
    }

    // true - include
    protected boolean filter(Storage.RecordingUri f) {
        if (filter != null) {
            if (!f.name.toLowerCase().contains(filter))
                return false;
        }
        if (toolbarFilterAll)
            return true;
        return MainApplication.getStar(context, f.uri);
    }

    public void scan(final List<Storage.Node> nn, final boolean clean, final Runnable done) {
        // đây là code
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        final Map<String, ?> prefs = shared.getAll();
        Log.d("keyUri",prefs.size()+"");

        final Thread old = thread;

        thread = new Thread("RecordingsAdapter Scan") {
            @Override
            public void run() {
                if (old != null) {
                    old.interrupt();
                    try {
                        old.join();
                        Log.d("key","1");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                try {
                    new ExoLoader(context, true);
                } catch (Exception e) {
                    Log.e(TAG, "error", e);
                }
                final Thread t = Thread.currentThread();
                final ArrayList<Storage.RecordingUri> all = new ArrayList<>();
                for (Storage.Node n : nn) {
                    if (t.isInterrupted()) {
                        Log.d("key", "2");
                        return;
                    }
                    Storage.RecordingStats fs = cache.get(n.uri);
                    if (fs == null) {
                        fs = getFileStats(prefs, n.uri);
                        if (fs != null)
                            cache.put(n.uri, fs);
                        Log.d("key","3");
                    }
                    if (fs != null) {
                        if (n.last != fs.last || n.size != fs.size)
                            Log.d("key","4");
                            fs = null;
                    }
                    if (fs == null) {
                        fs = new Storage.RecordingStats();
                        fs.size = n.size;
                        fs.last = n.last;
                        try {
                            fs.duration = getDuration(context, n.uri);
                            if (t.isInterrupted()) // getDuration can be interrupted and value is invalid
                                return;
                            cache.put(n.uri, fs);
                            setFileStats(context, n.uri, fs);
                            all.add(new Storage.RecordingUri(context, n.uri, fs));
                            Log.d("key","5");
                        } catch (Exception e) {
                            Log.d(TAG, n.toString(), e);
                        }
                        Log.d("keyUri",all.toString());
                    } else {
                        all.add(new Storage.RecordingUri(context, n.uri, fs));
                        Log.d("key","6");
                    }
                }

                handler.post(() -> {
                    if (thread != t)
                        return; // replaced with new thread, exit
                    items.clear(); // clear recordings
                    TreeSet<String> delete = new TreeSet<>();
                    for (String k : prefs.keySet()) {
                        if (k.startsWith(MainApplication.PREFERENCE_DETAILS_PREFIX)) {
                            delete.add(k);
                            Log.d("keyUriK",k);
                        }
                        Log.d("key","7");
                    }
                    Log.d("keyUriK",delete.size()+"");
                    TreeSet<Uri> delete2 = new TreeSet<>(cache.keySet());
                    Log.d("keyUriK2",delete2.size()+"");
                    for (Storage.RecordingUri f : all) {
                        Log.d("keyUri",f.name);
                        Log.d("keyUri",f.uri.toString());
                        Log.d("keyUri",f.toString());
                        if (filter(f))
                            items.add(f); // add recording
                        Log.d("key","8");
                        cleanDelete(delete, f.uri);
                        delete2.remove(f.uri);
                    }
                    Log.d("checkstarorall",items.size()+"");
                    Log.d("checkstarorall",toolbarFilterAll+"");

                    //bắn event khi click vào all or star
                    if (toolbarFilterAll == true){
                        sizeAll = items.size();
                    }else if(toolbarFilterAll == false){
                        sizeStar = items.size();
                    }

                    Log.d("keyUriK2",delete2.size()+"");
                    if (clean) {
                        SharedPreferences.Editor editor = shared.edit();
                        for (String s : delete)
                            editor.remove(s);
                        for (Uri f : delete2)
                            cache.remove(f);
                        editor.apply();
                        Log.d("key","9");
                    }
                    sort();
                    if (done != null)
                        done.run();
                    Log.d("key","10");
                });
            }
        };
        thread.start();
    }

    // luồng sẽ là ntn: có 2 cái prefer lưu, 1 cái là all, 1 cái star,
    // nó sẽ check true false để đổ lên list
    public void cleanDelete(TreeSet<String> delete, Uri f) { // file exists, prevent it from cleaning
        String p = MainApplication.getFilePref(f);
        delete.remove(p + MainApplication.PREFERENCE_DETAILS_FS);
        delete.remove(p + MainApplication.PREFERENCE_DETAILS_STAR);
    }

    public Comparator<Storage.RecordingUri> getSort() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        int selected = context.getResources().getIdentifier(shared.getString(MainApplication.PREFERENCE_SORT, context.getResources().getResourceEntryName(R.id.sort_date_desc)), "id", context.getPackageName());
        if (selected == R.id.sort_date_ask)
            return new SortByDate();
        else if (selected == R.id.sort_date_desc)
            return Collections.reverseOrder(new SortByDate());
        else if (selected == R.id.sort_name_ask)
            return new SortByName();
        else if (selected == R.id.sort_name_desc)
            return Collections.reverseOrder(new SortByName());
        return new SortByName();
    }

    public void sort(Comparator<Storage.RecordingUri> sort) {
        Collections.sort(items, sort);
        notifyDataSetChanged();
    }


    public void sort() {
        sort(getSort());
    }

    public void close() {
        playerStop();
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        if (receiver != null) {
            context.unregisterReceiver(receiver);
            receiver = null;
        }
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        shared.unregisterOnSharedPreferenceChangeListener(this);
    }

    public String[] getEncodingValues() {
        return Factory.getEncodingValues(context);
    }

    public void load(boolean clean, Runnable done) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        String path = shared.getString(MainApplication.PREFERENCE_STORAGE, "");
        Log.d(TAG, "load: " + path);
        Uri user;
        if (path.startsWith(ContentResolver.SCHEME_CONTENT)) {
            user = Uri.parse(path);
        } else if (path.startsWith(ContentResolver.SCHEME_FILE)) {
            user = Uri.parse(path);
        } else {
            user = Uri.fromFile(new File(path));
        }

        Uri mount = storage.getStoragePath(path);
        Log.d(TAG, "mount: " + mount);
        Log.d(TAG, "user:" + user);
        if (!user.equals(mount))
            clean = false; // do not clean if we failed to mount user selected folder

        load(mount, clean, done);
    }

    public void load(Uri mount, boolean clean, Runnable done) {
        scan(Storage.scan(context, mount, getEncodingValues()), clean, done);
    }

    public View inflate(LayoutInflater inflater, int id, ViewGroup parent) {
        return inflater.inflate(id, parent, false);
    }

    @SuppressLint("MissingPermission")
    @NonNull
    @Override
    public RecordingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View convertView = inflate(inflater, R.layout.recording, parent);
        //mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        return new RecordingHolder(convertView);
    }

    @Override
    public void onBindViewHolder(final RecordingHolder h, int position) {
        final Storage.RecordingUri f = items.get(position);

        final boolean isStar = MainApplication.getStar(context, f.uri);
        starUpdate(h.star, isStar);
        Log.d("checkStart",isStar+"");
        h.star.setOnClickListener(v -> {
            boolean b = !MainApplication.getStar(context, f.uri);
            MainApplication.setStar(context, f.uri, b);
            starUpdate(h.star, b);
        });

        h.title.setText(f.name);

        h.time.setText(MainApplication.SIMPLE.format(new Date(f.last)));

//        h.dur.setText(MainApplication.formatDuration(context, f.duration));

        h.size.setText(MainApplication.formatSize(context, f.size));

//        h.playerBase.setOnClickListener(v -> {
//        });

        @SuppressLint("InflateParams") final Runnable delete = () -> {
            LayoutInflater inflater = LayoutInflater.from(context);
            final View dialogView = inflater.inflate(R.layout.alert_dialog_delete_audio_layout, null);
            final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
            alertDialog.setView(dialogView);

            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            Button btnOk = dialogView.findViewById(R.id.btnOk);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            Objects.requireNonNull(btnOk).setOnClickListener(v -> {
                alertDialog.cancel();
                playerStop();
                try {
                    Storage.delete(context, f.uri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                select(-1);
                int pos = items.indexOf(f);
                items.remove(f); // instant remove
                notifyItemRemoved(pos);
                Log.d("deleteclick","click ok");
//                checkIfListIsEmpty();
            });

            Objects.requireNonNull(btnCancel).setOnClickListener(v -> {
                alertDialog.cancel();
                Log.d("deleteclick","click cancel");
            });

            alertDialog.show();
        };

        final Runnable rename = () -> {
            LayoutInflater inflater = LayoutInflater.from(context);
            final View dialogView = inflater.inflate(R.layout.alert_dialog_edit_audio_layout, null);
            final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
            alertDialog.setView(dialogView);

            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            Button btnOk = dialogView.findViewById(R.id.btnOk);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);

            EditText edtTitle = dialogView.findViewById(R.id.edtTitle);
            edtTitle.setText(Storage.getNameNoExt(f.name));
            edtTitle.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) showSoftKeyboard(context);
                else hideSoftKeyboard(context, edtTitle);
            });
            edtTitle.requestFocus();

            Objects.requireNonNull(btnOk).setOnClickListener(v -> {
                playerStop();
                String ext = Storage.getExt(f.name);
                String s = String.format("%s.%s", edtTitle.getText(), ext);
                f.uri = storage.rename(f.uri, s);
                f.name = s;
                int pos = items.indexOf(f);
                notifyItemChanged(pos);
//                checkIfListIsEmpty();
                edtTitle.clearFocus();
                alertDialog.dismiss();
            });

            Objects.requireNonNull(btnCancel).setOnClickListener(v -> {
                edtTitle.clearFocus();
                alertDialog.cancel();
            });

            alertDialog.setOnCancelListener(v -> {
                edtTitle.clearFocus();
                new Handler(Looper.getMainLooper()).postDelayed(() -> hideSoftKeyboard(context, edtTitle), 10);
            });

            alertDialog.show();
        };

        if (selected == position) {
            updatePlayerText(h, f);

            h.play.setOnClickListener(v -> {
                if (player == null) {
                    playerPlay(h, f);
                } else if (player.getPlayWhenReady()) {
                    playerPause(h, f);
                } else {
                    playerPlay(h, f);
                }
            });

            RxView.clicks(h.down).throttleFirst(1, TimeUnit.SECONDS).subscribe(v ->{
                try {
                  //  Uri uri = f.uri;
                   /* DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, f.name);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); // to notify when download is complete
                    request.allowScanningByMediaScanner();// if you want to be available from media players
                    DownloadManager manager = (DownloadManager) getContext().getSystemService(getContext().DOWNLOAD_SERVICE);
                    manager.enqueue(request);*/
                    AssetManager asset_manager = getContext().getAssets();


                    InputStream in = null;
                    OutputStream out = null;

                    in = new FileInputStream(new File(f.uri.getPath()));
                   // in = asset_manager.open(f.name);
                    String outDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DOWNLOAD";


                    Log.e("xxxx file ",outDir.toString());
                    Log.e("xxxx",outDir.toString());

                    File outFile = new File(outDir,f.name);

                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                    in.close();
                    in = null;
                    out.flush();
                    out.close();
                    out = null;
                    Toast.makeText(getContext(),"Download success",Toast.LENGTH_SHORT).show();
                }catch (Exception e){
                    Toast.makeText(getContext(),"Download fall",Toast.LENGTH_SHORT).show();
                }

            });


            RxView.clicks(h.edit).throttleFirst(1, TimeUnit.SECONDS).subscribe(v -> rename.run());

            RxView.clicks(h.share).throttleFirst(1, TimeUnit.SECONDS).subscribe(v -> {
                AppOpenManager.getInstance().disableAppResume();
                Storage.openResume = true;
                playerStop();
                String name = AboutPreferenceCompat.getVersion(context);
                Intent intent = new Intent(Intent.ACTION_SEND);
//                intent.setType(Storage.getTypeByName(f.name));
                intent.setType("audio/*");
//                intent.putExtra(Intent.EXTRA_EMAIL, "");
//                intent.putExtra(Intent.EXTRA_STREAM, f.uri);
                intent.putExtra(Intent.EXTRA_STREAM, StorageProvider.getProvider().share(f.uri));
                intent.putExtra(Intent.EXTRA_SUBJECT, f.name);
                intent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.shared_via, name));
//                PopupShareActionProvider.show(context, h.share, intent);
                context.startActivity(Intent.createChooser(intent, name));

            });

            RxView.clicks(h.imgTrimAudio).throttleFirst(1, TimeUnit.SECONDS).subscribe(v -> {
                startTrimAudioActivity(f.uri);
            });
//            h.imgTrimAudio.setOnClickListener(v -> {
////                Intent intent = new Intent(context, AudioTrimmerActivity.class);
//                Intent intent = new Intent(context, TrimAudioActivity.class);
//                intent.putExtra(EXTRA_AUDIO_URI, f.uri);
//                context.startActivity(intent);
//            });

            KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            final boolean locked = myKM.inKeyguardRestrictedInputMode();

            if (locked) {
                h.trash.setOnClickListener(null);
                h.trash.setClickable(true);
//                h.trash.setColorFilter(Color.GRAY);
            } else {
                RxView.clicks(h.trash).throttleFirst(1, TimeUnit.SECONDS).subscribe(v -> {
                    delete.run();
                    Log.d("deleteclick","click delete");
                    @SuppressLint("MissingPermission") FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
                    Bundle bundleDelete= new Bundle();
                    bundleDelete.putString("click","clickButtonDelete");
                    mFirebaseAnalytics.logEvent("btnClickDelete", bundleDelete);

                    MainApplication.setStar(context, f.uri, false);
                });

//                h.trash.setOnClickListener(v -> delete.run());
            }

            h.itemView.setOnClickListener(v -> select(-1));
        } else {
            h.itemView.setOnClickListener(v -> select(h.getAdapterPosition()));
        }

//        h.itemView.setOnLongClickListener(v -> {
//            PopupMenu popup = new PopupMenu(context, v);
//            MenuInflater inflater = popup.getMenuInflater();
//            inflater.inflate(R.menu.menu_context, popup.getMenu());
//            popup.setOnMenuItemClickListener(item -> {
//                if (item.getItemId() == R.id.action_delete) {
//                    delete.run();
//                    return true;
//                }
//                if (item.getItemId() == R.id.action_rename) {
//                    rename.run();
//                    return true;
//                }
//                return false;
//            });
//            popup.show();
//            return true;
//        });

        animator.onBindViewHolder(h, position);

        downloadTaskUpdate(null, f, h.itemView);
    }

    private void startTrimAudioActivity(Uri uri) {
        try {
            Intent intent = null;

            try {
                intent = new Intent(context, Class.forName("com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities.MainActivity"));
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (intent != null) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("android.intent.extra.BOOLEAN", true);
                bundle.putParcelable("com.github.audiolibrary.URI", uri);
                intent.putExtra("com.github.audiolibrary.BUNDLE", bundle);
            }
            Activity activity = (Activity) context;
            activity.startActivityForResult(intent, REQUEST_OPEN_TRIMMER);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideSoftKeyboard(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showSoftKeyboard(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public Bitmap downloadImageTask(CacheImagesAdapter.DownloadImageTask task) {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        Storage.RecordingUri f = (Storage.RecordingUri) task.item;
        try {
            File cover = getCover(context, f);
            if (cover.exists() && f.data == null)
                f.data = MoodbarView.loadMoodbar(cover);
            if (f.data == null)
                f.data = MoodbarView.getMoodbar(context, f.uri);
            if (f.data != null)
                MoodbarView.saveMoodbar(f.data, cover);
        } catch (Exception e) {
            Log.e(TAG, "Unable to load cover", e);
        }
        return null;
    }

    @Override
    public void downloadTaskUpdate(CacheImagesAdapter.DownloadImageTask task, Object item, Object view) {
        super.downloadTaskUpdate(task, item, view);
        RecordingHolder h = new RecordingHolder((View) view);
    }

    protected void starUpdate(ImageView star, boolean isStar) {
        if (isStar) {
            star.setImageResource(R.drawable.ic_star);
            star.setContentDescription(context.getString(R.string.starred));
        } else {
            star.setImageResource(R.drawable.ic_star_border);
            star.setContentDescription(context.getString(R.string.not_starred));
        }
    }

    public boolean getPrefCall() {
        return false;
    }

    protected void playerPlay(RecordingHolder h, final Storage.RecordingUri f) {
        if (player == null) {
            player = MediaPlayerCompat.create(context, f.uri);
            if (player == null) {
                Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            player.prepare();
            if (getPrefCall()) {
                pscl = new PhoneStateChangeListener(h, f);
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                tm.listen(pscl, PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
        player.setPlayWhenReady(true);

        if (proximity == null) {
            proximity = new ProximityPlayer(context) {
                @Override
                public void prepare() {
                    player.setAudioStreamType(streamType);
                }
            };
            proximity.create();
        }

        updatePlayerRun(h, f);
    }

    protected void playerPause(RecordingHolder h, Storage.RecordingUri f) {
        if (player != null)
            player.setPlayWhenReady(false);
        if (updatePlayer != null) {
            handler.removeCallbacks(updatePlayer);
            updatePlayer = null;
        }
        if (proximity != null) {
            proximity.close();
            proximity = null;
        }
        updatePlayerText(h, f);
    }

    protected void playerStop() {
        if (updatePlayer != null) {
            handler.removeCallbacks(updatePlayer);
            updatePlayer = null;
        }
        if (proximity != null) {
            proximity.close();
            proximity = null;
        }
        if (player != null) {
            player.setPlayWhenReady(false); // stop()
            player.release();
            player = null;
        }
        if (pscl != null) {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(pscl, PhoneStateListener.LISTEN_NONE);
            pscl = null;
        }
    }

    protected void updatePlayerRun(final RecordingHolder h, final Storage.RecordingUri f) {
        boolean playing = updatePlayerText(h, f);

        if (updatePlayer != null) {
            handler.removeCallbacks(updatePlayer);
            updatePlayer = null;
        }

        if (!playing) {
            playerStop(); // clear player instance
            updatePlayerText(h, f); // update length
            return;
        }

        updatePlayer = () -> updatePlayerRun(h, f);
        handler.postDelayed(updatePlayer, 200);
    }

    protected boolean updatePlayerText(final RecordingHolder h, final Storage.RecordingUri f) {
        final boolean playing = player != null && player.getPlayWhenReady();

        h.play.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        h.play.setContentDescription(context.getString(playing ? R.string.pause_button : R.string.play_button));

        long currentPos = 0;
        long duration = f.duration;

        if (player != null) {
            currentPos = player.getCurrentPosition();
            duration = player.getDuration();
        }

        h.bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;

                if (player == null) playerPlay(h, f);
                else {
                    player.seekTo(progress);
                    if (!player.getPlayWhenReady())
                        playerPlay(h, f);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        h.start.setText(MainApplication.formatDuration(context, currentPos));
        h.bar.setMax((int) duration);
        h.bar.setKeyProgressIncrement(1);
        h.bar.setProgress((int) currentPos);
        h.end.setText(MainApplication.formatDuration(context, duration));
//        h.end.setText(MainApplication.formatDuration(context, duration - currentPos));

        return playing;
    }

    public void select(int pos) {
        if (selected != pos && selected != -1)
            notifyItemChanged(selected);
        selected = pos;
        if (pos != -1)
            notifyItemChanged(pos);
        playerStop();

//        checkIfListIsEmpty();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Storage.RecordingUri getItem(int pos) {
        return items.get(pos);
    }

    protected AppCompatImageButton getCheckBox(View v) {
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View c = getCheckBox(g.getChildAt(i));
                if (c != null)
                    return (AppCompatImageButton) c;
            }
        }
        if (v instanceof AppCompatImageButton)
            return (AppCompatImageButton) v;
        return null;
    }

    // đoạn set view hiển thị
    public void setRvFilter(boolean filterOn) {
        toolbarFilterAll = filterOn;
        Log.d(TAG, "setRvFilter: " + toolbarFilterAll);
        load(false, null);
        save();
    }

    protected void save() {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = shared.edit();
        edit.putBoolean(MainApplication.PREFERENCE_FILTER, toolbarFilterAll);
        edit.apply();
    }

    protected void load() {
        final SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        toolbarFilterAll = shared.getBoolean(MainApplication.PREFERENCE_FILTER, true);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(MainApplication.PREFERENCE_STORAGE))
            load(true, null);
    }

    public void search(String q) {
        filter = q.toLowerCase(Locale.US);
        load(false, null);
    }

    public void searchClose() {
        filter = "";
        load(false, null);
    }

    public void showDialog(AlertDialog.Builder e) {
        e.show();
    }

    public void onCreateOptionsMenu(Menu menu) {
        MenuItem sort = menu.findItem(R.id.action_sort);
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        int selected = context.getResources().getIdentifier(shared.getString(MainApplication.PREFERENCE_SORT, context.getResources().getResourceEntryName(R.id.sort_date_desc)), "id", context.getPackageName());
        SubMenu sorts = sort.getSubMenu();
        for (int i = 0; i < sorts.size(); i++) {
            MenuItem m = sorts.getItem(i);
            if (m.getItemId() == selected)
                m.setChecked(true);
            m.setOnMenuItemClickListener(item -> false);
        }
    }

    public boolean onOptionsItemSelected(AppCompatActivity a, MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.sort_date_ask || i == R.id.sort_date_desc || i == R.id.sort_name_ask || i == R.id.sort_name_desc) {
            onSortOptionSelected(a, i);
            return true;
        }
        return false;
    }

    public void onSortOptionSelected(AppCompatActivity a, int id) {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        shared.edit().putString(MainApplication.PREFERENCE_SORT, context.getResources().getResourceEntryName(id)).apply();
        select(-1);
        sort();
//        ActivityCompat.invalidateOptionsMenu(a);
        a.invalidateOptionsMenu();
    }
    public String getPath(Uri uri)
    {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index =             cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s=cursor.getString(column_index);
        cursor.close();
        return s;
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}
