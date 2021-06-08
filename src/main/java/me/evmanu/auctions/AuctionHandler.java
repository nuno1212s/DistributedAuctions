package me.evmanu.auctions;

import me.evmanu.Standards;
import me.evmanu.blockchain.BlockChainHandler;
import me.evmanu.blockchain.Signable;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.transactions.ScriptPubKey;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.messages.MessageHandler;
import me.evmanu.messages.messagetypes.AuctionMessage;
import me.evmanu.messages.messagetypes.BidMessage;
import me.evmanu.messages.messagetypes.RequestPaymentMessage;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;
import me.evmanu.p2p.nodeoperations.ContentLookupOperation;
import me.evmanu.p2p.nodeoperations.StoreOperation;
import me.evmanu.util.ByteWrapper;
import me.evmanu.util.Hex;
import me.evmanu.util.Pair;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionHandler {

    private final P2PNode node;

    private final BlockChainHandler chainHandler;

    private final MessageHandler messageHandler;

    private final List<Auction> ourActiveAuctions;

    private final List<byte[]> activeAuctions;

    private final Map<ByteWrapper, List<byte[]>> ourActiveBids, activeBids;

    private final Map<ByteWrapper, List<Bid>> auctionWaitingForPayment;

    private final Map<ByteWrapper, Integer> currentBidIndexForAuc;

    private final Map<ByteWrapper, KeyPair> paymentKeys;

    private final List<Pair<Pair<byte[], Long>, byte[]>> pendingPayments;

    public AuctionHandler(P2PNode node, MessageHandler messageHandler, BlockChainHandler chainHandler) {
        this.node = node;
        this.chainHandler = chainHandler;
        this.messageHandler = messageHandler;
        this.ourActiveAuctions = new LinkedList<>();
        this.activeAuctions = Collections.synchronizedList(new LinkedList<>());
        this.ourActiveBids = new ConcurrentSkipListMap<>();
        this.activeBids = new ConcurrentSkipListMap<>();
        this.currentBidIndexForAuc = new ConcurrentSkipListMap<>();
        this.auctionWaitingForPayment = new ConcurrentSkipListMap<>();
        this.paymentKeys = new ConcurrentSkipListMap<>();
        this.pendingPayments = new LinkedList<>();
    }

    public void handleRequestActiveAuctions(byte[] requestingNodeID) {
        for (Auction ourActiveAuction : ourActiveAuctions) {
            messageHandler.sendMessage(requestingNodeID, new AuctionMessage(ourActiveAuction.getAuctionId()));
        }
    }

    public void startNewAuction(Auction auction) {
        var message = this.messageHandler.serializeObject(auction);

        //Store the auction information in the p2p network
        new StoreOperation(node, new StoredKeyMetadata(auction.getAuctionId(),
                message, auction.getAuctioneerNodeID())).execute();

        this.ourActiveAuctions.add(auction);

        this.activeAuctions.add(auction.getAuctionId());

        messageHandler.publishMessage(new AuctionMessage(auction.getAuctionId()));
    }

    public void handleNewAuctionStarted(byte[] auctionID) {
        this.activeAuctions.add(auctionID);
    }

    public void handleNewBid(byte[] bid) {

        new ContentLookupOperation(node, bid, (response) -> {
            Bid bidObj = messageHandler.deserializeObject(response, Bid.class);

            this.activeBids.compute(new ByteWrapper(bidObj.getAuctionId()),
                    (auc, bids) -> {

                        if (bids == null) {
                            bids = new LinkedList<>();
                        }

                        bids.add(bid);

                        return bids;
                    });

        }).execute();

    }

    public void publishBid(Bid bid) {

        var message = this.messageHandler.serializeObject(bid);

        new StoreOperation(node, new StoredKeyMetadata(
                bid.getBidID(),
                message,
                bid.getUserNodeId()
        )).execute();

        appendOurBid(bid);
        appendGlobalBid(bid);

        messageHandler.publishMessage(new BidMessage(bid.getBidID()));
    }

    public void auctionEnded(byte[] auctionID) {
        var itr = this.activeAuctions.iterator();

        while (itr.hasNext()) {

            var next = itr.next();

            if (Arrays.equals(auctionID, next)) {
                itr.remove();
                break;
            }
        }

        var wrappedID = new ByteWrapper(auctionID);

        this.activeBids.remove(wrappedID);

        if (this.ourActiveBids.containsKey(wrappedID)) {

            for (byte[] bidID : this.ourActiveBids.get(wrappedID)) {
                node.deleteValue(bidID);
            }
        }
    }

    private void appendGlobalBid(Bid bid) {
        this.activeBids.compute(new ByteWrapper(bid.getAuctionId()), (curr, bidList) -> {

            if (bidList == null) {

                var nBidList = new LinkedList<byte[]>();

                nBidList.add(bid.getBidID());

                return nBidList;
            }

            bidList.add(bid.getBidID());

            return bidList;
        });
    }

    private void appendOurBid(Bid bid) {
        this.ourActiveBids.compute(new ByteWrapper(bid.getAuctionId()), (curr, bidList) -> {

            if (bidList == null) {

                var nBidList = new LinkedList<byte[]>();

                nBidList.add(bid.getBidID());

                return nBidList;
            }

            bidList.add(bid.getBidID());

            return bidList;
        });
    }

    public void finishAuction(Auction auction, KeyPair pair) {

        if (!Arrays.equals(auction.getAuctioneerPK(), pair.getPublic().getEncoded())) {
            System.out.println("Wrong keypair for the auction you want to end!");
            return;
        }

        //TODO: Maybe request a bid list before doing this?
        AtomicInteger waiting = new AtomicInteger();

        List<Bid> bids = Collections.synchronizedList(new LinkedList<>());

        var wrappedID = new ByteWrapper(auction.getAuctionId());
        if (this.activeBids.containsKey(wrappedID)) {
            for (byte[] bidID : this.activeBids.get(wrappedID)) {
                waiting.incrementAndGet();

                new ContentLookupOperation(node, bidID, (result) -> {

                    var bidObj = messageHandler.deserializeObject(result, Bid.class);

                    bids.add(bidObj);

                    if (waiting.decrementAndGet() == 0) {
                        finishedLoadingBids(auction, bids, pair);
                    }
                }).execute();
            }
        }
    }

    public void finishedLoadingBids(Auction auction, List<Bid> bids, KeyPair auctionOwner) {

        bids.sort(Comparator.<Bid>comparingDouble(bid -> bid.getBidAmount(auctionOwner)).reversed());

        var key = new ByteWrapper(auction.getAuctionId());
        this.paymentKeys.put(key, auctionOwner);

        this.currentBidIndexForAuc.put(new ByteWrapper(auction.getAuctionId()), 0);
        this.auctionWaitingForPayment.put(new ByteWrapper(auction.getAuctionId()),
                bids);

        requestPaymentFor(bids.get(0));
    }

    public void requestPaymentFor(Bid bid) {

        var auctionIDWrapped = new ByteWrapper(bid.getAuctionId());
        var key = this.paymentKeys.get(auctionIDWrapped);

        RequestPaymentMessage paymentMessage = new RequestPaymentMessage(bid.getAuctionId(),
                bid.getBidID(), Standards.calculateHashedPublicKeyFrom(key.getPublic()));

        paymentMessage.setSignedByAuctioneer(Signable.calculateSignatureOf(paymentMessage, key.getPrivate()));

        messageHandler.sendMessage(bid.getUserNodeId(), paymentMessage, (response) -> {

            var txID = response;

            var bestCurrentChain = chainHandler.getBestCurrentChain();

            if (bestCurrentChain.isPresent()) {

                var latestValidBlock = bestCurrentChain.get().getLatestValidBlock();

                var blockNumber = latestValidBlock.getHeader().getBlockNumber();

                this.pendingPayments.add(Pair.of(Pair.of(txID, blockNumber), bid.getAuctionId()));

            } else {
                throw new IllegalArgumentException("Cannot accept a bid with no block chain!");
            }

        }, () -> {
            //If the node is not available to respond, move to the next bid
            int index = this.currentBidIndexForAuc.computeIfPresent(auctionIDWrapped, (id, curVal) -> curVal + 1);

            requestPaymentFor(this.auctionWaitingForPayment.get(auctionIDWrapped).get(index));
        });
    }

    public void handlePaymentRequest(byte[] bid, byte[] auctionID, byte[] destination) {
    }

    public void handleNewBlock(Block block) {

        Set<ByteWrapper> outputsToSearchFor = new TreeSet<>();

        this.paymentKeys.forEach((auction, keyPair) ->
                outputsToSearchFor.add(new ByteWrapper(Standards.calculateHashedPublicKeyFrom(keyPair.getPublic()))));

        Map<ByteWrapper, byte[]> transactionsToCheck = new TreeMap<>();

        var itr = this.pendingPayments.iterator();

        while (itr.hasNext()) {
            Pair<Pair<byte[], Long>, byte[]> pendingPayment = itr.next();

            var pendingTransactions = pendingPayment.getKey();

            byte[] auctionID = pendingPayment.getValue();

            if (pendingTransactions.getValue() + AuctionStandards.PAYMENT_EXPIRATION_BLOCKS < block.getHeader().getBlockNumber()) {

                //TODO: move to the next one

                ByteWrapper wrappedAucID = new ByteWrapper(auctionID);

                Integer currInde = this.currentBidIndexForAuc.get(wrappedAucID);

                List<Bid> bids = this.auctionWaitingForPayment.get(wrappedAucID);

                if (currInde + 1 >= bids.size()) {
                    System.out.println("Auction " + Hex.toHexString(auctionID) + " has failed since no bidders payed.");
                } else {
                    this.currentBidIndexForAuc.put(wrappedAucID, currInde + 1);

                    requestPaymentFor(bids.get(currInde + 1));
                }

                itr.remove();

            } else {
                transactionsToCheck.put(new ByteWrapper(pendingTransactions.getKey()), pendingPayment.getValue());
            }
        }

        for (Transaction transaction : block.getTransactions()) {

            var wrappedTxID = new ByteWrapper(transaction.getTxID());
            if (transactionsToCheck.containsKey(wrappedTxID)) {

                var auctionID = transactionsToCheck.get(wrappedTxID);
                var wrappedAuctionID = new ByteWrapper(auctionID);

                for (ScriptPubKey output : transaction.getOutputs()) {
                    if (outputsToSearchFor.contains(new ByteWrapper(output.getHashedPubKey()))) {

                        var bids = this.auctionWaitingForPayment.get(wrappedAuctionID);

                        var ind = this.currentBidIndexForAuc.get(wrappedAuctionID);

                        KeyPair keyPair = this.paymentKeys.get(wrappedAuctionID);

                        var bid = bids.get(ind);

                        if (Float.compare(output.getAmount(), bid.getBidAmount(keyPair)) == 0) {
                            //TODO: this auction is completed

                            this.auctionWaitingForPayment.remove(wrappedAuctionID);
                            this.currentBidIndexForAuc.remove(wrappedAuctionID);
                            this.paymentKeys.remove(wrappedAuctionID);
                            break;
                        }
                    }
                }
            }
        }
    }

}
