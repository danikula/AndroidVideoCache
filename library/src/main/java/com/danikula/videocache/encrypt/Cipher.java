package com.danikula.videocache.encrypt;

/**
 * Defines a encryption/decryption for cached data.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface Cipher {

    /**
     * Encrypts piece of data. Resulting byte[] array must have <b>same size<b/>.
     *
     * @param data   an input data to be encrypted.
     * @param length an length of data to be encrypted.
     */
    void encrypt(byte[] data, long length);

    /**
     * Decrypts piece of data. Resulting byte[] array must have <b>same size<b/>.
     *
     * @param data   an input data to be decrypted.
     * @param offset an offset of data to be decrypted.
     * @param length a length of data to be decrypted.
     */
    void decrypt(byte[] data, long offset, int length);
}
