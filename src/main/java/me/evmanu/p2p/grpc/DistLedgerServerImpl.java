package me.evmanu.p2p.grpc;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.Getter;
import me.evmanu.*;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;

import java.util.List;
import java.util.logging.Logger;

public class DistLedgerServerImpl extends P2PServerGrpc.P2PServerImplBase {

    @Getter(value = AccessLevel.PRIVATE)
    private final Logger logger;

    private final P2PNode node;

    public DistLedgerServerImpl(Logger logger, P2PNode p2PNode) {
        this.logger = logger;
        this.node = p2PNode;
    }

    //TODO: Every method in this class should invoke a handleSeenNode in our node
    @Override
    public void ping(Ping request, StreamObserver<Ping> responseObserver) {
        responseObserver.onNext(request);

        responseObserver.onCompleted();
    }

    @Override
    public void store(Store request, StreamObserver<Store> responseObserver) {
        node.storeValue(request.getKey().toByteArray(), request.getValue().toByteArray());

        responseObserver.onNext(Store.newBuilder().setValue(request.getValue()).build());

        responseObserver.onCompleted();
    }

    @Override
    public void findNode(TargetID request, StreamObserver<FoundNode> responseObserver) {

        final var kClosestNodes = this.node.findNode(request.getTargetID().toByteArray());

        for (NodeTriple kClosestNode : kClosestNodes) {
            final var node = FoundNode.newBuilder().setNodeID(ByteString.copyFrom(kClosestNode.getNodeID()))
                    .build();

            responseObserver.onNext(node);
        }

        responseObserver.onCompleted();
    }

    @Override
    public void findValue(TargetID request, StreamObserver<Store> responseObserver) {
        final var result = this.node.loadValue(request.getTargetID().toByteArray());

        responseObserver.onNext(Store.newBuilder().setValue(ByteString.copyFrom(result)).build());

        responseObserver.onCompleted();
    }
}
