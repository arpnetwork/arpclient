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
import android.util.Log;
import android.view.Surface;

import org.arpnetwork.arpclient.data.AVPacket;
import org.arpnetwork.arpclient.data.ErrorInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

abstract class MediaCodecThread implements Runnable {
    private static final String TAG = MediaCodecThread.class.getSimpleName();
    private static final int MAX_PACKETS = 15;

    private Thread mCodecThread;
    private MediaCodec mMediaCodec;
    private RenderThread mRenderThread;

    private int mPacketQueueCapacity;
    private boolean mStopped;

    private LinkedBlockingQueue<AVPacket> mPacketQueue = new LinkedBlockingQueue<AVPacket>();
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();

    private boolean mFirstRendered = false;
    private MediaPlayer.MediaPlayerListener mListener;

    public MediaCodecThread() {
        this(MAX_PACKETS);
    }

    public MediaCodecThread(int capacity) {
        mPacketQueueCapacity = capacity;
        mCodecThread = new Thread(this);
        mStopped = true;
    }

    public void setListener(MediaPlayer.MediaPlayerListener listener) {
        mListener = listener;
    }

    public void start() {
        start(null);
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

            mRenderThread = new RenderThread();
            mRenderThread.start();

            mCodecThread.start();
        }
    }

    /**
     * Stop media codec thread
     */
    public synchronized void stop() {
        if (!mStopped) {
            mStopped = true;
            mRenderThread.interrupt();
            mCodecThread.interrupt();
            try {
                mRenderThread.join();
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

                queueInputBuffer(packet);
                synchronized (mRenderThread) {
                    mRenderThread.notify();
                }
            } catch (InterruptedException ignored) {
            } catch (Exception ignored) {
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
        } catch (IOException ignored) {
        }

        if (mMediaCodec != null) {
            MediaFormat mediaFormat = createMediaFormat();
            if (mediaFormat != null) {
                mMediaCodec.configure(mediaFormat, surface, null, 0);
                mMediaCodec.start();
            }
        }
    }

    private void queueInputBuffer(AVPacket packet) {
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(packet.data, 0, packet.size);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, packet.size, packet.pts, 0);
        } else {
            Log.e(TAG, "queueInputBuffer. inputBufferIndex = " + inputBufferIndex);
        }
    }

    private boolean render() {
        while (!mStopped) {
            // Get output buffer index
            try {
                int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, 15);
                if (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                    boolean rendered = onRender(mBufferInfo, outputBuffer);
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, !rendered);
                    if (!mFirstRendered && mListener != null) {
                        mListener.onFirstFrameShow();
                        mFirstRendered = true;
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    onFormatChanged(mMediaCodec.getOutputFormat());
                }
            } catch (Exception e) {
                if (mListener != null) {
                    mListener.onError(ErrorInfo.ERROR_MEDIA, ErrorInfo.getErrorMessage(ErrorInfo.ERROR_MEDIA));
                }
                break;
            }
        }
        return true;
    }

    private class RenderThread extends Thread {
        @Override
        public void run() {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
            render();
        }
    }
}