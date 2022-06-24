package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.github.axet.androidlibrary.activities.AppCompatSettingsThemeActivity;
import com.github.axet.androidlibrary.preferences.NameFormatPreferenceCompat;
import com.github.axet.androidlibrary.preferences.SeekBarPreference;
import com.github.axet.androidlibrary.preferences.StoragePathPreferenceCompat;
import com.github.axet.audiorecorder.R;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.AudioApplication;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.Storage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.services.ControlsService;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;
import com.voicerecorder.axet.audiolibrary.app.Sound;
import com.voicerecorder.axet.audiolibrary.encoders.Factory;
import com.voicerecorder.axet.audiolibrary.widgets.RecordingVolumePreference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SettingsActivity extends AppCompatSettingsThemeActivity implements PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

    public static final int RESULT_STORAGE = 1;
    public static final int RESULT_CALL = 2;
    private InterstitialAd interBack;
    Storage storage;

    FirebaseAnalytics mFirebaseAnalytics;


    public static String[] PREMS = new String[]{Manifest.permission.READ_PHONE_STATE};

    public static void startActivity(Context context) {
        context.startActivity(new Intent(context, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeElement(Class<T> c, T[] aa, int i) {
        List<T> ll = Arrays.asList(aa);
        ll = new ArrayList<>(ll);
        ll.remove(i);
        return ll.toArray((T[]) Array.newInstance(c, ll.size()));
    }

    @Override
    public int getAppTheme() {
        return AudioApplication.getTheme(this, R.style.RecThemeLight_NoActionBar, R.style.RecThemeDark_NoActionBar);
    }

    @Override
    public String getAppThemeKey() {
        return AudioApplication.PREFERENCE_THEME;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set Language
        SystemUtil.setLocale(getBaseContext());


        storage = new Storage(this);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        //firebase
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        super.onSharedPreferenceChanged(sharedPreferences, key);
        if (key.equals(AudioApplication.PREFERENCE_CONTROLS)) {
            if (sharedPreferences.getBoolean(AudioApplication.PREFERENCE_CONTROLS, false))
                ControlsService.start(this);
            else
                ControlsService.stop(this);
        }
        if (key.equals(AudioApplication.PREFERENCE_STORAGE))
            storage.migrateLocalStorageDialog(this);
        if (key.equals(AudioApplication.PREFERENCE_RATE)) {
            int sampleRate = Integer.parseInt(sharedPreferences.getString(AudioApplication.PREFERENCE_RATE, ""));
            if (sampleRate != Sound.getValidRecordRate(Sound.getInMode(this), sampleRate)) {
                try {
                    Toast.makeText(SettingsActivity.this, "Not supported Hz", Toast.LENGTH_SHORT).show();
                } catch (Exception exception) {
                }
            }
//                Toast.Text(this, "Not supported Hz");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
       /* if (System.currentTimeMillis() - AdmodUtils.getInstance().lastTimeShowInterstitial > 30 * 1000) {
            AdmodUtils.getInstance().loadAndShowAdInterstitialWithCallback(this, Common.getRemoteConfigAdUnit("ads_admob_inter_back_home"), 0,
                    new AdCallback() {
                        @Override
                        public void onAdClosed() {
                            //
                            AdmodUtils.getInstance().mInterstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
                                @Override
                                public void onPaidEvent(@NonNull AdValue adValue) {

                                    VAppUtility.logAdAdmobValue(adValue,
                                            AdmodUtils.getInstance().mInterstitialAd.getAdUnitId(),
                                            AdmodUtils.getInstance().mInterstitialAd.getResponseInfo().getMediationAdapterClassName(),
                                            mFirebaseAnalytics);
                                }
                            });
                            //
                            finish();
                            MainActivity.startActivity(SettingsActivity.this);
                        }

                        @Override
                        public void onAdFail() {
                            finish();
                            MainActivity.startActivity(SettingsActivity.this);
                        }
                    }, true);
        }
        else {
            finish();
            MainActivity.startActivity(SettingsActivity.this);
        }*/
        finish();
        MainActivity.startActivity(SettingsActivity.this);
    }

    @Override
    public boolean onPreferenceDisplayDialog(PreferenceFragmentCompat caller, Preference pref) {
        if (pref instanceof NameFormatPreferenceCompat) {
            NameFormatPreferenceCompat.show(caller, pref.getKey());
            return true;
        }
        if (pref instanceof SeekBarPreference) {
            RecordingVolumePreference.show(caller, pref.getKey());
            return true;
        }
        return false;
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {
        void initPrefs(PreferenceManager pm, PreferenceScreen screen) {
            // Set Language
            SystemUtil.setLocale(getContext());
            final Context context = screen.getContext();
            ListPreference enc = (ListPreference) pm.findPreference(AudioApplication.PREFERENCE_ENCODING);
            String v = enc.getValue();
            CharSequence[] ee = Factory.getEncodingTexts(context);
            CharSequence[] vv = Factory.getEncodingValues(context);
            if (ee.length > 1) {
                enc.setEntries(ee);
                enc.setEntryValues(vv);

                int i = enc.findIndexOfValue(v);
                if (i == -1) {
                    enc.setValueIndex(0);
                } else {
                    enc.setValueIndex(i);
                }

                bindPreferenceSummaryToValue(enc);
            } else {
                screen.removePreference(enc);
            }

            bindPreferenceSummaryToValue(pm.findPreference(AudioApplication.PREFERENCE_RATE));
//            bindPreferenceSummaryToValue(pm.findPreference(AudioApplication.PREFERENCE_THEME));
            bindPreferenceSummaryToValue(pm.findPreference(AudioApplication.PREFERENCE_CHANNELS));
            bindPreferenceSummaryToValue(pm.findPreference(AudioApplication.PREFERENCE_FORMAT));
            bindPreferenceSummaryToValue(pm.findPreference(AudioApplication.PREFERENCE_VOLUME));


            StoragePathPreferenceCompat s = (StoragePathPreferenceCompat) pm.findPreference(AudioApplication.PREFERENCE_STORAGE);
            s.setStorage(new Storage(getContext()));
            s.setPermissionsDialog(this, Storage.PERMISSIONS_RW, RESULT_STORAGE);
            s.setStorageAccessFramework(this, RESULT_STORAGE);



            AudioManager am = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            Preference bluetooth = pm.findPreference(AudioApplication.PREFERENCE_SOURCE);
            if (!am.isBluetoothScoAvailableOffCall()) {
                bluetooth.setVisible(false);
            }
            bindPreferenceSummaryToValue(bluetooth);

            Preference p = pm.findPreference(AudioApplication.PREFERENCE_CALL);
            p.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean b = (boolean) newValue;
                if (b) {
                    return Storage.permitted(GeneralPreferenceFragment.this, PREMS, RESULT_CALL);
                }
                return true;
            });

              Preference language = pm.findPreference(AudioApplication.PREFERENCE_LANGUAGE);
            language.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(),LanguageActivity.class));
                    return false;
                }
            });
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setHasOptionsMenu(true);
            addPreferencesFromResource(R.xml.pref_general);
            initPrefs(getPreferenceManager(), getPreferenceScreen());
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                requireActivity().onBackPressed();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            StoragePathPreferenceCompat s = (StoragePathPreferenceCompat) findPreference(AudioApplication.PREFERENCE_STORAGE);
            switch (requestCode) {
                case RESULT_STORAGE:
                    s.onRequestPermissionsResult(permissions, grantResults);
                    break;
                case RESULT_CALL:
                    SwitchPreferenceCompat p = (SwitchPreferenceCompat) findPreference(AudioApplication.PREFERENCE_CALL);
                    p.setChecked(Storage.permitted(getContext(), PREMS));
                    break;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            StoragePathPreferenceCompat s = (StoragePathPreferenceCompat) findPreference(AudioApplication.PREFERENCE_STORAGE);
            switch (requestCode) {
                case RESULT_STORAGE:
                    s.onActivityResult(resultCode, data);
                    break;
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            //    SilencePreferenceCompat silent = (SilencePreferenceCompat) findPreference(AudioApplication.PREFERENCE_SILENT);
            //     silent.onResume();
//            Preference controls = findPreference(AudioApplication.PREFERENCE_CONTROLS);
//            if (Build.VERSION.SDK_INT < 21)
//                controls.setVisible(false);
        }
    }


}
