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

package org.arpnetwork.arpclient.data;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;

public class Message {
    public static final int HEARTBEAT = -1;
    public static final int VIDEO = 0;
    public static final int AUDIO = 1;
    public static final int TOUCH = 2;
    public static final int PROTOCOL = 3;
    public static final int TIME = 4;
    public static final int KEY = 6;

    private int mType;
    private byte[] mData;

    public Message(int type) {
        this(type, new byte[0]);
    }

    public Message(int type, byte[] data) {
        mType = type;
        mData = data;
    }

    /**
     * @return message type
     */
    public int getType() {
        return mType;
    }

    /**
     * @return message data buffer
     */
    public ByteBuffer getDataBuffer() {
        return ByteBuffer.wrap(mData);
    }

    public static Message readFrom(ByteBuf buf) {
        int size = buf.readInt();
        if (size == 0) {
            return new Message(HEARTBEAT);
        }

        int type = buf.readByte();
        ByteBuf body = buf.readBytes(size - 1);
        return new Message(type, body.array());
    }

    public void writeTo(ByteBuf buf) {
        if (mType == HEARTBEAT) {
            buf.writeInt(0); // heartbeat
        } else {
            // size = (byte)type size + data size
            buf.writeInt(1 + mData.length);
            buf.writeByte((byte) mType);
            buf.writeBytes(mData);
        }
    }
}
