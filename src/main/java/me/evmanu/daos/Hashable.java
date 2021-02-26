package me.evmanu.daos;

import me.evmanu.Standards;

import java.security.MessageDigest;

public interface Hashable {

    void addToHash(MessageDigest hash);

    static byte[] calculateHashOf(Hashable hashable) {
        MessageDigest digest = Standards.getDigestInstance();

        assert digest != null;

        hashable.addToHash(digest);

        return digest.digest();
    }

}
