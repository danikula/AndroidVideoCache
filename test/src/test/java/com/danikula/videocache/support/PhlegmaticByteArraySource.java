package com.danikula.videocache.support;

import com.danikula.videocache.ByteArraySource;
import com.danikula.videocache.ProxyCacheException;

import java.util.Random;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
@Deprecated // TODO: use Mockito to mock delay
public class PhlegmaticByteArraySource extends ByteArraySource {

    private final Random delayGenerator;
    private final int maxDelayMs;

    public PhlegmaticByteArraySource(byte[] data, int maxDelayMs) {
        super(data);
        this.maxDelayMs = maxDelayMs;
        this.delayGenerator = new Random(System.currentTimeMillis());
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        try {
            Thread.sleep(delayGenerator.nextInt(maxDelayMs));
        } catch (InterruptedException e) {
            throw new ProxyCacheException("Error sleeping", e);
        }
        return super.read(buffer);
    }
}
