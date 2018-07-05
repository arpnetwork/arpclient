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

package org.arpnetwork.arpclient;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.arpnetwork.arpclient.data.AVPacket;
import org.arpnetwork.arpclient.data.ErrorCode;
import org.arpnetwork.arpclient.data.Result;
import org.arpnetwork.arpclient.data.TouchSetting;
import org.arpnetwork.arpclient.data.TouchSettingPacket;
import org.arpnetwork.arpclient.data.VideoInfoPacket;
import org.arpnetwork.arpclient.play.MediaPlayer;
import org.arpnetwork.arpclient.protocol.ProtocolProxy;
import org.arpnetwork.arpclient.touch.TouchHandler;
import org.arpnetwork.arpclient.util.PreferenceManager;

public class ARPClient {
    private static final int TOUCH_SETTING = 100;
    private static final int VIDEO_SETTING = 101;

    private MediaPlayer mMediaPlayer;
    private TextureView mSurfaceView;
    private TouchHandler mTouchHandler;
    private ProtocolProxy mProtocolProxy;

    private ARPClientListener mListener;
    private Handler mHandler;
    private Gson mGson;

    private Uri mUri;
    private Size mViewSize;

    private boolean mReconnected;
    private boolean mClosed;
    private boolean mConnected;
    private boolean mDisconnect;
    private boolean mError;

    public interface ARPClientListener {
        /**
         * Called when the video is ready for play.
         */
        void onPrepared();

        /**
         * Socket closed.
         */
        void onClosed();

        /**
         * Error occurred.
         *
         * @param code error code, see {@link org.arpnetwork.arpclient.data.ErrorCode}
         * @param msg
         */
        void onError(int code, String msg);
    }

    public static void init(Context context) {
        PreferenceManager.init(context);
    }

    public static void fini() {
        PreferenceManager.fini();
    }

    public ARPClient(ARPClientListener listener) {
        mMediaPlayer = new MediaPlayer();
        mTouchHandler = new TouchHandler(mTouchHandlerListener);
        mProtocolProxy = new ProtocolProxy(mProtocolProxyListener);
        mListener = listener;
        mHandler = new Handler();
        mGson = new Gson();
    }

    /**
     * Set surface view for video to render on
     *
     * @param view TextureView for render
     */
    public void setSurfaceView(TextureView view) {
        mSurfaceView = view;

        mSurfaceView.setFocusable(true);
        mSurfaceView.setKeepScreenOn(true);
        mSurfaceView.setSurfaceTextureListener(mSurfaceTextureListener);
        if (!Build.MANUFACTURER.equalsIgnoreCase("huawei")) {
            // Fix touch exception: ACTION_DOWN has not handle
            mSurfaceView.setOnClickListener(null);
        }
        mSurfaceView.setOnTouchListener(mOnTouchListener);
    }

    /**
     * Start connection
     *
     * @param uri Format: arp://IP:PORT, example: arp://192.168.1.1:8080
     */
    public void start(Uri uri) {
        mUri = uri;
        if (!mDisconnect) {
            open();
        }
    }

    /**
     * Reconnect the remote device
     * Effective only in five seconds after disconnection
     */
    public void reconnect() {
        if (mDisconnect && !mClosed) {
            mReconnected = true;
            open();
        }
    }

    /**
     * Close socket, stop player
     * Can be reconnect in five seconds
     */
    public void disconnect() {
        mMediaPlayer.setSurface(null);
        mProtocolProxy.close();
        mMediaPlayer.removeCallbacks();
        mHandler.removeCallbacksAndMessages(null);
        mDisconnect = true;

        mMediaPlayer.stop();
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

    /**
     * Set the orientation to adjust touch
     *
     * @param isLandscape true for landscape, default is false;
     */
    public void setLandscape(boolean isLandscape) {
        mTouchHandler.setLandscape(isLandscape);
    }

    private void setSurface(Surface surface) {
        mMediaPlayer.setSurface(surface);
        if (mConnected) {
            mMediaPlayer.start();
        }
    }

    private void open() {
        mMediaPlayer.initThread();
        mClosed = false;
        mDisconnect = false;
        mProtocolProxy.open(mUri.getHost(), mUri.getPort());
    }

    private void handleConnect() {
        mMediaPlayer.start();
        mReconnected = false;
        mConnected = true;
        mProtocolProxy.sendConnectReq();
    }

    private int handleProtocolPacket(String data) {
        if (!TextUtils.isEmpty(data)) {
            Result result = mGson.fromJson(data, Result.class);

            switch (result.id) {
                case TOUCH_SETTING:
                    TouchSetting touchSetting = null;
                    try {
                        TouchSettingPacket touchSettingPacket = mGson.fromJson(data, TouchSettingPacket.class);
                        touchSetting = touchSettingPacket.data;
                    } catch (JsonSyntaxException e) {
                        return ErrorCode.ERROR_PROTOCOL_TOUCH_SETTING;
                    }
                    touchSetting.setScreenSize(mViewSize);
                    mTouchHandler.setTouchSetting(touchSetting);
                    break;

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
                        mMediaPlayer.setVideoSize(videoW, videoH);

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mListener != null) {
                                    mListener.onPrepared();
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

    private void handleError(int code, String msg) {
        mConnected = false;
        if (!mReconnected && code == ErrorCode.ERROR_NETWORK) {
            disconnect();
            reconnect();
        } else {
            mError = true;
            if (mListener != null) {
                mListener.onError(code, msg);
            }
        }
    }

    private void setTransform(int width, int height) {
        mViewSize = new Size(width, height);
        if (width > height) {
            Matrix matrix = new Matrix();
            matrix.preScale(height * 1.0f / width, width * 1.0f / height, width / 2, height / 2);
            matrix.postRotate(-90, width / 2, height / 2);
            mSurfaceView.setTransform(matrix);

            setLandscape(true);
        } else {
            setLandscape(false);
        }
    }

    private final TouchHandler.OnTouchInfoListener mTouchHandlerListener = new TouchHandler.OnTouchInfoListener() {
        @Override
        public void onTouchInfo(String touchInfo) {
            mProtocolProxy.sendTouchEvent(touchInfo);
        }
    };

    private final ProtocolProxy.OnProtocolListener mProtocolProxyListener = new ProtocolProxy.OnProtocolListener() {
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
            mMediaPlayer.putVideoPacket(packet);
        }

        @Override
        public int onProtocolPacket(String data) {
            return handleProtocolPacket(data);
        }

        @Override
        public void onClosed() {
            mListener.onClosed();
        }
    };

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setTransform(width, height);

            setSurface(new Surface(surfaceTexture));
            reconnect();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            setTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            disconnect();
            if (surfaceTexture != null) {
                surfaceTexture.release();
            }
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };

    private final View.OnTouchListener mOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            return mTouchHandler.onTouchEvent(motionEvent);
        }
    };
}
