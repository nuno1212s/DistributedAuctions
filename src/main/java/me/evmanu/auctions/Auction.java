package me.evmanu.auctions;

import lombok.Getter;
import lombok.Setter;
import me.evmanu.Standards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Auction {

    private byte[] auctionId;
    private String auctionName;

    private long startingBid;
    private long minimumBid;

    private long auctionDuration; // in minutes
    private long initialTs;
    private long finalTs;

    @Setter
    private List<Bid> listOfBids;

    private byte[] auctioneer;
    private byte[] signature;

    public Auction(String auctionName, long openingBid, long minimumBid, long auctionDuration) {
        this.auctionName = auctionName;
        this.startingBid = openingBid;
        this.auctionDuration = auctionDuration;
        this.minimumBid = minimumBid;
        this.initialTs = getCurrentTime();
        this.finalTs = addMinutesToTimestamp(initialTs, auctionDuration);
        this.auctionId = generateId(auctionName, this.initialTs);
        this.listOfBids = new ArrayList<>();
    }

    private byte[] generateId(String auctionName, long initialTs) {

        String s = String.valueOf(initialTs);

        byte[] name = auctionName.getBytes();
        byte[] ts = s.getBytes();

        byte[] merge = Standards.concatenateTwoBytes(name, ts);

        return Arrays.copyOfRange(Standards.calculateHashedFromByte(merge), 0, 20);
    }

    private long getCurrentTime() {
        return System.currentTimeMillis() / 1000;
    }

    private long addMinutesToTimestamp(long initial, long min) {
        return initial + (min * 60);
    }

    public Auction getAuctionById(byte[] auctionId) {

        // TODO: get auction from network

        return null;
    }
    public static void main(String Args[]) {

        Auction ibizaAuction = new Auction("Leilao da Ibiza", 10, 100, 60);

    }


}
