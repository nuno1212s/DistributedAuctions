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

    public static byte[] xor(byte[] xor1, byte[] xor2) {
        assert xor1.length == xor2.length;

        byte[] result = new byte[xor1.length];

        for (int i = 0; i < xor1.length; i++) {

            result[i] = (byte) (xor1[i] ^ xor2[i]);

        }

        return result;
    }

    public static boolean hasFirstBitsSetToZero(byte[] toVerify, int zeroes) {

        for (int block = 0; block < (int) Math.ceil(zeroes / (float) Byte.SIZE); block++) {
            byte byteBlock = toVerify[block];

            //Get the amount of zeroes that are required for this byte, taking into account that the previous
            //Bytes were already checked
            int zeroesRequiredInThisBlock = Math.min(zeroes - (block * Byte.SIZE), 8);

            final var byteWithFirstOnes = ByteHelper.getByteWithFirstOnes(zeroesRequiredInThisBlock);

            //TO check if the first x bits are 0, we do an and with a byte where the first x bits are 1,
            //And the rest are 0, so when we AND them, we must get 0
            //Example: 0001 1111 & 1110 0000 = 0
            if ((byteBlock & byteWithFirstOnes) != 0) {
                return false;
            }
        }

        return true;
    }

}
