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

import android.os.Handler;
import android.view.Surface;

import org.arpnetwork.arpclient.data.AVPacket;

public class MediaPlayer {
    private VideoCodecThread mVideoThread;
    private AudioCodecThread mAudioThread;

    private Surface mSurface;

    private Handler mHandler;

    private MediaPlayerListener mListener;

    public MediaPlayer() {
        mHandler = new Handler();
    }

    public interface MediaPlayerListener {
        void onFirstFrameShow();

        void onError(int code, String msg);
    }

    /**
     * Set surface for video to render on
     *
     * @param surface for render
     */
    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    /**
     * Init video decode thread with call back
     *
     * @param listener MediaPlayerListener
     */
    public void initThreadWithListener(MediaPlayerListener listener) {
        mListener = listener;
        mAudioThread = new AudioCodecThread();
    }

    /**
     * Start audio decode thread.
     */
    public void startAudio() {
        mAudioThread.start();
    }

    /**
     * Start video decode thread.
     */
    public void startVideo() {
        if (mSurface != null && mVideoThread != null) {
            mVideoThread.start(mSurface);
        }
    }

    /**
     * Stop video decode thread.
     */
    public void stop() {
        if (mVideoThread != null) {
            mVideoThread.stop();
            mVideoThread = null;
        }

        if (mAudioThread != null) {
            mAudioThread.stop();
            mAudioThread = null;
        }

        mListener = null;
    }

    /**
     * Remove callbacks.
     */
    public void removeCallbacks() {
        mHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Put video packets for decoder.
     *
     * @param packet
     */
    public void putVideoPacket(AVPacket packet) {
        mVideoThread.putPacket(packet);
    }

    public void putAudioPacket(AVPacket packet) {
        mAudioThread.putPacket(packet);
    }

    /**
     * Change video size setting.
     *
     * @param videoW video width
     * @param videoH video height
     */
    public void setVideoSize(final int videoW, final int videoH) {
        if (mVideoThread == null) {
            initVideoThread(videoW, videoH);
        } else {
            if (videoW != mVideoThread.getWidth() || videoH != mVideoThread.getHeight()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mVideoThread.stop();
                        initVideoThread(videoW, videoH);
                    }
                });
            }
        }
    }

    private void initVideoThread(int videoW, int videoH) {
        mVideoThread = new VideoCodecThread(videoW, videoH);
        mVideoThread.setListener(mListener);
        startVideo();
    }
}
