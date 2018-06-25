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
package org.arpnetwork.arpclient.socket;

import org.arpnetwork.arpclient.data.ErrorCode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

public class SocketService extends Thread implements ReadThread.OnReadListener {
    private Socket mSocket;
    private DataInputStream mInputStream;
    private DataOutputStream mOutputStream;

    private ReadThread mReadThread;
    private WriteThread mWriteThread;

    private final OnConnectionListener mConnectionListener;

    private String mIp;
    private int mPort;

    public SocketService(OnConnectionListener listener) {
        mConnectionListener = listener;
    }

    /**
     * Start the ReadThread to begin a connection, and manage it
     */
    public void createSocket(String ip, int port) {
        close(true);

        this.mIp = ip;
        this.mPort = port;

        start();
    }

    /**
     * Send socket request
     * @param byteBuffer Request content
     */
    public void write(ByteBuffer byteBuffer) {
        mWriteThread.write(byteBuffer);
    }

    /**
     * Close socket
     */
    public void close() {
        close(false);
    }

    @Override
    public void run() {
        try {
            mSocket = new Socket(mIp, mPort);
            mInputStream = new DataInputStream(mSocket.getInputStream());
            mOutputStream = new DataOutputStream(mSocket.getOutputStream());
        } catch (IOException e) {
            if (mConnectionListener != null) {
                mConnectionListener.onError(ErrorCode.NETWORK_ERROR, e.getMessage());
            }
            return;
        }

        mReadThread = new ReadThread(mInputStream, this);
        mReadThread.start();

        mWriteThread = new WriteThread(mOutputStream);
        mWriteThread.start();

        if (mConnectionListener != null) {
            mConnectionListener.onConnected();
        }
    }

    @Override
    public void onReceive(byte[] bytes) {
        mConnectionListener.onReceiveData(ByteBuffer.wrap(bytes));
    }

    @Override
    public void onError(int errorCode, String message) {
        mConnectionListener.onError(errorCode, message);
    }

    private void close(boolean isReset) {
        try {
            // Close streams
            if (mInputStream != null) {
                mInputStream.close();
            }
            if (mOutputStream != null) {
                mOutputStream.close();
            }

            // Close socket
            if (mSocket != null) {
                mSocket.close();
            }

            // Cancel any thread currently running a connection
            if (mReadThread != null) {
                mReadThread.cancel();
                mReadThread = null;
            }

            if (mWriteThread != null) {
                mWriteThread.cancel();
                mWriteThread = null;
            }
        } catch (IOException e) {
        }
        if (mConnectionListener != null && !isReset) {
            mConnectionListener.onClosed();
        }
    }
}
