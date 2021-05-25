package me.evmanu.daos;

import java.security.Signature;

public interface Signable {

    void addToSignature(Signature signature);

}
