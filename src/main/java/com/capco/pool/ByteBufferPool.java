package com.capco.pool;

import java.nio.ByteBuffer;

/**
 * Created by Sridhar on 4/13/2017.
 */
public class ByteBufferPool implements IBufferPool{
    @Override
    public ByteBuffer getNextByteBufferFree() {
        return ByteBuffer.allocate(2000);
    }

}
