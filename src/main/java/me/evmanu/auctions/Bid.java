package me.evmanu.auctions;

import lombok.Getter;

@Getter
public class Bid {

    // TODO: encriptar a bid com a public key do auctioneer

    private byte[] userNodeId;

    private float bidAmount;

    private byte[] auctionId;

    public Bid(float bidAmount) {
        this.bidAmount = bidAmount;
    }

    public Bid(byte[] userNodeId, float bidAmount, byte[] auctionId) {
        this.userNodeId = userNodeId;
        this.bidAmount = bidAmount;
        this.auctionId = auctionId;
    }

    // TODO: da lhe nuno

    /**
     * boolean true: bid was successfully submitted
     *
     */
    public boolean sendBid() {



        return false;
    }

}
