package me.evmanu.p2p;

import io.grpc.stub.StreamObserver;
import lombok.AccessLevel;
import lombok.Getter;
import me.evmanu.*;

import java.util.logging.Logger;

public class DistLedgerServerImpl extends P2PServerGrpc.P2PServerImplBase {

    @Getter(value = AccessLevel.PRIVATE)
    private final Logger logger;

    public DistLedgerServerImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void ping(Ping request, StreamObserver<Ping> responseObserver) {
        responseObserver.onNext(request);

        responseObserver.onCompleted();
    }

    @Override
    public void store(Store request, StreamObserver<Store> responseObserver) {
        super.store(request, responseObserver);
    }

    @Override
    public void findNode(TargetID request, StreamObserver<FoundNode> responseObserver) {
        super.findNode(request, responseObserver);
    }

    @Override
    public void findValue(TargetID request, StreamObserver<Store> responseObserver) {
        super.findValue(request, responseObserver);
    }
}
