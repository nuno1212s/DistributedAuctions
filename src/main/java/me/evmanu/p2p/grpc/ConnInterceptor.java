package me.evmanu.p2p.grpc;

import io.grpc.*;
import me.evmanu.CRCRequest;
import me.evmanu.Ping;
import me.evmanu.Store;
import me.evmanu.TargetID;
import me.evmanu.p2p.kademlia.CRChallenge;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.util.Hex;

import java.lang.annotation.Target;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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

        InetAddress address = null;

        try {
            address = InetAddress.getByName(socketAddress.getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();

            return next.startCall(call, headers);
        }

        InetAddress finalAddress = address;

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(next.startCall(call, headers)) {
            @Override
            public void onMessage(ReqT message) {

                byte[] nodeID;
                int port;

                if (message instanceof Ping) {
                    nodeID = ((Ping) message).getNodeID().toByteArray();
                    port = ((Ping) message).getRequestingNodePort();
                } else if (message instanceof Store) {
                    nodeID = ((Store) message).getRequestingNodeID().toByteArray();
                    port = ((Store) message).getRequestingNodePort();
                } else if (message instanceof TargetID) {
                    nodeID = ((TargetID) message).getRequestingNodeID().toByteArray();
                    port = ((TargetID) message).getRequestNodePort();
                } else if (message instanceof CRCRequest) {
                    nodeID = ((CRCRequest) message).getChallengingNodeID().toByteArray();
                    port = ((CRCRequest) message).getChallengingNodePort();
                } else {
                    super.onMessage(message);

                    return;
                }

                NodeTriple seen = new NodeTriple(finalAddress, port, nodeID, System.currentTimeMillis());

                System.out.println("Received a message of the type " + message.getClass() + " from the node " +
                        seen);

                //Fork the context as handling seen nodes will probably call RPCs that originate from this
                //Server (This is required by gRPC for programs that use a mix of client and server on the same
                //JVM)
                Context fork = Context.current().fork();

                fork.run(() -> node.handleSeenNode(seen));

                super.onMessage(message);
            }
        };
    }
}
