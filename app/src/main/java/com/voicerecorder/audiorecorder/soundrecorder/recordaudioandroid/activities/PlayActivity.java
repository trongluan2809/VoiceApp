package com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.axet.audiorecorder.R;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.callback.CustomClickListener;
import com.voicerecorder.audiorecorder.soundrecorder.recordaudioandroid.utils.SystemUtil;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PlayActivity extends AppCompatActivity {
    private static final String LOG_TAG = "ViewFileActivity";
    private static final String DATA_RECORD = "DATA_RECORD";
    private static final String ARG_ITEM = "recording_item";
    private Handler mHandler = new Handler();

    private MediaPlayer mMediaPlayer = null;
    private int seekForwardTime = 5000; // 5000 milliseconds
    private int seekBackwardTime = 5000; // 5000 milliseconds
    private SeekBar mSeekBar = null;
    private ImageView mPlayButton = null,imgBack;
    private TextView mCurrentProgressTextView = null;
    public static TextView mFileNameTextView = null;
    private TextView mtvFileLength = null;
    private TextView mtvDateAdded = null;
    private TextView mFileLengthTextView = null;
    Toolbar toolbar = null;
    TextView title = null;
    String mFileName = null;
    ImageView imgLoad;
    //stores whether or not the mediaplayer is currently playing audio
    private boolean isPlaying = false;

    //stores minutes and seconds of the length of the file.
    long minutes = 0;
    long seconds = 0;
    String pathAudio = "";
    String nameAudio = "";
    long lengthAudio = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set Language
        SystemUtil.setLocale(getBaseContext());

        setContentView(R.layout.activity_play);
        imgLoad = findViewById(R.id.imgLoad);

      //  Glide.with(this).asGif().load(R.drawable.gif_file).into(imgLoad);
        Bundle bundle= getIntent().getExtras();
        if(bundle!=null){
            nameAudio = getIntent().getExtras().getString("title","");
            pathAudio = getIntent().getExtras().getString("path","");
            lengthAudio = Integer.parseInt(getIntent().getExtras().getString("length","0"));
        }

        initView();
        clickContent();
        imgBack.setOnClickListener(v->{onBackPressed();});

    }


    private void initView() {
        mFileNameTextView = (TextView) findViewById(R.id.tvFileName);
        mtvFileLength = (TextView) findViewById(R.id.tvFileLength);
        imgBack = (ImageView) findViewById(R.id.icBack);
        mFileLengthTextView = (TextView) findViewById(R.id.file_length_text_view);
        mCurrentProgressTextView = (TextView) findViewById(R.id.current_progress_text_view);

        mSeekBar = (SeekBar) findViewById(R.id.seekbar);
/*        ColorFilter filter = new LightingColorFilter
                (getResources().getColor(R.color.primary), getResources().getColor(R.color.primary));
        mSeekBar.getProgressDrawable().setColorFilter(filter);
        mSeekBar.getThumb().setColorFilter(filter);*/

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mMediaPlayer != null && fromUser) {
                    mMediaPlayer.seekTo(progress);
                    mHandler.removeCallbacks(mRunnable);

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer.getCurrentPosition());
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer.getCurrentPosition())
                            - TimeUnit.MINUTES.toSeconds(minutes);
                    mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes, seconds));

                    updateSeekBar();

                } else if (mMediaPlayer == null && fromUser) {
                    prepareMediaPlayerFromPoint(progress);
                    updateSeekBar();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null) {
                    // remove message Handler from updating progress bar
                    mHandler.removeCallbacks(mRunnable);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mMediaPlayer.seekTo(seekBar.getProgress());

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer.getCurrentPosition());
                    long seconds = TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer.getCurrentPosition())
                            - TimeUnit.MINUTES.toSeconds(minutes);
                    mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes, seconds));
                    updateSeekBar();
                }
            }
        });

        mPlayButton = (ImageView) findViewById(R.id.fab_play);
        mPlayButton.setOnClickListener(new CustomClickListener() {
            @Override
            public void performClick(View v) {
                onPlay(isPlaying);
                isPlaying = !isPlaying;
            }
        });
        mtvFileLength.setText(formatDuration(lengthAudio));
        mFileNameTextView.setText(nameAudio);
        mFileLengthTextView.setText(formatDuration(lengthAudio));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pausePlaying();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayer = null;
    }

    private void pausePlaying() {
        mPlayButton.setBackgroundResource(R.drawable.ic_play_file);
        mHandler.removeCallbacks(mRunnable);
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    private void resumePlaying() {
        mPlayButton.setBackgroundResource(R.drawable.ic_pause_file);
        mHandler.removeCallbacks(mRunnable);
        mMediaPlayer.start();
        updateSeekBar();
    }

    private void stopPlaying() {
        mPlayButton.setBackgroundResource(R.drawable.ic_play_file);
        mHandler.removeCallbacks(mRunnable);
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        mMediaPlayer.release();
        mMediaPlayer = null;

        mSeekBar.setProgress(mSeekBar.getMax());
        isPlaying = !isPlaying;

        mCurrentProgressTextView.setText(mFileLengthTextView.getText());
        mSeekBar.setProgress(mSeekBar.getMax());

        //allow the screen to turn off again once audio is finished playing
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    //updating mSeekBar
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mMediaPlayer != null) {

                int mCurrentPosition = mMediaPlayer.getCurrentPosition();
                mSeekBar.setProgress(mCurrentPosition);

                long minutes = TimeUnit.MILLISECONDS.toMinutes(mCurrentPosition);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(mCurrentPosition)
                        - TimeUnit.MINUTES.toSeconds(minutes);
                mCurrentProgressTextView.setText(String.format("%02d:%02d", minutes, seconds));

                updateSeekBar();
            }
        }
    };

    private void updateSeekBar() {
        mHandler.postDelayed(mRunnable, 1000);
    }
    // Play start/stop
    private void onPlay(boolean isPlaying) {
        if (!isPlaying) {
            //currently MediaPlayer is not playing audio
            if (mMediaPlayer == null) {
                startPlaying(); //start from beginning
            } else {
                resumePlaying(); //resume the currently paused MediaPlayer
            }

        } else {
            //pause the MediaPlayer
            pausePlaying();
        }
    }

    private void startPlaying() {
        mPlayButton.setBackgroundResource(R.drawable.ic_pause_file);
        mMediaPlayer = new MediaPlayer();

        try {
            mMediaPlayer.setDataSource(pathAudio);
            mMediaPlayer.prepare();
            mSeekBar.setMax(mMediaPlayer.getDuration());

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mMediaPlayer.start();
                }
            });
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopPlaying();
            }
        });

        updateSeekBar();

        //keep screen on while playing audio
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void prepareMediaPlayerFromPoint(int progress) {
        //set mediaPlayer to start from middle of the audio file

        mMediaPlayer = new MediaPlayer();

        try {
            mMediaPlayer.setDataSource(pathAudio);
            mMediaPlayer.prepare();
            mSeekBar.setMax(mMediaPlayer.getDuration());
            mMediaPlayer.seekTo(progress);

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlaying();
                }
            });

        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        //keep screen on while playing audio
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    public static String formatDuration(long duration) {
        String hms;
        if (TimeUnit.MILLISECONDS.toHours(duration) > 0)
            hms = String.format(Locale.getDefault(), "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(duration),
                    TimeUnit.MILLISECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1));
        else
            hms = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1));
        return hms;
    }
    private void clickContent() {
        findViewById(R.id.btnTuaCham).setOnClickListener(new CustomClickListener() {
            @Override
            public void performClick(View v) {
                if (mMediaPlayer != null) {
                    int currentPosition = mMediaPlayer.getCurrentPosition();
                    // check if seekBackward time is greater than 0 sec
                    if (currentPosition - seekBackwardTime >= 0) {
                        // forward song
                        mMediaPlayer.seekTo(currentPosition - seekBackwardTime);
                    } else {
                        // backward to starting position
                        mMediaPlayer.seekTo(0);
                    }
                }
            }
        });
        findViewById(R.id.btnTuaNhanh).setOnClickListener(new CustomClickListener() {
            @Override
            public void performClick(View v) {
                if (mMediaPlayer != null) {
                    int currentPosition = mMediaPlayer.getCurrentPosition();
                    // check if seekForward time is lesser than song duration
                    if (currentPosition + seekForwardTime <= mMediaPlayer.getDuration()) {
                        // forward song
                        mMediaPlayer.seekTo(currentPosition + seekForwardTime);
                    } else {
                        // forward to end position
                        mMediaPlayer.seekTo(mMediaPlayer.getDuration());
                    }
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(this,MainActivity.class));
        finish();
    }
}