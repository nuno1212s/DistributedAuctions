package me.evmanu.messages.messagetypes;

import lombok.Getter;
import lombok.Setter;
import me.evmanu.blockchain.Signable;
import me.evmanu.messages.MessageContent;
import me.evmanu.messages.MessageType;

import java.security.Signature;
import java.security.SignatureException;

@Getter
public class RequestPaymentMessage extends MessageContent implements Signable {

    private final byte[] auctionID;

    private final byte[] bidID;

    private final byte[] destinationPubKeyHash;

    @Setter
    private byte[] signedByAuctioneer;

    public RequestPaymentMessage(byte[] auctionID, byte[] bidID, byte[] destinationPubKeyHash) {
        this.auctionID = auctionID;
        this.bidID = bidID;
        this.destinationPubKeyHash = destinationPubKeyHash;
    }

    @Override
    public void addToSignature(Signature signature) throws SignatureException {
        signature.update(this.auctionID);
        signature.update(this.bidID);
        signature.update(this.destinationPubKeyHash);
    }

    @Override
    public MessageType getType() {
        return MessageType.REQUEST_PAYMENT;
    }
}
