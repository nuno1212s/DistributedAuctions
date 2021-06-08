package me.evmanu.auctions;

import lombok.Getter;
import me.evmanu.Standards;

import java.security.PublicKey;
import java.util.List;

@Getter
public class Bidder {

    byte[] userId; // will be the public key
    private String name;
    private float walletAmount;

    private List<Auction> auctions;




}
