import me.evmanu.p2p.grpc.DistLedgerServer;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import org.junit.Test;

import java.io.IOException;

public class KademliaTests {

    @Test
    public void testNodeConnection() {

        DistLedgerServer kademliaNode1 = new DistLedgerServer(), kademliaNode2 = new DistLedgerServer();

        try {
            P2PNode node1 = kademliaNode1.start(null, 8080);

            P2PNode node2 = kademliaNode2.start(null, 8081);

//            node1.boostrap(new NodeTriple(node2.getNodeID()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
