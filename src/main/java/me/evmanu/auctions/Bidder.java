package me.evmanu.auctions;

import lombok.Getter;
import me.evmanu.Standards;

import java.security.PublicKey;
import java.util.List;

@Getter
public class Bidder {

    byte[] userId; // will be the public key

    private String name;

    private float balance;

    //private List<Auction> auctions; // tendo a lista de transações na wallet, é necssario ter a list de auction que o utilizador esta a participar?

    // private Wallet userWallet;


}
