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

import android.os.Handler;
import android.os.Looper;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * This thread runs during a connection with a remote device.
 * It handles all outgoing transmissions.
 */
class WriteThread extends Thread {
    private DataOutputStream mOutputStream;
    private List<ByteBuffer> mBufferList;

    private boolean mStopped;

    private Handler mHeartbeatHandler = new Handler(Looper.getMainLooper());

    public WriteThread(DataOutputStream outputStream) {
        mOutputStream = outputStream;
        mBufferList = new ArrayList<>();
        mStopped = false;
    }

    /**
     * Write socket output data
     * @param buffer Output data
     */
    public void write(ByteBuffer buffer) {
        mBufferList.add(buffer);
        synchronized (this) {
            notify();
        }
    }

    /**
     * Cancel thread
     */
    public synchronized void cancel() {
        mStopped = true;
        mHeartbeatHandler.removeCallbacksAndMessages(null);
        interrupt();
    }

    @Override
    public void run() {
        sendHeartbeat();
        while (!mStopped) {
            if (mBufferList.size() == 0) {
                try {
                    synchronized (this) {
                        if (mStopped) {
                            break;
                        }
                        wait();
                    }
                } catch (InterruptedException e) {
                    continue;
                }
            }
            ByteBuffer buffer = mBufferList.remove(0);
            try {
                if (buffer.capacity() == 4) {
                    mOutputStream.writeInt(0);
                } else {
                    byte[] bytes = new byte[buffer.capacity()]; // transform ByteBuffer into byte[]
                    buffer.get(bytes);
                    mOutputStream.write(bytes);
                }
                mOutputStream.flush();
            } catch (IOException e) {
            }
        }
    }

    private void sendHeartbeat() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4);
        byteBuffer.putInt(0);
        write(byteBuffer);
        mHeartbeatHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendHeartbeat();
            }
        }, 5000);
    }
}
