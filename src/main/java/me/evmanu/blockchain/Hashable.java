package me.evmanu.blockchain;

import me.evmanu.Standards;

import java.security.MessageDigest;

public interface Hashable {

    /**
     * Add all the elements of this object to a hash that's being built.
     *
     * Should be recursive.
     *
     * @param hash
     */
    void addToHash(MessageDigest hash);

    static byte[] calculateHashOf(Hashable hashable) {
        MessageDigest digest = Standards.getDigestInstance();

        assert digest != null;

        hashable.addToHash(digest);

        return digest.digest();
    }

}
