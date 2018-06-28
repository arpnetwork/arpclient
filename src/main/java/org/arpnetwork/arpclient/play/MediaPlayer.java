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
    private Surface mSurface;

    private Handler mHandler;

    private boolean mVideoThreadStart;

    public MediaPlayer() {
        mHandler = new Handler();
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
     * Init video decode thread.
     */
    public void initThread() {
        mVideoThread = new VideoCodecThread();
    }

    /**
     * Start video decode thread.
     */
    public void start() {
        if (mSurface != null && !mVideoThreadStart) {
            mVideoThread.start(mSurface);
            mVideoThreadStart = true;
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

    /**
     * Change video size setting.
     *
     * @param videoW video width
     * @param videoH video height
     */
    public void setVideoSize(final int videoW, final int videoH) {
        if (videoW != mVideoThread.getWidth() || videoH != mVideoThread.getHeight()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mVideoThread.stop();
                    mVideoThread = new VideoCodecThread(videoW, videoH);
                    mVideoThread.start(mSurface);
                }
            });
        }
    }
}
