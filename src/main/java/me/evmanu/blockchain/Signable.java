package me.evmanu.blockchain;

import java.security.Signature;

public interface Signable {

    void addToSignature(Signature signature);

}
