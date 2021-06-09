package me.evmanu.auctions;

import lombok.Getter;
import me.evmanu.Standards;
import me.evmanu.blockchain.Hashable;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.MessageDigest;

@Getter
public class Bid implements Hashable {

    private final byte[] bidID;

    private final byte[] userNodeId;

    private final byte[] auctionId;

    private final byte[] userPublicKey;

    private final byte[] encryptedBid;

    public Bid(byte[] userNodeId, byte[] userPublicKey, byte[] encryptedBid, byte[] auctionId) {
        this.userNodeId = userNodeId;
        this.userPublicKey = userPublicKey;
        this.encryptedBid = encryptedBid;
        this.auctionId = auctionId;

        this.bidID = Hashable.calculateHashOf(this);
    }

    public float getBidAmount(KeyPair auctionOwner) {

        var sharedSecret = Standards.generateSharedSecret(userPublicKey, auctionOwner.getPrivate());

        var decrypted = Standards.decryptWithPrefixIV(this.encryptedBid, sharedSecret);

        var wrapped = ByteBuffer.wrap(decrypted);

        return wrapped.getFloat();
    }

    public static Bid initializeBidFor(Auction auction, byte[] nodeID, KeyPair keyPair, float amount) {
        var sharedSecret = Standards.generateSharedSecret(auction.getAuctioneerPK(), keyPair.getPrivate());

        var allocate = ByteBuffer.allocate(Float.BYTES);

        allocate.putFloat(amount);

        var cipherText = Standards.encryptTextWithPrependedIV(allocate.array(), sharedSecret);

        return new Bid(nodeID, keyPair.getPublic().getEncoded(), cipherText,
                auction.getAuctionId());
    }

    @Override
    public void addToHash(MessageDigest hash) {
        hash.update(this.userNodeId);
        hash.update(this.auctionId);
        hash.update(this.userPublicKey);
        hash.update(this.encryptedBid);
    }
}
