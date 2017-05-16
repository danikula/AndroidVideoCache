package com.danikula.videocache.encrypt;

/**
 * {@link Cipher} that does not perform any encryption/decryption.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class NoCipher implements Cipher {

    @Override
    public void encrypt(byte[] data, long length) {
    }

    @Override
    public void decrypt(byte[] data, long offset, int length) {
    }
}
