package me.evmanu.util;

import java.math.BigInteger;

public class Hex {

    public static String toHexString(byte[] array) {
        StringBuilder result = new StringBuilder();

        for (byte aByte : array) {

            result.append(String.format("%02x", aByte));

            // upper case
            // result.append(String.format("%02X", aByte));
        }

        return result.toString();
    }

    public static byte[] fromHexString(String hex) {
        return new BigInteger(hex, 16).toByteArray();
    }

}
