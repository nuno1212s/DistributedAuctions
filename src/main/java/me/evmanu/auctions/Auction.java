package me.evmanu.auctions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.evmanu.Standards;
import me.evmanu.blockchain.Signable;
import me.evmanu.p2p.kademlia.P2PNode;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

@Getter
public class Auction implements Signable {

    private final byte[] auctionId;
    private final byte[] auctioneerNodeID;
    private final String auctionName;

    private final float minimumBid;

    private final long auctionDuration; // in minutes
    private final long initialTs;

    private final byte[] auctioneerPK;

    @Setter(value = AccessLevel.PRIVATE)
    private byte[] signature;

    public Auction(String auctionName, byte[] auctioneerNodeID, float minimumBid, long initialTs, long auctionDuration, byte[] auctioneer,
                   byte[] signature) {
        this.auctioneerPK = auctioneer;
        this.auctioneerNodeID = auctioneerNodeID;
        this.signature = signature;
        this.auctionName = auctionName;
        this.auctionDuration = auctionDuration;
        this.minimumBid = minimumBid;
        this.initialTs = initialTs;
        this.auctionId = generateId(auctionName, this.initialTs);
    }

    public Auction(String auctionName, byte[] auctioneerNodeID, float minimumBid, long auctionDuration, byte[] auctioneer) {
        this.auctionName = auctionName;
        this.auctioneerNodeID = auctioneerNodeID;
        this.minimumBid = minimumBid;
        this.auctionDuration = auctionDuration;
        this.initialTs = System.currentTimeMillis();
        this.auctioneerPK = auctioneer;

        this.auctionId = generateId(auctionName, this.initialTs);
    }

    public long getFinalTs() {
        return this.initialTs + this.auctionDuration;
    }

    private byte[] generateId(String auctionName, long initialTs) {

        String s = String.valueOf(initialTs);

        byte[] name = auctionName.getBytes();
        byte[] ts = s.getBytes(StandardCharsets.UTF_8);

        byte[] merge = Standards.concatenateTwoBytes(name, ts);

        return Arrays.copyOfRange(Standards.calculateHashedFromByte(merge), 0, 20);
    }

    @Override
    public void addToSignature(Signature signature) throws SignatureException {

        signature.update(this.auctionName.getBytes(StandardCharsets.UTF_8));

        signature.update(this.auctionId);

        signature.update(this.auctioneerNodeID);

        var auction = ByteBuffer.allocate(Long.BYTES * 2 + Float.BYTES);

        auction.putFloat(this.minimumBid);
        auction.putLong(this.initialTs);
        auction.putLong(this.auctionDuration);

        signature.update(auction.array());
    }

    public static Auction initializeNewAuction(P2PNode node, String auctionName, long durationInMillis, float minBid, KeyPair ownerKeys) {
        var auction = new Auction(auctionName, node.getNodeID(),
                minBid, durationInMillis, ownerKeys.getPublic().getEncoded());

        auction.setSignature(Signable.calculateSignatureOf(auction, ownerKeys.getPrivate()));

        return auction;
    }
}
