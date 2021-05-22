import me.evmanu.p2p.grpc.DistLedgerServer;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

public class KademliaTests {

    @Test
    public void testNodeConnection() {

        DistLedgerServer kademliaNode1 = new DistLedgerServer(), kademliaNode2 = new DistLedgerServer();

        try {
            P2PNode node1 = kademliaNode1.start(null, 8080);

            P2PNode node2 = kademliaNode2.start(null, 8081);

            node1.boostrap(Arrays.asList(new NodeTriple(InetAddress.getLocalHost(),
                    8081, node2.getNodeID(), System.currentTimeMillis())));

            node1.waitForAllOperations();
            node2.waitForAllOperations();

            System.out.println("ALL NODES: " + node1.collectAllNodesInRoutingTable());

            System.out.println("ALL NODES 2: " + node2.collectAllNodesInRoutingTable());

            kademliaNode1.blockUntilShutdown();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

}
