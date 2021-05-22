package me.evmanu.util;

import lombok.Getter;

import java.util.Arrays;

@Getter
public class ByteWrapper {

    private final byte[] bytes;

    public ByteWrapper(byte[] bytes) {
        this.bytes = bytes;
    }

    @Override
    public boolean equals(Object rhs) {

        if (rhs == null) return false;

        if (rhs instanceof byte[]) {
            return Arrays.equals(bytes, (byte[]) rhs);
        } else
            return rhs instanceof ByteWrapper
                    && Arrays.equals(bytes, ((ByteWrapper) rhs).bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
