package com.github.axet.androidlibrary.sound;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;

public class AudioTrack extends android.media.AudioTrack {
    public static final int SHORT_SIZE = Short.SIZE / Byte.SIZE;

    public long playStart = 0;
    public int len; // len in frames (stereo frames = len * 2)
    public int frames; // frames written to audiotrack (including zeros, stereo frames = frames)

    Handler playbackHandler = new Handler();
    Runnable playbackUpdate;
    OnPlaybackPositionUpdateListener playbackListener;

    int markerInFrames = -1;
    int periodInFrames = 1000;

    // AudioTrack unable to play shorter then 'min' size of data, fill it with zeros
    public static int getMinSize(int sampleRate, int c, int audioFormat, int b) {
        int min = android.media.AudioTrack.getMinBufferSize(sampleRate, c, audioFormat);
        if (b < min)
            b = min;
        return b;
    }

    // streamType AudioManager#STREAM_MUSIC
    // usage AudioAttributes#USAGE_MEDIA
    // ct AudioAttributes#CONTENT_TYPE_MUSIC
    public static AudioTrack create(int streamType, int usage, int ct, AudioBuffer buffer) {
        AudioTrack t = create(streamType, usage, ct, buffer, buffer.getBytesMin());
        t.write(buffer);
        return t;
    }

    public static AudioTrack create(int streamType, int usage, int ct, AudioParams buffer, int len) {
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes a = new AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(ct)
                    .build();
            return new AudioTrack(a, buffer, len);
        } else {
            return new AudioTrack(streamType, buffer, len);
        }
    }

    public static class AudioParams {
        public int hz; // sample rate
        public int c; // AudioFormat.CHANNEL_OUT_MONO or AudioFormat.CHANNEL_OUT_STEREO
        public int a; // AudioFormat.ENCODING_PCM_16BIT

        public int getChannels() {
            switch (c) {
                case AudioFormat.CHANNEL_OUT_MONO:
                    return 1;
                case AudioFormat.CHANNEL_OUT_STEREO:
                    return 2;
                default:
                    throw new RuntimeException("unknown mode");
            }
        }

        @TargetApi(21)
        public AudioFormat getAudioFormat() {
            AudioFormat.Builder builder = new AudioFormat.Builder();
            builder.setEncoding(Sound.DEFAULT_AUDIOFORMAT);
            builder.setSampleRate(hz);
            builder.setChannelMask(c);
            return builder.build();
        }
    }

    public static class AudioBuffer extends AudioParams {
        public short[] buffer; // buffer including zeros (to fill minimum size)
        public int len; // buffer length
        public int pos; // write AudioTrack pos

        public AudioBuffer(int sampleRate, int channelConfig, int audioFormat, short[] buf, int len) {
            this.hz = sampleRate;
            this.c = channelConfig;
            this.a = audioFormat;
            this.buffer = buf;
            this.len = len;
        }

        public AudioBuffer(int sampleRate, int channelConfig, int audioFormat, int len) {
            this.hz = sampleRate;
            this.c = channelConfig;
            this.a = audioFormat;

            int b = len * SHORT_SIZE;
            b = getMinSize(sampleRate, channelConfig, audioFormat, b);
            if (b <= 0)
                throw new RuntimeException("unable to get min size");
            int blen = b / SHORT_SIZE;

            this.len = len;
            this.buffer = new short[blen];
        }

        public AudioBuffer(int sampleRate, int channelConfig, int audioFormat) {
            this.hz = sampleRate;
            this.c = channelConfig;
            this.a = audioFormat;
            this.len = getMinSize(sampleRate, c, audioFormat, 0);
            if (len <= 0)
                throw new RuntimeException("unable to initialize audio");
            this.buffer = new short[len];
        }

        public void write(short[] buf, int pos, int len) {
            System.arraycopy(buf, pos, buffer, 0, len);
        }

        public void write(int pos, short s) {
            buffer[pos] = s;
        }

        public void write(int pos, short s1, short s2) {
            buffer[pos] = s1;
            buffer[pos + 1] = s2;
        }

        public void write(int pos, short... ss) {
            for (int i = 0; i < ss.length; i++)
                buffer[pos + i] = ss[i];
        }

        public void write(int pos, short s, int cn) {
            switch (cn) {
                case 1:
                    write(pos, s);
                    break;
                case 2:
                    write(pos, s, s);
                    break;
                default:
                    for (int i = 0; i < cn; i++)
                        buffer[pos + i] = s;
            }
        }

        public void reset() {
            pos = 0;
        }

        public int getBytesLen() {
            return buffer.length * SHORT_SIZE;
        }

        public int getBytesMin() {
            return getMinSize(hz, c, a, getBytesLen());
        }
    }

    public static class NotInitializedException extends RuntimeException {
    }

    // old phones bug.
    // http://stackoverflow.com/questions/27602492
    //
    // with MODE_STATIC setNotificationMarkerPosition not called
    public AudioTrack(int streamType, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes) throws IllegalArgumentException {
        super(streamType, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes, MODE_STREAM);
    }

    public AudioTrack(int streamType, AudioBuffer buffer) throws IllegalArgumentException {
        this(streamType, buffer, buffer.getBytesMin());
        write(buffer);
    }

    @TargetApi(21)
    public AudioTrack(AudioAttributes a, AudioBuffer buffer) throws IllegalArgumentException {
        this(a, buffer, buffer.getBytesMin());
        write(buffer);
    }

    public AudioTrack(int streamType, AudioParams buffer, int len) throws IllegalArgumentException {
        super(streamType, buffer.hz, buffer.c, buffer.a, len, MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (getState() != STATE_INITIALIZED)
            throw new NotInitializedException();
    }

    @TargetApi(21)
    public AudioTrack(AudioAttributes a, AudioParams buffer, int len) throws IllegalArgumentException {
        super(a, buffer.getAudioFormat(), len, MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (getState() != STATE_INITIALIZED)
            throw new NotInitializedException();
    }

    void playbackListenerUpdate() {
        if (playbackListener == null)
            return;
        if (playStart <= 0)
            return;

        int mark = 0;
        try {
            mark = getNotificationMarkerPosition();
        } catch (IllegalStateException ignore) { // Unable to retrieve AudioTrack pointer for getMarkerPosition()
        }

        if (mark <= 0 && markerInFrames >= 0) { // some old bugged phones unable to set markers
            playbackHandler.removeCallbacks(playbackUpdate);
            playbackUpdate = new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    if (markerInFrames >= 0) {
                        long playEnd = playStart + markerInFrames * 1000 / getSampleRate();
                        if (now >= playEnd) {
                            playbackListener.onMarkerReached(AudioTrack.this);
                            return;
                        }
                    }
                    playbackListener.onPeriodicNotification(AudioTrack.this);
                    long update = periodInFrames * 1000 / getSampleRate();

                    int len = getNativeFrameCount() * 1000 / getSampleRate(); // getNativeFrameCount() checking stereo fine
                    long end = len * 2 - (now - playStart);
                    if (update > end)
                        update = end;

                    playbackHandler.postDelayed(playbackUpdate, update);
                }
            };
            playbackUpdate.run();
        } else {
            playbackHandler.removeCallbacks(playbackUpdate);
            playbackUpdate = null;
        }
    }

    @Override
    public void release() {
        super.release();
        if (playbackUpdate != null) {
            playbackHandler.removeCallbacks(playbackUpdate);
            playbackUpdate = null;
        }
    }

    @Override
    public void play() throws IllegalStateException {
        super.play();
        playStart = System.currentTimeMillis();
        playbackListenerUpdate();
    }

    @Override
    public int setNotificationMarkerPosition(int markerInFrames) {  // do not check != AudioTrack.SUCCESS crash often
        this.markerInFrames = markerInFrames;
        return super.setNotificationMarkerPosition(markerInFrames);
    }

    @Override
    public int setPositionNotificationPeriod(int periodInFrames) {
        this.periodInFrames = periodInFrames;
        return super.setPositionNotificationPeriod(periodInFrames);
    }

    @Override
    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener) {
        super.setPlaybackPositionUpdateListener(listener);
        this.playbackListener = listener;
        playbackListenerUpdate();
    }

    @Override
    public void setPlaybackPositionUpdateListener(OnPlaybackPositionUpdateListener listener, Handler handler) {
        super.setPlaybackPositionUpdateListener(listener, handler);
        this.playbackListener = listener;
        if (handler != null) {
            this.playbackHandler.removeCallbacks(playbackUpdate);
            this.playbackHandler = handler;
        }
        playbackListenerUpdate();
    }

    @Override
    public int write(short[] audioData, int offsetInShorts, int sizeInShorts) {
        int out = super.write(audioData, offsetInShorts, sizeInShorts);
        if (out > 0) {
            this.len += out / getChannelCount();
            this.frames += out;
        }
        return out;
    }

    public int write(AudioBuffer buf) {
        int out = write(buf, buf.pos, buf.buffer.length - buf.pos); // use 'buffer.length' instead of 'len'
        if (out > 0) {
            buf.pos += out;
            this.len += out / getChannelCount();
            this.frames += out;
        }
        return out;
    }

    public int write(AudioBuffer buffer, int pos, int len) {
        return write(buffer.buffer, pos, len);
    }
}
