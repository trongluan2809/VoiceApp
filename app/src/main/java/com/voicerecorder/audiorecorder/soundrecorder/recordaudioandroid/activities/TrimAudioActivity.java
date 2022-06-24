package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.amazic.ads.callback.NativeCallback;
import com.amazic.ads.util.Admod;
import com.github.axet.androidlibrary.services.FileProvider;
import com.github.axet.audiorecorder.R;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.Common;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.app.Storage;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;
import com.voicerecorder.axet.audiolibrary.app.MainApplication;
import com.voicerecorder.axet.audiolibrary.trimmer.customAudioViews.MarkerView;
import com.voicerecorder.axet.audiolibrary.trimmer.customAudioViews.SamplePlayer;
import com.voicerecorder.axet.audiolibrary.trimmer.customAudioViews.SoundFile;
import com.voicerecorder.axet.audiolibrary.trimmer.customAudioViews.WaveformView;
import com.voicerecorder.axet.audiolibrary.trimmer.utils.Utility;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TrimAudioActivity extends AppCompatActivity implements View.OnClickListener,
        MarkerView.MarkerListener, WaveformView.WaveformListener {

    private static final int REQUEST_STORAGE_PERMISSION = 102;
    private MarkerView markerStart, markerEnd;
    private WaveformView audioWaveform;
    private TextView txtStartPosition, txtEndPosition, txtAudioTitle;
    private Button btnAudioCancel, btnReset, btnCut;
    private ImageView imgAudioPlay, imgNext, imgPrev;

    private boolean isAudioRecording = false;
    private boolean mRecordingKeepGoing;
    private SoundFile mLoadedSoundFile;
    private SoundFile mAudioFile;
    private SamplePlayer mPlayer;
    private Handler mHandler;

    private boolean mTouchDragging;
    private float mTouchStart;
    private int mTouchInitialOffset;
    private int mTouchInitialStartPos;
    private int mTouchInitialEndPos;
    private float mDensity;
    private int mMarkerLeftInset;
    private int mMarkerRightInset;
    private int mMarkerTopOffset;
    private int mMarkerBottomOffset;
    private int mPlayStartMsec;
    private int mPlayEndMillSec;

    private int mOffset;
    private int mOffsetGoal;
    private int mFlingVelocity;
    private int mWidth;
    private int mMaxPos;
    private int mStartPos;
    private int mEndPos;

    private boolean mStartVisible;
    private boolean mEndVisible;
    private int mLastDisplayedStartPos;
    private int mLastDisplayedEndPos;
    private boolean mIsPlaying = false;
    private boolean mKeyDown;
    private ProgressDialog savingRecordDialog;
    private long mLoadingLastUpdateTime;
    private boolean mLoadingKeepGoing;
    private File mFile;
    private Storage storage;
    String fileName = "";
    private SharedPreferences shared;
    public static void setLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set Language
        SystemUtil.setLocale(getBaseContext());

        storage = new Storage(this);
        setContentView(R.layout.activity_trim_audio);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.trim_audio));
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setHomeAsUpIndicator(R.drawable.ic_back);

        mHandler = new Handler(Looper.getMainLooper());
        shared = PreferenceManager.getDefaultSharedPreferences(this);
        btnAudioCancel = findViewById(R.id.btnAudioCancel);
        txtAudioTitle = findViewById(R.id.txtAudioTitle);
        txtStartPosition = findViewById(R.id.txtStartPosition);
        txtEndPosition = findViewById(R.id.txtEndPosition);
        markerStart = findViewById(R.id.markerStart);
        markerEnd = findViewById(R.id.markerEnd);
        audioWaveform = findViewById(R.id.audioWaveform);
        btnReset = findViewById(R.id.btnReset);
        imgAudioPlay = findViewById(R.id.imgAudioPlay);
        imgNext = findViewById(R.id.imgNext);
        imgPrev = findViewById(R.id.imgPrev);
        btnCut = findViewById(R.id.btnCut);

        mAudioFile = null;
        mKeyDown = false;
        audioWaveform.setListener(this);

        markerStart.setListener(this);
        markerStart.setFocusable(true);
        markerStart.setFocusableInTouchMode(true);
        mStartVisible = true;

        markerEnd.setListener(this);
        markerEnd.setFocusable(true);
        markerEnd.setFocusableInTouchMode(true);
        mEndVisible = true;

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;

        /**
         * Change this for marker handle as per your view
         */
        mMarkerLeftInset = (int) markerStart.getPaddingStart();
        mMarkerRightInset = (int) (30 * mDensity);
        mMarkerTopOffset = (int) (6 * mDensity);
        mMarkerBottomOffset = (int) (6 * mDensity);

        /**
         * Change this for duration text as per your view
         */

        btnAudioCancel.setOnClickListener(this);
        imgAudioPlay.setOnClickListener(this);
        imgPrev.setOnClickListener(this);
        imgNext.setOnClickListener(this);
        btnCut.setOnClickListener(this);
        btnReset.setOnClickListener(this);

        mHandler.postDelayed(mTimerRunnable, 100);
        ProgressDialog openAudioDialog = new ProgressDialog(this);
        openAudioDialog.setMessage(getString(R.string.loading));
        openAudioDialog.show();
        showTrimmingEditor(openAudioDialog);

    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            handlePause();
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btnCut.performClick();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        showAdsAndFinish();
    }

    private void showAdsAndFinish() {
      /*  if (System.currentTimeMillis() - AdmodUtils.getInstance().lastTimeShowInterstitial > 30 * 1000) {
            AdmodUtils.getInstance().loadAndShowAdInterstitialWithCallback(this, Common.getRemoteConfigAdUnit("ads_admob_inter_back_home"), 0,
                    new AdCallback() {
                        @Override
                        public void onAdClosed() {
                            finish();
                        }

                        @Override
                        public void onAdFail() {
                            finish();
                        }
                    }, true);
        } else {
            finish();
        }*/
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private final Runnable mTimerRunnable = new Runnable() {
        public void run() {
            // Updating Text is slow on Android.  Make sure
            // we only do the update if the text has actually changed.
            if (mStartPos != mLastDisplayedStartPos) {
                txtStartPosition.setText(formatTime(mStartPos));
                mLastDisplayedStartPos = mStartPos;
            }

            if (mEndPos != mLastDisplayedEndPos) {
                txtEndPosition.setText(formatTime(mEndPos));
                mLastDisplayedEndPos = mEndPos;
            }

            mHandler.postDelayed(mTimerRunnable, 100);
        }
    };

    @Override
    public void onClick(View view) {
        if (view == btnAudioCancel) {
            showAdsAndFinish();
        } else if (view == imgAudioPlay) {
            if (!mIsPlaying) {
                imgAudioPlay.setImageResource(R.drawable.ic_pause_circle);
            } else {
                imgAudioPlay.setImageResource(R.drawable.ic_backward);
            }
            onPlay(mStartPos);
        } else if (view == btnCut) {
            if (mIsPlaying) handlePause();
            if (Utility.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_STORAGE_PERMISSION)) {
                showSaveAudioDialog();
            }
        } else if (view == btnReset) {
            if (mIsPlaying) handlePause();
            audioWaveform.setIsDrawBorder(true);
            mPlayer = new SamplePlayer(mAudioFile);
            finishOpeningSoundFile(mAudioFile);
        } else if (view == imgNext) {
            if (mIsPlaying) {
                int newPos = 5000 + mPlayer.getCurrentPosition();
                if (newPos > mPlayEndMillSec)
                    newPos = mPlayEndMillSec;
                if (newPos >= mPlayEndMillSec) {
                    imgAudioPlay.setImageResource(R.drawable.ic_play_circle);
                    onPlay(mStartPos);
                }
                mPlayer.seekTo(newPos);
            } else {
                markerEnd.requestFocus();
//                mEndMarker.setImageResource(R.drawable.ic_keo2);
//                mStartMarker.setImageResource(R.drawable.ic_keo1);
                markerFocus(markerEnd);
                int now = mPlayer.getCurrentPosition();
            }
        } else if (view == imgPrev) {
            if (mIsPlaying) {
                int newPos = mPlayer.getCurrentPosition() - 5000;
                if (newPos < mPlayStartMsec)
                    newPos = mPlayStartMsec;
                mPlayer.seekTo(newPos);
            } else {
                markerStart.requestFocus();
//                mStartMarker.setImageResource(R.drawable.ic_keo1);
//                mEndMarker.setImageResource(R.drawable.ic_keo2);
                markerFocus(markerStart);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void showSaveAudioDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView = inflater.inflate(R.layout.alert_dialog_save_trimmed_audio, null);
        final AlertDialog renameDialog = new AlertDialog.Builder(this).create();
        renameDialog.setView(dialogView);

        renameDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        EditText edtTitle = dialogView.findViewById(R.id.edtTitle);
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        edtTitle.setText(FilenameUtils.removeExtension(getAudioTitle()) + " (2)");

        Objects.requireNonNull(btnOk).setOnClickListener(v -> {
            saveAudio(edtTitle.getText().toString());
            renameDialog.dismiss();
        });

        Objects.requireNonNull(btnCancel).setOnClickListener(v -> renameDialog.cancel());

        renameDialog.show();
    }

    private String getAudioTitle() {
        return txtAudioTitle.getText().toString();
    }

    private void updateAudioTitle(String title) {
        runOnUiThread(() -> txtAudioTitle.setText(title));
    }

    private void saveAudio(String title) {
        double startTime = audioWaveform.pixelsToSeconds(mStartPos);
        double endTime = audioWaveform.pixelsToSeconds(mEndPos);
        double trimmingLength = endTime - startTime;
        if (trimmingLength <= 0) {
            Toast.makeText(this, getString(R.string.trim_sec_alert), Toast.LENGTH_SHORT).show();
        } else {
            // Create an indeterminate progress dialog
            savingRecordDialog = new ProgressDialog(this);
            savingRecordDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            savingRecordDialog.setTitle(getString(R.string.processing));
            savingRecordDialog.setIndeterminate(true);
            savingRecordDialog.setCancelable(false);
            savingRecordDialog.show();
            String ext = shared.getString(MainApplication.PREFERENCE_ENCODING, ".mp3");
            // Try AAC first.
            fileName = title + "."+ext;
            String externalRootDir = Environment.getExternalStorageDirectory().getPath();
            if (!externalRootDir.endsWith("/")) {
                externalRootDir += "/";
            }
            String parentDir = null;
         /*   String subDir = getString(R.string.saving_recorder_folder);
            String parentDir = externalRootDir + subDir;

            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
            String newDir = shared.getString(MainApplication.PREFERENCE_STORAGE, parentDir);
            */
            
            try {
                Uri newUri = storage.getStoragePath();
                parentDir = newUri.getPath();
            } catch (Exception e) {
                Utility.log(e.toString());
            }

            // Create the parent directory if not exist
            File parentDirFile = new File(parentDir);
            if (!parentDirFile.exists()) parentDirFile.mkdirs();
//
            File outFile = new File(parentDir + "/" + fileName);
//            File outFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), fileName);

            Runnable runnable = () -> {
                afterSavingRingtone(title, outFile.getPath(), (int) Math.round(endTime - startTime));
                audioWaveform.setIsDrawBorder(true);
            };
            if (outFile.exists()) {
                showExistingFileDialog(outFile, runnable);
            } else {
                startSavingAudio(outFile, runnable);
            }
        }
    }

    private void startSavingAudio(File outFile, Runnable runnable) {
        // Save the sound file in a background thread
        new Thread() {
            @Override
            public void run() {
                double startTime = audioWaveform.pixelsToSeconds(mStartPos);
                double endTime = audioWaveform.pixelsToSeconds(mEndPos);
                final int startFrame = audioWaveform.secondsToFrames(startTime);
//                final int endFrame = audioWaveform.secondsToFrames(endTime - 0.04);
                final int endFrame = audioWaveform.secondsToFrames(endTime);

                try {
                    mAudioFile.WriteFile(outFile, startFrame, endFrame - startFrame);
                    mHandler.post(runnable);
                    startActivity(new Intent(TrimAudioActivity.this, SuccessActivity.class).putExtra("FILE_NAME", fileName));;
                    TrimAudioActivity.this.finish();
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() ->
                            Toast.makeText(TrimAudioActivity.this, getString(R.string.save_audio_failed), Toast.LENGTH_SHORT).show());
                } finally {
                    savingRecordDialog.dismiss();
                }
            }
        }.start();
    }

    private void showExistingFileDialog(File outFile, Runnable runnable) {
        LayoutInflater inflater = LayoutInflater.from(this);
        final View dialogView = inflater.inflate(R.layout.alert_dialog_override_file, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setView(dialogView);

        Button btnOk = dialogView.findViewById(R.id.btnOk);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        Objects.requireNonNull(btnOk).setOnClickListener(v -> {
            outFile.delete();
            startSavingAudio(outFile, runnable);
            alertDialog.dismiss();
            savingRecordDialog.dismiss();
        });

        Objects.requireNonNull(btnCancel).setOnClickListener(v -> {
            alertDialog.cancel();
            savingRecordDialog.dismiss();
        });

        alertDialog.show();
    }

    /**
     * Start recording
     *
     * @param openAudioDialog
     */
    private void showTrimmingEditor(ProgressDialog openAudioDialog) {
        Bundle bundle = getIntent().getBundleExtra(Common.EXTRA_BUNDLE);
        Uri uri = bundle.getParcelable(Common.EXTRA_AUDIO_URI);
        Utility.log(uri.toString());
        new Thread() {
            @Override
            public void run() {
                isAudioRecording = false;
                mRecordingKeepGoing = false;
                try {
                    mAudioFile = SoundFile.create(uri.getPath(), null);
                    updateAudioTitle(FilenameUtils.getName(uri.getPath()));
                } catch (IOException | SoundFile.InvalidInputException e) {
                    e.printStackTrace();
                }
                if (mAudioFile != null) {
                    mPlayer = new SamplePlayer(mAudioFile);
                }
                audioWaveform.setIsDrawBorder(true);
                runOnUiThread(() -> {
                    finishOpeningSoundFile(mAudioFile);
                    openAudioDialog.dismiss();
                });
            }
        }.start();
    }

    /**
     * After recording finish do necessary steps
     *
     * @param mSoundFile sound file
     */
    private void finishOpeningSoundFile(SoundFile mSoundFile) {
        audioWaveform.setSoundFile(mSoundFile);
        audioWaveform.recomputeHeights(mDensity);

        mMaxPos = audioWaveform.maxPos();
        mLastDisplayedStartPos = -1;
        mLastDisplayedEndPos = -1;

        mTouchDragging = false;

        mOffset = 0;
        mOffsetGoal = 0;
        mFlingVelocity = 0;
        resetPositions();
        if (mEndPos > mMaxPos)
            mEndPos = mMaxPos;

        updateDisplay();
    }

    /**
     * Update views
     */

    private synchronized void updateDisplay() {
        if (mIsPlaying) {
            int now = mPlayer.getCurrentPosition();
            int frames = audioWaveform.millisecsToPixels(now);
            audioWaveform.setPlayback(frames);
            Log.e("mWidth >> ", "" + mWidth);
            setOffsetGoalNoUpdate(frames - mWidth / 2);
            if (now >= mPlayEndMillSec) {
                handlePause();
            }
        }

        if (!mTouchDragging) {
            int offsetDelta;

            if (mFlingVelocity != 0) {
                offsetDelta = mFlingVelocity / 30;
                if (mFlingVelocity > 80) {
                    mFlingVelocity -= 80;
                } else if (mFlingVelocity < -80) {
                    mFlingVelocity += 80;
                } else {
                    mFlingVelocity = 0;
                }

                mOffset += offsetDelta;

                if (mOffset + mWidth / 2 > mMaxPos) {
                    mOffset = mMaxPos - mWidth / 2;
                    mFlingVelocity = 0;
                }
                if (mOffset < 0) {
                    mOffset = 0;
                    mFlingVelocity = 0;
                }
                mOffsetGoal = mOffset;
            } else {
                offsetDelta = mOffsetGoal - mOffset;

                if (offsetDelta > 10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta > 0)
                    offsetDelta = 1;
                else if (offsetDelta < -10)
                    offsetDelta = offsetDelta / 10;
                else if (offsetDelta < 0)
                    offsetDelta = -1;
                else
                    offsetDelta = 0;

                mOffset += offsetDelta;
            }
        }

        audioWaveform.setParameters(mStartPos, mEndPos, mOffset);
        audioWaveform.invalidate();

        markerStart.setContentDescription(" Start Marker" + formatTime(mStartPos));
        markerEnd.setContentDescription(" End Marker" + formatTime(mEndPos));

        int startX = mStartPos - mOffset - mMarkerLeftInset;
        if (startX + markerStart.getWidth() >= 0) {
            if (!mStartVisible) {
                mHandler.postDelayed(() -> {
                    mStartVisible = true;
                    markerStart.setAlpha(1f);
                }, 0);
            }
        } else {
            if (mStartVisible) {
                markerStart.setAlpha(0f);
                mStartVisible = false;
            }
            startX = 0;
        }

        int endX = mEndPos - mOffset - markerEnd.getWidth() + mMarkerLeftInset;
        if (endX + markerEnd.getWidth() >= 0) {
            if (!mEndVisible) {
                mHandler.postDelayed(() -> {
                    mEndVisible = true;
                    markerEnd.setAlpha(1f);
                }, 0);
            }
        } else {
            if (mEndVisible) {
                markerEnd.setAlpha(0f);
                mEndVisible = false;
            }
            endX = 0;
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(startX, markerEnd.getHeight() - mMarkerLeftInset * 2, 0, 0);
        markerStart.setLayoutParams(params);

        params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(endX, audioWaveform.getMeasuredHeight() + markerEnd.getHeight() / 2 - mMarkerLeftInset * 2, 0, 0);
        markerEnd.setLayoutParams(params);
    }

    /**
     * Reset all positions
     */

    private void resetPositions() {
        mStartPos = audioWaveform.secondsToPixels(0.0);
        mEndPos = audioWaveform.secondsToPixels(30.0);
        updateDisplay();
    }

    private void setOffsetGoalNoUpdate(int offset) {
        if (mTouchDragging) {
            return;
        }

        mOffsetGoal = offset;
        if (mOffsetGoal + mWidth / 2 > mMaxPos)
            mOffsetGoal = mMaxPos - mWidth / 2;
        if (mOffsetGoal < 0)
            mOffsetGoal = 0;
    }

    private String formatTime(int pixels) {
        if (audioWaveform != null && audioWaveform.isInitialized()) {
            return formatDecimal(audioWaveform.pixelsToSeconds(pixels));
        } else {
            return "";
        }
    }

    private String formatDecimal(double seconds) {
        //* seconds = 102.34 then time is 102sec : 34 millis
        long millis = (long) seconds * 1000; //* convert to milliseconds
        return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    private int trap(int pos) {
        if (pos < 0)
            return 0;
        if (pos > mMaxPos)
            return mMaxPos;
        return pos;
    }

    private void setOffsetGoalStart() {
        setOffsetGoal(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalStartNoUpdate() {
        setOffsetGoalNoUpdate(mStartPos - mWidth / 2);
    }

    private void setOffsetGoalEnd() {
        setOffsetGoal(mEndPos - mWidth / 2);
    }

    private void setOffsetGoalEndNoUpdate() {
        setOffsetGoalNoUpdate(mEndPos - mWidth / 2);
    }

    private void setOffsetGoal(int offset) {
        setOffsetGoalNoUpdate(offset);
        updateDisplay();
    }

    public void markerDraw() {
    }

    public void markerTouchStart(MarkerView marker, float x) {
        mTouchDragging = true;
        mTouchStart = x;
        mTouchInitialStartPos = mStartPos;
        mTouchInitialEndPos = mEndPos;
        handlePause();
    }

    public void markerTouchMove(MarkerView marker, float x) {
        float delta = x - mTouchStart;

        if (marker == markerStart) {
            mStartPos = trap((int) (mTouchInitialStartPos + delta));
            mEndPos = trap((int) (mTouchInitialEndPos + delta));
        } else {
            mEndPos = trap((int) (mTouchInitialEndPos + delta));
            if (mEndPos < mStartPos)
                mEndPos = mStartPos;
        }

        updateDisplay();
    }

    public void markerTouchEnd(MarkerView marker) {
        mTouchDragging = false;
        if (marker == markerStart) {
            setOffsetGoalStart();
        } else {
            setOffsetGoalEnd();
        }
    }

    public void markerLeft(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == markerStart) {
            int saveStart = mStartPos;
            mStartPos = trap(mStartPos - velocity);
            mEndPos = trap(mEndPos - (saveStart - mStartPos));
            setOffsetGoalStart();
        }

        if (marker == markerEnd) {
            if (mEndPos == mStartPos) {
                mStartPos = trap(mStartPos - velocity);
                mEndPos = mStartPos;
            } else {
                mEndPos = trap(mEndPos - velocity);
            }

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    public void markerRight(MarkerView marker, int velocity) {
        mKeyDown = true;

        if (marker == markerStart) {
            int saveStart = mStartPos;
            mStartPos += velocity;
            if (mStartPos > mMaxPos)
                mStartPos = mMaxPos;
            mEndPos += (mStartPos - saveStart);
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;

            setOffsetGoalStart();
        }

        if (marker == markerEnd) {
            mEndPos += velocity;
            if (mEndPos > mMaxPos)
                mEndPos = mMaxPos;

            setOffsetGoalEnd();
        }

        updateDisplay();
    }

    public void markerEnter(MarkerView marker) {
    }

    public void markerKeyUp() {
        mKeyDown = false;
        updateDisplay();
    }

    public void markerFocus(MarkerView marker) {
        mKeyDown = false;
        if (marker == markerStart) {
            setOffsetGoalStartNoUpdate();
        } else {
            setOffsetGoalEndNoUpdate();
        }

        // Delay updaing the display because if this focus was in
        // response to a touch event, we want to receive the touch
        // event too before updating the display.
        mHandler.postDelayed(this::updateDisplay, 100);
    }

//
// WaveformListener
//

    /**
     * Every time we get a message that our waveform drew, see if we need to
     * animate and trigger another redraw.
     */
    public void waveformDraw() {
        mWidth = audioWaveform.getMeasuredWidth();
        if (mOffsetGoal != mOffset && !mKeyDown)
            updateDisplay();
        else if (mIsPlaying) {
            updateDisplay();
        } else if (mFlingVelocity != 0) {
            updateDisplay();
        }
    }

    public void waveformTouchStart(float x) {
        mTouchDragging = true;
        mTouchStart = x;
        mTouchInitialOffset = mOffset;
        mFlingVelocity = 0;
//        long mWaveformTouchStartMsec = Utility.getCurrentTime();
    }

    public void waveformTouchMove(float x) {
        mOffset = trap((int) (mTouchInitialOffset + (mTouchStart - x)));
        updateDisplay();
    }

    public void waveformTouchEnd() {
//        /*mTouchDragging = false;
//        mOffsetGoal = mOffset;
//
//        long elapsedMsec = Utility.getCurrentTime() - mWaveformTouchStartMsec;
//        if (elapsedMsec < 300) {
//            if (mIsPlaying) {
//                int seekMsec = audioWaveform.pixelsToMillisecs(
//                        (int) (mTouchStart + mOffset));
//                if (seekMsec >= mPlayStartMsec &&
//                        seekMsec < mPlayEndMillSec) {
//                    mPlayer.seekTo(seekMsec);
//                } else {
////                    handlePause();
//                }
//            } else {
//                onPlay((int) (mTouchStart + mOffset));
//            }
//        }
    }

    private synchronized void handlePause() {
        imgAudioPlay.setImageResource(R.drawable.ic_play_circle);
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
        audioWaveform.setPlayback(-1);
        mIsPlaying = false;
    }

    private synchronized void onPlay(int startPosition) {
        if (mIsPlaying) {
            handlePause();
            return;
        }

        if (mPlayer == null) {
            // Not initialized yet
            return;
        }

        try {
            mPlayStartMsec = audioWaveform.pixelsToMillisecs(startPosition);
            if (startPosition < mStartPos) {
                mPlayEndMillSec = audioWaveform.pixelsToMillisecs(mStartPos);
            } else if (startPosition > mEndPos) {
                mPlayEndMillSec = audioWaveform.pixelsToMillisecs(mMaxPos);
            } else {
                mPlayEndMillSec = audioWaveform.pixelsToMillisecs(mEndPos);
            }
            mPlayer.setOnCompletionListener(this::handlePause);
            mIsPlaying = true;

            mPlayer.seekTo(mPlayStartMsec);
            mPlayer.start();
            updateDisplay();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void waveformFling(float vx) {
        mTouchDragging = false;
        mOffsetGoal = mOffset;
        mFlingVelocity = (int) (-vx);
        updateDisplay();
    }

    public void waveformZoomIn() {
        /*audioWaveform.zoomIn();
        mStartPos = audioWaveform.getStart();
        mEndPos = audioWaveform.getEnd();
        mMaxPos = audioWaveform.maxPos();
        mOffset = audioWaveform.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();*/
    }

    public void waveformZoomOut() {
        /*audioWaveform.zoomOut();
        mStartPos = audioWaveform.getStart();
        mEndPos = audioWaveform.getEnd();
        mMaxPos = audioWaveform.maxPos();
        mOffset = audioWaveform.getOffset();
        mOffsetGoal = mOffset;
        updateDisplay();*/
    }

    /**
     * After saving as ringtone set its content values
     *
     * @param title    title
     * @param outPath  output path
     * @param duration duration of file
     */
    private void afterSavingRingtone(CharSequence title, String outPath, int duration) {
        File outFile = new File(outPath);
        long fileSize = outFile.length();

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, outPath);
        values.put(MediaStore.MediaColumns.TITLE, title.toString());
        values.put(MediaStore.MediaColumns.SIZE, fileSize);
        values.put(MediaStore.MediaColumns.MIME_TYPE, Utility.AUDIO_MIME_TYPE);
        values.put(MediaStore.Audio.Media.ARTIST, getApplicationInfo().name);
        values.put(MediaStore.Audio.Media.DURATION, duration);
        values.put(MediaStore.Audio.Media.IS_MUSIC, true);

//        Uri uri = MediaStore.Audio.Media.getContentUriForPath(outPath);
//        final Uri newUri = getContentResolver().insert(uri, values);
//        Log.e("final URI >> ", newUri + " >> " + outPath);

//        Bundle conData = new Bundle();
//        conData.putString("INTENT_AUDIO_FILE", outPath);
//        Intent intent = getIntent();
//        intent.putExtras(conData);
//        setResult(RESULT_OK, intent);
        Toast.makeText(this, getString(R.string.saved_to) + outPath, Toast.LENGTH_LONG).show();
        updateAudioTitle(FilenameUtils.getName(outPath));
        loadFromFile(outPath);
    }

    /**
     * Load file from path
     *
     * @param mFilename file name
     */
    private void loadFromFile(String mFilename) {
        mFile = new File(mFilename);
        mLoadingLastUpdateTime = Utility.getCurrentTime();
        mLoadingKeepGoing = true;
        savingRecordDialog = new ProgressDialog(this);
        savingRecordDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        savingRecordDialog.setTitle("Opening cut file...");
        savingRecordDialog.show();

        final SoundFile.ProgressListener listener =
                fractionComplete -> {
                    long now = Utility.getCurrentTime();
                    if (now - mLoadingLastUpdateTime > 100) {
                        savingRecordDialog.setProgress(
                                (int) (savingRecordDialog.getMax() * fractionComplete));
                        mLoadingLastUpdateTime = now;
                    }
                    return mLoadingKeepGoing;
                };

        // Load the sound file in a background thread
        Thread mLoadSoundFileThread = new Thread() {
            public void run() {
                try {
                    mLoadedSoundFile = SoundFile.create(mFile.getAbsolutePath(), listener);
                    if (mLoadedSoundFile == null) {
                        savingRecordDialog.dismiss();
                        String name = mFile.getName().toLowerCase();
                        String[] components = name.split("\\.");
                        String err;
                        if (components.length < 2) {
                            err = "No Extension";
                        } else {
                            err = "Bad Extension";
                        }
                        final String finalErr = err;
                        Log.e(" >> ", "" + finalErr);
                        return;
                    }
                    mPlayer = new SamplePlayer(mLoadedSoundFile);
                } catch (final Exception e) {
                    savingRecordDialog.dismiss();
                    e.printStackTrace();
                    return;
                }
                savingRecordDialog.dismiss();
                if (mLoadingKeepGoing) {
                    Runnable runnable = () -> {
//                            audioWaveform.setVisibility(View.INVISIBLE);
//                            audioWaveform.setBackgroundColor(getResources().getColor(R.color.audio_cut_color));
                        audioWaveform.setIsDrawBorder(false);
                        mAudioFile = mLoadedSoundFile;
                        finishOpeningSoundFile(mLoadedSoundFile);
                    };
                    mHandler.post(runnable);
                }
            }
        };
        mLoadSoundFileThread.start();
    }


   

}
