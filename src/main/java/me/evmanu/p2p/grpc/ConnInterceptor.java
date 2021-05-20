package me.evmanu.p2p.grpc;

import io.grpc.*;
import me.evmanu.Ping;
import me.evmanu.Store;
import me.evmanu.TargetID;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.util.Hex;

import java.lang.annotation.Target;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Handle updating our k buckets when we receive a request from another kademlia node.
 */
public class ConnInterceptor implements ServerInterceptor {

    private final P2PNode node;

    public ConnInterceptor(P2PNode node) {
        this.node = node;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {

        final var remoteAddr = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);

        InetSocketAddress socketAddress = (InetSocketAddress) remoteAddr;

        System.out.println(socketAddress.getHostName() + ":" + socketAddress.getHostString() +
                ":" + socketAddress.getAddress().toString());

        int port = socketAddress.getPort();
        InetAddress address = socketAddress.getAddress();

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(next.startCall(call, headers)) {
            @Override
            public void onMessage(ReqT message) {

                byte[] nodeID;

                if (message instanceof Ping) {
                    nodeID = ((Ping) message).getNodeID().toByteArray();
                } else if (message instanceof Store) {
                    nodeID = ((Store) message).getRequestingNodeID().toByteArray();
                } else if (message instanceof TargetID) {
                    nodeID = ((TargetID) message).getRequestingNodeID().toByteArray();
                } else {
                    super.onMessage(message);

                    return;
                }

                node.handleSeenNode(new NodeTriple(address, port, nodeID, System.currentTimeMillis()));

                super.onMessage(message);
            }
        };
    }
}
