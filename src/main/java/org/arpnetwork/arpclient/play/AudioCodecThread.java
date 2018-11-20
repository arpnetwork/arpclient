/*
 * Copyright 2018 ARP Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arpnetwork.arpclient.play;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class AudioCodecThread extends MediaCodecThread {

    private static final String MIME_TYPE = "audio/mp4a-latm"; // AAC
    private static final int SAMPLE_RATE = 44100;
    private static final int CAPACITY = 10;

    private AudioTrack mAudioTrack;

    public AudioCodecThread() {
        super(CAPACITY);
    }

    @Override
    protected String mimeType() {
        return MIME_TYPE;
    }

    @Override
    protected MediaFormat createMediaFormat() {
        return makeAACCodecSpecificData(MediaCodecInfo.CodecProfileLevel.AACObjectLC, SAMPLE_RATE, 2);
    }

    @Override
    protected void onStart() {
        int bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);
        setAudioTrackVolume(mAudioTrack, 1.0f);
        mAudioTrack.play();
    }

    @Override
    protected void onStop() {
        mAudioTrack.stop();
        mAudioTrack.release();
        mAudioTrack = null;
    }

    @Override
    protected void onFormatChanged(MediaFormat mediaFormat) {
        mAudioTrack.setPlaybackRate(mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
    }

    @Override
    protected boolean onRender(MediaCodec.BufferInfo info, ByteBuffer buffer) {
        final byte[] chunk = new byte[info.size];
        buffer.get(chunk); // Read the buffer all at once
        buffer.clear();    // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN

        mAudioTrack.write(chunk, info.offset, info.offset + info.size); // AudioTrack write data

        return true;
    }

    /**
     * The code profile, Sample rate, channel Count is used to
     * produce the AAC Codec SpecificData.
     * Android 4.4.2/frameworks/av/media/libstagefright/avc_utils.cpp refer
     * to the portion of the code written.
     * <p/>
     * MPEG-4 Audio refer : http://wiki.multimedia.cx/index.php?title=MPEG-4_Audio#Audio_Specific_Config
     *
     * @param audioProfile  is MPEG-4 Audio Object Types
     * @param sampleRate
     * @param channelConfig
     * @return MediaFormat
     */
    private MediaFormat makeAACCodecSpecificData(int audioProfile, int sampleRate, int channelConfig) {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig);

        format.setInteger(MediaFormat.KEY_IS_ADTS, 1);

        int samplingFreq[] = {
                96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
                16000, 12000, 11025, 8000
        };

        // Search the Sampling Frequencies
        int sampleIndex = -1;
        for (int i = 0; i < samplingFreq.length; ++i) {
            if (samplingFreq[i] == sampleRate) {
                sampleIndex = i;
            }
        }

        if (sampleIndex == -1) {
            return null;
        }

        ByteBuffer csd = ByteBuffer.allocate(2);
        csd.put((byte) ((audioProfile << 3) | (sampleIndex >> 1)));

        csd.position(1);
        csd.put((byte) ((byte) ((sampleIndex << 7) & 0x80) | (channelConfig << 3)));
        csd.flip();
        format.setByteBuffer("csd-0", csd); // add csd-0

        return format;
    }

    @SuppressWarnings("deprecation")
    private static void setAudioTrackVolume(AudioTrack audioTrack, float volume) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioTrack.setVolume(volume);
        } else {
            audioTrack.setStereoVolume(volume, volume);
        }
    }
}
