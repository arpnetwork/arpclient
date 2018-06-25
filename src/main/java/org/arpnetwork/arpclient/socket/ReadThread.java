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
import java.io.IOException;

/**
 * This thread runs during a connection with a remote device.
 * It handles all incoming transmissions.
 */
class ReadThread extends Thread {
    private DataInputStream mInputStream;
    private boolean mStopped;

    private OnReadListener mListener;

    public interface OnReadListener {
        /**
         * Received socket data
         * @param bytes Received data
         */
        void onReceive(byte[] bytes);

        /**
         * Read stream error
         * @param errorCode ErrorCode.READ_STREAM_ERROR
         * @param message Read error
         */
        void onError(int errorCode, String message);
    }

    public ReadThread(DataInputStream inputStream, OnReadListener listener) {
        mInputStream = inputStream;
        mListener = listener;
        mStopped = false;
    }

    /**
     * Cancel thread
     */
    public void cancel() {
        mStopped = true;
    }

    @Override
    public void run() {
        int BUFFER_SIZE = 8 * 1024;
        int readCount;
        byte[] buffer = new byte[BUFFER_SIZE];

        // Keep listening to the InputStream while createSocket
        while (!mStopped) {
            try {
                int dataSize = mInputStream.readInt();
                if (dataSize != 0) {
                    byte[] dataBuffer = new byte[dataSize];
                    int dataRead = 0;
                    int frameOffset = 0;
                    while (dataRead < dataSize) {
                        if (dataSize - dataRead > BUFFER_SIZE) {
                            readCount = mInputStream.read(buffer);
                        } else {
                            readCount = mInputStream.read(buffer, 0, dataSize - dataRead);
                        }
                        if (readCount == -1) {
                            mListener.onError(ErrorCode.READ_STREAM_ERROR, "read error");
                            break;
                        }

                        dataRead += readCount;

                        System.arraycopy(buffer, 0, dataBuffer, frameOffset, readCount);
                        frameOffset += readCount;
                    }
                    if (dataRead == dataSize) {
                        mListener.onReceive(dataBuffer);
                    }
                }
            } catch (IOException e) {
                if (!mStopped) {
                    mListener.onError(ErrorCode.READ_STREAM_ERROR, e.getMessage());
                }
                break;
            }
        }
    }
}
