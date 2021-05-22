package me.evmanu.p2p.grpc;

import com.google.protobuf.ByteString;
import io.grpc.ServerCall;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.Getter;
import me.evmanu.*;
import me.evmanu.p2p.kademlia.CRChallenge;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.util.Hex;

import java.util.List;
import java.util.logging.Logger;

/**
 * This class handles requests for the node
 * There is no need to handle seen nodes (and updating our k buckets as a result)
 * as that is handled in the interceptor that uses a protocol layer that is lower
 * Than the one this class operates in.
 * <p>
 * See {@link ConnInterceptor}
 */
public class DistLedgerServerImpl extends P2PServerGrpc.P2PServerImplBase {

    @Getter(value = AccessLevel.PRIVATE)
    private final Logger logger;

    private final P2PNode node;

    public DistLedgerServerImpl(Logger logger, P2PNode p2PNode) {
        this.logger = logger;
        this.node = p2PNode;
    }

    @Override
    public void ping(Ping request, StreamObserver<Ping> responseObserver) {
        responseObserver.onNext(request);

        responseObserver.onCompleted();
    }

    @Override
    public void store(Store request, StreamObserver<Store> responseObserver) {
        node.storeValue(request.getOwningNodeID().toByteArray(), request.getKey().toByteArray(),
                request.getValue().toByteArray());

        responseObserver.onNext(Store.newBuilder().setValue(request.getValue()).build());

        responseObserver.onCompleted();
    }

    @Override
    public void findNode(TargetID request, StreamObserver<FoundNode> responseObserver) {

        final var kClosestNodes = this.node.findKClosestNodes(request.getTargetID().toByteArray());

        for (NodeTriple kClosestNode : kClosestNodes) {
            final var node = FoundNode.newBuilder().setNodeID(ByteString.copyFrom(kClosestNode.getNodeID()))
                    .build();

            responseObserver.onNext(node);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void findValue(TargetID request, StreamObserver<FoundValue> responseObserver) {

        byte[] targetID = request.getTargetID().toByteArray();

        final var result = this.node.loadValue(targetID);

        //If the node did not receive a store request for this value, then
        //Return the K closest nodes.
        if (result == null) {
            List<NodeTriple> closestNodes = this.node.findKClosestNodes(targetID);

            for (NodeTriple closestNode : closestNodes) {

                FoundValue foundValue = FoundValue.newBuilder()
                        .setValueKind(StoreKind.NODES)
                        .setKey(ByteString.copyFrom(closestNode.getNodeID())).build();

                responseObserver.onNext(foundValue);
            }
        } else {
            FoundValue foundValue = FoundValue.newBuilder().setValueKind(StoreKind.VALUE_FOUND)
                    .setKey(request.getTargetID())
                    .setValue(ByteString.copyFrom(result))
                    .build();

            responseObserver.onNext(foundValue);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void requestCRC(CRCRequest request, StreamObserver<CRCResponse> responseObserver) {

        long challenge = request.getChallenge();

        long result = CRChallenge.solveCRChallenge(challenge);

        responseObserver.onNext(CRCResponse.newBuilder().setChallenge(challenge)
                .setResponse(result)
                .setChallengedNodeID(ByteString.copyFrom(node.getNodeID()))
                .build());

        responseObserver.onCompleted();
    }
}
