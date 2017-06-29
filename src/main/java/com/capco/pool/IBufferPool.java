package com.capco.pool;

import java.nio.ByteBuffer;

/**
 * Created by Sridhar on 4/13/2017.
 */
public interface IBufferPool {
    ByteBuffer getNextByteBufferFree();
}
