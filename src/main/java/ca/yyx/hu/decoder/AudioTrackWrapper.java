package ca.yyx.hu.decoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import ca.yyx.hu.utils.AppLog;


/**
 * @author algavris
 * @date 26/10/2016.
 */
class AudioTrackWrapper {
    private final AudioTrack audioTrack;

    AudioTrackWrapper(int sampleRateInHz, int channelCount) {
        audioTrack = createAudioTrack(sampleRateInHz, channelCount);
        audioTrack.play();
    }

    private AudioTrack createAudioTrack(int sampleRateInHz, int channelCount) {
        int pcmFrameSize = 2 * channelCount;
        int channelConfig = channelCount == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;

        int bufferSize = AudioBuffer.getSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT, pcmFrameSize);
        AppLog.logd("Audio buffer size: " + bufferSize + " sampleRateInHz: " + sampleRateInHz + " channelCount: " + channelCount);

        return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz,channelConfig, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
    }

    int write(byte[] buffer, int offset, int size) {
        int written = audioTrack.write(buffer, offset, size);
        if (written != size) {
            AppLog.loge("Error AudioTrack written: " + written + "  len: " + size);
        }
        return written;
    }

    void stop() {
        int playState = audioTrack.getPlayState();
        if (playState == android.media.AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.pause();
        }
        final AudioTrack toRelease = audioTrack;
        // AudioTrack.release can take some time, so we call it on a background thread.
        new Thread() {
            @Override
            public void run() {
                toRelease.flush();
                toRelease.release();
            }
        }.start();
    }

    private static class AudioBuffer {
        /**
         * A minimum length for the {@link android.media.AudioTrack} buffer, in microseconds.
         */
        private static final long MIN_BUFFER_DURATION_US = 250000;
        /**
         * A multiplication factor to apply to the minimum buffer size requested by the underlying
         * {@link android.media.AudioTrack}.
         */
        private static final int BUFFER_MULTIPLICATION_FACTOR = 4;
        /**
         * A maximum length for the {@link android.media.AudioTrack} buffer, in microseconds.
         */
        private static final long MAX_BUFFER_DURATION_US = 750000;

        /**
         * The number of microseconds in one second.
         */
        private static final long MICROS_PER_SECOND = 1000000L;

        static int getSize(int sampleRate,int channelConfig,int audioFormat, int pcmFrameSize)
        {
            int minBufferSize = android.media.AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
            int multipliedBufferSize = minBufferSize * BUFFER_MULTIPLICATION_FACTOR;
            int minAppBufferSize = (int) durationUsToFrames(MIN_BUFFER_DURATION_US, sampleRate) * pcmFrameSize;
            int maxAppBufferSize = (int) Math.max(minBufferSize,
                    durationUsToFrames(MAX_BUFFER_DURATION_US, sampleRate) * pcmFrameSize);
            return multipliedBufferSize < minAppBufferSize ? minAppBufferSize
                    : multipliedBufferSize > maxAppBufferSize ? maxAppBufferSize
                    : multipliedBufferSize;
        }

        private static long durationUsToFrames(long durationUs, int sampleRate) {
            return (durationUs * sampleRate) / MICROS_PER_SECOND;
        }
    }
}