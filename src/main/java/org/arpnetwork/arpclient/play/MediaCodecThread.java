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

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Surface;

import org.arpnetwork.arpclient.data.AVPacket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

abstract class MediaCodecThread implements Runnable {
    private Thread mCodecThread;
    private MediaCodec mMediaCodec;

    private int mPacketQueueCapacity;
    private boolean mStopped;

    private LinkedBlockingQueue<AVPacket> mPacketQueue = new LinkedBlockingQueue<AVPacket>();
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    public MediaCodecThread() {
        this(Integer.MAX_VALUE);
    }

    public MediaCodecThread(int capacity) {
        mPacketQueueCapacity = capacity;
        mCodecThread = new Thread(this);
        mStopped = true;
    }

    /**
     * Start media codec thread
     *
     * @param surface For video render
     */
    public synchronized void start(Surface surface) {
        initDecoder(surface);

        if (mMediaCodec != null) {
            mStopped = false;
            onStart();
            mCodecThread.start();
        }
    }

    /**
     * Stop media codec thread
     */
    public synchronized void stop() {
        if (!mStopped) {
            mStopped = true;
            mCodecThread.interrupt();
            try {
                mCodecThread.join();
            } catch (InterruptedException e) {
            }
            mPacketQueue.clear();
            releaseDecoder();

            onStop();
        }
    }

    /**
     * Enqueue media packet for codec
     *
     * @param packet Media packet for codec
     */
    public synchronized void putPacket(AVPacket packet) {
        if (mMediaCodec != null) {
            try {
                mPacketQueue.put(packet);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void run() {
        while (!mStopped) {
            try {
                AVPacket packet = mPacketQueue.take();
                while (mPacketQueue.size() > mPacketQueueCapacity) {
                    packet = mPacketQueue.take();
                }
                render(packet);
            } catch (InterruptedException e) {
            } catch (Exception e) {
            }
        }
    }

    protected abstract String mimeType();

    protected abstract MediaFormat createMediaFormat();

    protected void onStart() {
    }

    protected void onStop() {
    }

    protected void onFormatChanged(MediaFormat mediaFormat) {
    }

    protected boolean onRender(MediaCodec.BufferInfo info, ByteBuffer buffer) {
        return false;
    }

    private void releaseDecoder() {
        try {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
            }
        } catch (IllegalStateException e) {
        } finally {
            mMediaCodec = null;
        }
    }

    private void initDecoder(Surface surface) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(mimeType());
        } catch (IOException e) {
        }

        if (mMediaCodec != null) {
            MediaFormat mediaFormat = createMediaFormat();
            if (mediaFormat != null) {
                mMediaCodec.configure(mediaFormat, surface, null, 0);
                mMediaCodec.start();
            }
        }
    }

    private boolean render(AVPacket packet) {
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(packet.data, 0, packet.size);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, packet.size, packet.pts, 0);
        }

        while (!mStopped) {
            // Get output buffer index
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 0);

            if (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                boolean rendered = onRender(mBufferInfo, outputBuffer);
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, !rendered);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                onFormatChanged(mMediaCodec.getOutputFormat());
            } else {
                break;
            }
        }

        return true;
    }
}
