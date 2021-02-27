package me.evmanu.daos.users;

/**
 * This class refers to a bitcoin address that we can send currency to.
 * It is composed of the hashed public key that belongs to user and extra verifications
 * To assure that the data has not suffered any modifications during travel
 */
public class Address {

    private byte[] destinationHashedPublicKey;

}
