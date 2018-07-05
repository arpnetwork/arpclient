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

import android.media.MediaFormat;

class VideoCodecThread extends MediaCodecThread {
    private static final String MIME_TYPE = "video/avc"; // H.264
    public static final int WIDTH = 720;
    public static final int HEIGHT = 1280;

    private int mWidth;
    private int mHeight;

    public VideoCodecThread() {
        this(WIDTH, HEIGHT);
    }

    public VideoCodecThread(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * @return Width of video
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * @return Height of video
     */
    public int getHeight() {
        return mHeight;
    }

    @Override
    protected String mimeType() {
        return MIME_TYPE;
    }

    @Override
    protected MediaFormat createMediaFormat() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        // add the following setting to fix crash bugs on Galaxy Nexus
        // http://stackoverflow.com/questions/15105843/mediacodec-jelly-bean
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        return mediaFormat;
    }
}
