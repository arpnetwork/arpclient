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

package org.arpnetwork.arpclient.protocol;

import android.os.Handler;

import com.google.gson.Gson;

import org.arpnetwork.arpclient.data.AVPacket;
import org.arpnetwork.arpclient.data.ConnectReq;
import org.arpnetwork.arpclient.data.ErrorCode;
import org.arpnetwork.arpclient.data.Message;
import org.arpnetwork.arpclient.data.StopReq;
import org.arpnetwork.arpclient.socket.NettyConnection;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class ProtocolProxy implements NettyConnection.ConnectionListener {
    private static final int HEARTBEAT_INTERVAL = 5000;

    private Gson mGson;
    private NettyConnection mConnection;
    private OnProtocolListener mListener;

    private Handler mHeartbeatHandler = new Handler();

    public interface OnProtocolListener {
        /**
         * Socket connected
         */
        void onConnected();

        /**
         * Socket error
         *
         * @param code See {@link org.arpnetwork.arpclient.data.ErrorCode}
         * @param msg  Error details
         */
        void onError(int code, String msg);

        /**
         * Received video packet
         *
         * @param packet Video packet
         */
        void onVideoPacket(AVPacket packet);

        /**
         * Received protocol packet
         *
         * @param data Protocol json string
         * @return protocol packet error
         */
        int onProtocolPacket(String data);

        /**
         * Socket closed
         */
        void onClosed();
    }

    public ProtocolProxy(OnProtocolListener listener) {
        mConnection = new NettyConnection(this);
        mListener = listener;
        mGson = new Gson();
    }

    /**
     * Open socket connection
     *
     * @param host Socket ip
     * @param port Socket port
     */
    public void open(String host, int port) {
        mConnection.connect(host, port);
    }

    /**
     * Close socket connection
     */
    public void close() {
        mConnection.close();
    }

    /**
     * Send a connection request to remote device after socket connected
     */
    public void sendConnectReq() {
        sendRequest(mGson.toJson(new ConnectReq(null)), Message.PROTOCOL);
    }

    /**
     * Send touch event commands.
     *
     * @param touchInfo event commands
     */
    public void sendTouchEvent(String touchInfo) {
        sendRequest(touchInfo, Message.TOUCH);
    }

    /**
     * Send a stop request to remote device before socket closed
     * Once the stop request was sent, there is no way to reconnect to the same device
     */
    public void sendStopReq() {
        sendRequest(mGson.toJson(new StopReq()), Message.PROTOCOL);
    }

    @Override
    public void onConnected(NettyConnection conn) {
        mListener.onConnected();
        sendHeartbeat();
    }

    @Override
    public void onClosed(NettyConnection conn) {
        mHeartbeatHandler.removeCallbacksAndMessages(null);
        mListener.onClosed();
    }

    @Override
    public void onMessage(NettyConnection conn, Message msg) {
        switch (msg.getType()) {
            case Message.VIDEO:
                mListener.onVideoPacket(getPacket(msg.getDataBuffer()));
                break;

            case Message.PROTOCOL:
                int errorCode = mListener.onProtocolPacket(getString(msg.getDataBuffer()));
                if (errorCode != 0) {
                    mListener.onError(errorCode, "parse protocol packet error");
                }
                break;

            case Message.TIME:
                // FIXME
                break;

            default:
                break;
        }
    }

    @Override
    public void onException(NettyConnection conn, Throwable cause) {
        mListener.onError(ErrorCode.ERROR_NETWORK, cause.getMessage());
    }

    private void sendRequest(String request, int type) {
        byte[] bytes = request.getBytes();
        Message msg = new Message((byte) type, bytes);
        mConnection.write(msg);
    }

    private void sendHeartbeat() {
        Message msg = new Message((byte) Message.HEARTBEAT);
        mConnection.write(msg);
        mHeartbeatHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendHeartbeat();
            }
        }, HEARTBEAT_INTERVAL);
    }

    private static AVPacket getPacket(ByteBuffer data) {
        long pts = data.getLong();
        int size = data.capacity() - data.position();
        byte[] packetData = new byte[size];
        data.get(packetData);
        return new AVPacket(pts, packetData, size);
    }

    private static String getString(ByteBuffer buffer) {
        try {
            Charset charset = Charset.forName("UTF-8");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer charBuffer = decoder.decode(buffer.asReadOnlyBuffer());
            // to avoid the space before json string
            return charBuffer.toString().trim();
        } catch (Exception ex) {
            return "";
        }
    }
}


