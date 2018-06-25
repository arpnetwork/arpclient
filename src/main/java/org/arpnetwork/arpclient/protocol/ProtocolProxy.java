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

import com.google.gson.Gson;

import org.arpnetwork.arpclient.data.AVPacket;
import org.arpnetwork.arpclient.data.ConnectReq;
import org.arpnetwork.arpclient.data.StopReq;
import org.arpnetwork.arpclient.socket.OnConnectionListener;
import org.arpnetwork.arpclient.socket.SocketService;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class ProtocolProxy implements OnConnectionListener {

    private static final int VIDEO = 0;
    private static final int PROTOCOL = 3;

    private OnProtocolListener mListener;
    private SocketService mSocketService;
    private Gson mGson;

    public interface OnProtocolListener {
        /**
         * Socket connected
         */
        void onConnected();

        /**
         * Socket error
         * @param code See {@link org.arpnetwork.arpclient.data.ErrorCode}
         * @param msg Error details
         */
        void onError(int code, String msg);

        /**
         * Received video packet
         * @param packet Video packet
         */
        void onVideoPacket(AVPacket packet);

        /**
         * Received protocol packet
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
        mSocketService = new SocketService(this);
        mListener = listener;
        mGson = new Gson();
    }

    /**
     * Open socket connection
     * @param ip Socket ip
     * @param port Socket port
     */
    public void open(String ip, int port) {
        mSocketService.createSocket(ip, port);
    }

    /**
     * Close socket connection
     */
    public void close() {
        mSocketService.close();
    }

    /**
     * Send a connection request to remote device after socket connected
     */
    public void sendConnectReq() {
        sendRequest(mGson.toJson(new ConnectReq(null)));
    }

    /**
     * Send a stop request to remote device before socket closed
     * Once the stop request was sent, there is no way to reconnect to the same device
     */
    public void sendStopReq() {
        sendRequest(mGson.toJson(new StopReq()));
    }

    @Override
    public void onConnected() {
        mListener.onConnected();
    }

    @Override
    public void onReceiveData(ByteBuffer data) {
        int type = data.get();
        switch (type) {
            case VIDEO:
                mListener.onVideoPacket(getPacket(data));
                break;

            case PROTOCOL:
                int errorCode = mListener.onProtocolPacket(getString(data));
                if (errorCode != 0) {
                    mListener.onError(errorCode, "parse protocol packet error");
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onError(int errorCode, String msg) {
        mListener.onError(errorCode, msg);
    }

    @Override
    public void onClosed() {
        mListener.onClosed();
    }

    private void sendRequest(String request) {
        byte[] bytes = request.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 5);
        byteBuffer.putInt(bytes.length + 1); //size
        byteBuffer.put((byte) PROTOCOL); //type
        byteBuffer.put(bytes); //data
        byteBuffer.flip();

        mSocketService.write(byteBuffer);
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


