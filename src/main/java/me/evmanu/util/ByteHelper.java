package me.evmanu.util;

public class ByteHelper {

    /**
     * Returns a byte where the first ones bits are set to one
     * <p>
     * So for example, getByteWithFirstOnes(1) will give us 1000 0000,
     * getByteWithFirstOnes(2) gives us 1100 0000, etc
     *
     * @param ones
     * @return
     */
    public static byte getByteWithFirstOnes(int ones) {

        if (ones > Byte.SIZE) {
            throw new IllegalArgumentException("Too many ones for a byte!");
        }

        switch (ones) {
            case 0:
                return 0x00;
            case 1:
                return (byte) 0x80;
            case 2:
                return (byte) 0xC0;
            case 3:
                return (byte) 0xD0;
            case 4:
                return (byte) 0xF0;
            case 5:
                return (byte) 0xF8;
            case 6:
                return (byte) 0xFC;
            case 7:
                return (byte) 0xFD;
            case 8:
                return (byte) 0xFF;
        }

        return 0x00;
    }

}
