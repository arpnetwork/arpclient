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

import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Surface;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.arpnetwork.arpclient.data.AVPacket;
import org.arpnetwork.arpclient.data.ErrorCode;
import org.arpnetwork.arpclient.data.Result;
import org.arpnetwork.arpclient.data.VideoInfoPacket;
import org.arpnetwork.arpclient.protocol.ProtocolProxy;

public class MediaPlayer implements ProtocolProxy.OnProtocolListener {
    private static final int VIDEO_SETTING = 101;

    private VideoCodecThread mVideoThread;
    private ProtocolProxy mProtocolProxy;
    private Surface mSurface;
    private Gson mGson;

    private Uri mUri;
    private Handler mHandler;

    private boolean mReconnected;
    private boolean mClosed;
    private boolean mConnected;
    private boolean mDisconnect;
    private boolean mError;
    private boolean mVideoThreadStart;

    private OnMediaPlayerListener mOnMediaPlayerListener;

    public interface OnMediaPlayerListener {
        /**
         * Called when the video is ready for play.
         */
        void onPrepared(MediaPlayer player);

        /**
         * Socket closed
         */
        void onClose();

        /**
         * Play error
         * @param errorCode See {@link org.arpnetwork.arpclient.data.ErrorCode}
         * @param msg Error details
         */
        void onError(int errorCode, String msg);
    }

    public MediaPlayer() {
        mHandler = new Handler();
        mGson = new Gson();
        mProtocolProxy = new ProtocolProxy(this);
    }

    /**
     * Set media player callback
     * @param listener Media player callback
     */
    public void setOnMediaPlayerListener(OnMediaPlayerListener listener) {
        mOnMediaPlayerListener = listener;
    }

    /**
     * Set URI for connection
     * @param uri Format: arp://IP:PORT, example: arp://192.168.1.1:8080
     */
    public void setDataSource(Uri uri) {
        mUri = uri;
    }

    /**
     * Set surface for video to render on
     * @param surface for render
     */
    public void setSurface(Surface surface) {
        mSurface = surface;
        if (mConnected) {
            start();
        }
    }

    /**
     * Prepare video data asynchronously
     */
    public void prepareAsync() {
        if (!mDisconnect) {
            doOpen();
        }
    }

    /**
     * Reconnect the remote device
     * Effective only in five seconds after disconnection
     */
    public void reconnect() {
        if (mDisconnect && !mClosed) {
            mReconnected = true;
            doOpen();
        }
    }

    /**
     * Close socket, reset player
     * Can be reconnect in five seconds
     */
    public void disconnect() {
        mSurface = null;
        mProtocolProxy.close();
        mHandler.removeCallbacksAndMessages(null);
        mDisconnect = true;

        reset();
    }

    /**
     * Submit stop request before closing socket
     * Once the method was called, remote device can not be reconnected
     */
    public void stop() {
        if (!mClosed && !mDisconnect) {
            if (mConnected) {
                mProtocolProxy.sendStopReq();
                mConnected = false;
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            }, 500);
            mClosed = true;
            mDisconnect = true;
        }
    }

    @Override
    public void onConnected() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mDisconnect) {
                    handleConnect();
                }
            }
        });
    }

    @Override
    public void onError(final int code, final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mClosed && !mError) {
                    handleError(code, msg);
                }
            }
        });
    }

    @Override
    public void onVideoPacket(AVPacket packet) {
        if (!mDisconnect) {
            mVideoThread.putPacket(packet);
        }
    }

    @Override
    public int onProtocolPacket(String data) {
        if (!TextUtils.isEmpty(data)) {
            Result result = mGson.fromJson(data, Result.class);

            switch (result.id) {
                case VIDEO_SETTING:
                    VideoInfoPacket videoInfoData = null;
                    try {
                        videoInfoData = mGson.fromJson(data, VideoInfoPacket.class);
                    } catch (JsonSyntaxException e) {
                        return ErrorCode.ERROR_PROTOCOL_VIDEO_INFO;
                    }

                    if (videoInfoData != null && videoInfoData.data != null) {
                        final int videoW = videoInfoData.data.width;
                        final int videoH = videoInfoData.data.height;
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

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mOnMediaPlayerListener != null) {
                                    mOnMediaPlayerListener.onPrepared(MediaPlayer.this);
                                }
                            }
                        });
                    }
                    break;

                default:
                    break;
            }
        }
        return 0;
    }

    @Override
    public void onClosed() {
        mOnMediaPlayerListener.onClose();
    }

    private void handleConnect() {
        start();
        mProtocolProxy.sendConnectReq();
        mReconnected = false;
        mConnected = true;
    }

    private void handleError(int code, String msg) {
        mConnected = false;
        if (!mReconnected) {
            disconnect();
            reconnect();
        } else {
            mError = true;
            if (mOnMediaPlayerListener != null) {
                mOnMediaPlayerListener.onError(code, msg);
            }
        }
    }

    private void start() {
        if (mSurface != null && !mVideoThreadStart) {
            mVideoThread.start(mSurface);
            mVideoThreadStart = true;
        }
    }

    private void reset() {
        if (mVideoThread != null) {
            mVideoThread.stop();
            mVideoThread = null;
        }
    }

    private void doOpen() {
        mVideoThread = new VideoCodecThread();
        mClosed = false;
        mDisconnect = false;
        mProtocolProxy.open(mUri.getHost(), mUri.getPort());
    }
}
