package org.arpnetwork.arpclient.data;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;

public class Message {
    public static final int HEARTBEAT = -1;
    public static final int VIDEO = 0;
    public static final int AUDIO = 1;
    public static final int TOUCH = 2;
    public static final int PROTOCOL = 3;

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
        if(size == 0) {
            return new Message(HEARTBEAT);
        }

        int type = buf.readByte();

        ByteBuf body = buf.readBytes(size - 1);
        return new Message(type, body.array());
    }

    public void writeTo(ByteBuf buf) {
        if (mType == HEARTBEAT) {
            buf.writeInt(0); //heartbeat
        } else {
            buf.writeInt(1 + mData.length); //size = (byte)type size + data size
            buf.writeByte((byte) mType); //type
            buf.writeBytes(mData); //data
        }
    }
}
