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
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.google.gson.Gson;

import org.arpnetwork.arpclient.data.AVPacket;
import org.arpnetwork.arpclient.data.ConnectResponsePacket;
import org.arpnetwork.arpclient.data.Quality;
import org.arpnetwork.arpclient.data.UserInfo;
import org.arpnetwork.arpclient.data.ErrorInfo;
import org.arpnetwork.arpclient.data.Result;
import org.arpnetwork.arpclient.data.TouchSetting;
import org.arpnetwork.arpclient.data.TouchSettingPacket;
import org.arpnetwork.arpclient.data.VideoInfo;
import org.arpnetwork.arpclient.data.VideoInfoPacket;
import org.arpnetwork.arpclient.play.MediaPlayer;
import org.arpnetwork.arpclient.protocol.DeviceProtocol;
import org.arpnetwork.arpclient.protocol.ServerProtocol;
import org.arpnetwork.arpclient.touch.TouchHandler;
import org.arpnetwork.arpclient.util.PreferenceManager;
import org.json.JSONObject;

import java.util.HashMap;

public class ARPClient {
    private MediaPlayer mMediaPlayer;
    private TextureView mSurfaceView;
    private TouchHandler mTouchHandler;
    private DeviceProtocol mDeviceProtocol;

    private ARPClientListener mListener;
    private Handler mHandler;
    private Context mContext;
    private Gson mGson;

    private UserInfo mUserInfo;

    private boolean mConnected;
    private boolean mDisconnected;
    private boolean mReconnected;
    private boolean mClosed;
    private boolean mError;

    private Size mDisplaySize;

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
         * @param code error code, see {@link ErrorInfo}
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

    /**
     * Set quality.
     *
     * @param quality see #{@link Quality}
     */
    public static void setQuality(int quality) {
        if (quality > 0) {
            Quality.save(quality);
        }
    }

    public ARPClient(Context context, ARPClientListener listener) {
        mMediaPlayer = new MediaPlayer();
        mTouchHandler = new TouchHandler(mTouchHandlerListener);
        mDeviceProtocol = new DeviceProtocol(mProtocolProxyListener);
        mListener = listener;
        mContext = context;
        mHandler = new Handler();
        mGson = new Gson();
    }

    /**
     * Set surface view for video to render on
     *
     * @param view TextureView for render
     */
    public void setSurfaceView(TextureView view) {
        if (!(view.getParent() instanceof FrameLayout)) {
            throw new IllegalStateException("the parent of textureView must be a FrameLayout");
        }

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
     * Get remote device info and start connection
     *
     * @param condition remote device requirement
     */
    public void start(HashMap<String, Object> condition) {
        HashMap<String, Object> param = new HashMap<>();
        param.put("filters", condition);

        ServerProtocol.getUserInfo(mContext, new JSONObject(param).toString(), new ServerProtocol.OnReceiveUserInfo() {
            @Override
            public void onReceiveUserInfo(UserInfo info) {
                mUserInfo = info;
                open();
            }
        }, new ServerProtocol.OnServerProtocolError() {
            @Override
            public void onServerProtocolError(int code, String msg) {
                mListener.onError(code, msg == null ? ErrorInfo.getErrorMessage(code) : msg);
            }
        });
    }

    /**
     * Reconnect the remote device
     * Effective only in five seconds after disconnection
     */
    public void reconnect() {
        if (mDisconnected && !mClosed) {
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
        mDeviceProtocol.close();
        mMediaPlayer.removeCallbacks();
        mHandler.removeCallbacksAndMessages(null);
        mDisconnected = true;

        mMediaPlayer.stop();
    }

    /**
     * Submit stop request before closing socket
     * Once the method was called, remote device can not be reconnected
     */
    public void stop() {
        if (!mClosed && !mDisconnected) {
            if (mConnected) {
                mDeviceProtocol.sendStopReq();
                mConnected = false;
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disconnect();
                }
            }, 500);
            mClosed = true;
            mDisconnected = true;
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
        mDisconnected = false;
        mDeviceProtocol.open(mUserInfo.device.ip, mUserInfo.device.port, mUserInfo.session);
    }

    private void handleConnect() {
        mMediaPlayer.start();
        mReconnected = false;
        mConnected = true;
        mDeviceProtocol.sendConnectReq();
        mDeviceProtocol.sendTimestamp();
    }

    private int handleProtocolPacket(String data) {
        if (!TextUtils.isEmpty(data)) {
            Result result = mGson.fromJson(data, Result.class);

            switch (result.id) {
                case TouchSettingPacket.ID:
                    return handleTouchSettingPacket(data);

                case VideoInfoPacket.ID:
                    return handleVideoInfoPacket(data);

                case ConnectResponsePacket.ID:
                    return handleConnectResponsePacket(data);

                default:
                    break;
            }
        }
        return 0;
    }

    private int handleTouchSettingPacket(String data) {
        TouchSettingPacket touchSettingPacket = null;
        try {
            touchSettingPacket = mGson.fromJson(data, TouchSettingPacket.class);
        } catch (Exception e) {
            return ErrorInfo.ERROR_PROTOCOL_TOUCH_SETTING;
        }

        if (touchSettingPacket.data == null) {
            return ErrorInfo.ERROR_PROTOCOL_TOUCH_SETTING;
        }

        TouchSetting touchSetting = touchSettingPacket.data;
        mTouchHandler.setTouchSetting(touchSetting);
        return 0;
    }

    private int handleVideoInfoPacket(String data) {
        VideoInfoPacket videoInfoPacket = null;
        try {
            videoInfoPacket = mGson.fromJson(data, VideoInfoPacket.class);
        } catch (Exception e) {
            return ErrorInfo.ERROR_PROTOCOL_VIDEO_INFO;
        }

        final VideoInfo videoInfo = videoInfoPacket.data;

        if (videoInfo == null || videoInfo.width == 0 || videoInfo.height == 0
                || videoInfo.resolutionWidth == 0 || videoInfo.resolutionHeight == 0) {
            return ErrorInfo.ERROR_PROTOCOL_VIDEO_INFO;
        }

        if (mDisplaySize == null) {
            throw new IllegalStateException("surface view must be set when init and before calling start");
        }

        final Rect viewRect = videoInfo.getSurfaceViewRect(mDisplaySize.getWidth(),
                mDisplaySize.getHeight());
        setViewRect(viewRect);
        mTouchHandler.setVideoViewRect(
                videoInfo.getDisplayRect(viewRect.width(), viewRect.height()),
                videoInfo.statusBarHeight, videoInfo.virtualBarHeight);
        mMediaPlayer.setVideoSize(videoInfo.width, videoInfo.height);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mListener != null) {
                    mListener.onPrepared();
                }
            }
        });
        return 0;
    }

    private int handleConnectResponsePacket(String data) {
        ConnectResponsePacket responsePacket = null;
        try {
            responsePacket = mGson.fromJson(data, ConnectResponsePacket.class);
        } catch (Exception e) {
            return ErrorInfo.ERROR_CONNECTION_RESULT;
        }

        if (responsePacket == null || responsePacket.result != 0) {
            return ErrorInfo.ERROR_CONNECTION_REFUSED_VERSION;
        }

        ServerProtocol.setConnectionState(mContext, mUserInfo.id, UserInfo.STATE_CONNECTED);
        return 0;
    }

    private void handleError(int code, String msg) {
        if (mUserInfo != null) {
            if (mConnected) {
                ServerProtocol.setConnectionState(mContext, mUserInfo.id, UserInfo.STATE_DISCONNECT_ILLEGAL);
            } else {
                ServerProtocol.setConnectionState(mContext, mUserInfo.id, UserInfo.STATE_CONNECT_FAIL);
            }
        }
        mConnected = false;
        mError = true;
        if (mListener != null) {
            mListener.onError(code, msg == null ? ErrorInfo.getErrorMessage(code) : msg);
        }
    }

    private void setViewRect(Rect viewRect) {
        final FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(viewRect.width(), viewRect.height());
        params.leftMargin = viewRect.left;
        params.topMargin = viewRect.top;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSurfaceView.setLayoutParams(params);
            }
        });
    }

    private void setTransform(int width, int height) {
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

    private void setDisplaySize(int width, int height) {
        if (mDisplaySize == null) {
            // surface view must be full screen
            // get size of full view
            WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);

            // get height of status bar
            int statusBarHeight = -1;
            int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = mContext.getResources().getDimensionPixelSize(resourceId);
            }

            if (width < dm.widthPixels || height < (dm.heightPixels - statusBarHeight)) {
                throw new IllegalStateException("surface view must be full screen");
            }

            mDisplaySize = new Size(width, height);
        }
    }

    private final TouchHandler.OnTouchInfoListener mTouchHandlerListener = new TouchHandler.OnTouchInfoListener() {
        @Override
        public void onTouchInfo(String touchInfo) {
            mDeviceProtocol.sendTouchEvent(touchInfo);
        }
    };

    private final DeviceProtocol.OnProtocolListener mProtocolProxyListener = new DeviceProtocol.OnProtocolListener() {
        @Override
        public void onConnected() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!mDisconnected) {
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
            if (mUserInfo != null) {
                ServerProtocol.setConnectionState(mContext, mUserInfo.id, UserInfo.STATE_DISCONNECTED);
            }
            mListener.onClosed();
        }
    };

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setDisplaySize(width, height);
            setTransform(width, height);

            setSurface(new Surface(surfaceTexture));
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
