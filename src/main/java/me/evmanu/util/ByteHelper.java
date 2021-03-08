package me.evmanu.util;

public class ByteHelper {

    /**
     * Returns a byte where the first ones bits are set to one
     * <p>
     * So for example, getByteWithFirstOnes(1) will give us 1000 0000,
     * getByteWithFirstOnes(2) gives us 1100 0000m, etc
     *
     * @param ones
     * @return
     */
    public static byte getByteWithFirstOnes(int ones) {

        byte initialByte = 0x00;

        for (int i = 0; i < Byte.SIZE; i++) {

            if (i < ones) {
                initialByte |= 0x01;
            }

            initialByte = (byte) (initialByte << 1);
        }

        return initialByte;
    }

}
