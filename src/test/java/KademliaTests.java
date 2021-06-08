import me.evmanu.p2p.grpc.DistLedgerServer;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;
import me.evmanu.p2p.kademlia.StoredKeyMetadata;
import me.evmanu.p2p.nodeoperations.*;
import me.evmanu.util.Hex;
import me.evmanu.util.Pair;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KademliaTests {

    @Ignore
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

    @Ignore
    public void testNodePropagation() {

        DistLedgerServer[] nodes = new DistLedgerServer[10];

        P2PNode[] actualNodes = new P2PNode[nodes.length];

        Thread[] threads = new Thread[nodes.length];

        InetAddress localHost;

        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        //Because the number of nodes is less that K, node9 should know about all other nodes.
        int firstPort = 8080;

        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new DistLedgerServer();

            try {
                actualNodes[i] = nodes[i].start(null, firstPort++);

                int finalI = i;
                threads[i] = new Thread(() -> {
                    try {
                        nodes[finalI].blockUntilShutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

                threads[i].start();

                if (i > 0) {
                    actualNodes[i].boostrap(Collections.singletonList(
                            new NodeTriple(localHost, actualNodes[0].getNodePublicPort(),
                                    actualNodes[0].getNodeID(), System.currentTimeMillis())
                    ));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (P2PNode actualNode : actualNodes) {
            actualNode.waitForAllOperations();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (P2PNode actualNode : actualNodes) {
            List<Pair<NodeTriple, Integer>> nodeTriples = actualNode.collectAllNodesInRoutingTable();
            System.out.println("Node table " + Hex.toHexString(actualNode.getNodeID()) + "( " + nodeTriples.size() + ") : " + nodeTriples);
        }

        for (P2PNode actualNode : actualNodes) {
            new NodeLookupOperation(actualNode, actualNode.getNodeID(), (_done) -> {}).execute();

            actualNode.waitForAllOperations();
        }

        for (P2PNode actualNode : actualNodes) {
            List<Pair<NodeTriple, Integer>> nodeTriples = actualNode.collectAllNodesInRoutingTable();
            System.out.println("Node table " + Hex.toHexString(actualNode.getNodeID()) + "( " + nodeTriples.size() + ") : " + nodeTriples);
        }

        try {
            nodes[0].blockUntilShutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Ignore
    public void testBroadcast() {

        DistLedgerServer[] nodes = new DistLedgerServer[10];

        P2PNode[] actualNodes = new P2PNode[nodes.length];

        Thread[] threads = new Thread[nodes.length];

        InetAddress localHost;

        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        //Because the number of nodes is less that K, node9 should know about all other nodes.
        int firstPort = 8080;

        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new DistLedgerServer();

            try {
                actualNodes[i] = nodes[i].start(null, firstPort++);

                int finalI = i;
                threads[i] = new Thread(() -> {
                    try {
                        nodes[finalI].blockUntilShutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

                threads[i].start();

                if (i > 0) {
                    actualNodes[i].boostrap(Collections.singletonList(
                            new NodeTriple(localHost, actualNodes[0].getNodePublicPort(),
                                    actualNodes[0].getNodeID(), System.currentTimeMillis())
                    ));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (P2PNode actualNode : actualNodes) {
            actualNode.waitForAllOperations();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (P2PNode actualNode : actualNodes) {
            List<Pair<NodeTriple, Integer>> nodeTriples = actualNode.collectAllNodesInRoutingTable();
            System.out.println("Node table " + Hex.toHexString(actualNode.getNodeID()) + "( " + nodeTriples.size() + ") : " + nodeTriples);
        }

        for (P2PNode actualNode : actualNodes) {
            new NodeLookupOperation(actualNode, actualNode.getNodeID(), (_done) -> {}).execute();

            actualNode.waitForAllOperations();
        }

        for (P2PNode actualNode : actualNodes) {
            List<Pair<NodeTriple, Integer>> nodeTriples = actualNode.collectAllNodesInRoutingTable();
            System.out.println("Node table " + Hex.toHexString(actualNode.getNodeID()) + "( " + nodeTriples.size() + ") : " + nodeTriples);
        }

        String message = "Ola a todos";

        System.out.println("Message content: " + Hex.toHexString(message.getBytes()));

        new BroadcastMessageOperation(actualNodes[0], 0, actualNodes[0].getNodeID(), message.getBytes()).execute();

        try {
            nodes[0].blockUntilShutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Ignore
    public void testSendMessage() {

        DistLedgerServer[] nodes = new DistLedgerServer[10];

        P2PNode[] actualNodes = new P2PNode[nodes.length];

        Thread[] threads = new Thread[nodes.length];

        InetAddress localHost;

        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        //Because the number of nodes is less that K, node9 should know about all other nodes.
        int firstPort = 8080;

        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new DistLedgerServer();

            try {
                actualNodes[i] = nodes[i].start(null, firstPort++);

                int finalI = i;
                threads[i] = new Thread(() -> {
                    try {
                        nodes[finalI].blockUntilShutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

                threads[i].start();

                if (i > 0) {
                    actualNodes[i].boostrap(Collections.singletonList(
                            new NodeTriple(localHost, actualNodes[0].getNodePublicPort(),
                                    actualNodes[0].getNodeID(), System.currentTimeMillis())
                    ));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (P2PNode actualNode : actualNodes) {
            actualNode.waitForAllOperations();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (P2PNode actualNode : actualNodes) {
            List<Pair<NodeTriple, Integer>> nodeTriples = actualNode.collectAllNodesInRoutingTable();
            System.out.println("Node table " + Hex.toHexString(actualNode.getNodeID()) + "( " + nodeTriples.size() + ") : " + nodeTriples);
        }

        for (P2PNode actualNode : actualNodes) {
            new NodeLookupOperation(actualNode, actualNode.getNodeID(), (_done) -> {}).execute();

            actualNode.waitForAllOperations();
        }

        for (P2PNode actualNode : actualNodes) {
            List<Pair<NodeTriple, Integer>> nodeTriples = actualNode.collectAllNodesInRoutingTable();
            System.out.println("Node table " + Hex.toHexString(actualNode.getNodeID()) + "( " + nodeTriples.size() + ") : " + nodeTriples);
        }

        String message = "Ola a todos";

        System.out.println("Message content: " + Hex.toHexString(message.getBytes()));

        System.out.println("Sending message from: " + Hex.toHexString(actualNodes[0].getNodeID()) + " to node " +
                Hex.toHexString(actualNodes[1].getNodeID()));

        new SendMessageOperation(actualNodes[0],  actualNodes[1].getNodeID(), message.getBytes()).execute();

        try {
            nodes[0].blockUntilShutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Ignore
    public void testStoreProcedure() {

        DistLedgerServer[] nodes = new DistLedgerServer[10];

        P2PNode[] actualNodes = new P2PNode[nodes.length];

        Thread[] threads = new Thread[nodes.length];

        InetAddress localHost;

        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        //Because the number of nodes is less that K, node9 should know about all other nodes.
        int firstPort = 8080;

        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new DistLedgerServer();

            try {
                actualNodes[i] = nodes[i].start(null, firstPort++);

                int finalI = i;
                threads[i] = new Thread(() -> {
                    try {
                        nodes[finalI].blockUntilShutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });

                threads[i].start();

                if (i > 0) {
                    actualNodes[i].boostrap(Collections.singletonList(
                            new NodeTriple(localHost, actualNodes[0].getNodePublicPort(),
                                    actualNodes[0].getNodeID(), System.currentTimeMillis())
                    ));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (P2PNode actualNode : actualNodes) {
            actualNode.waitForAllOperations();
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (P2PNode actualNode : actualNodes) {
            List<Pair<NodeTriple, Integer>> nodeTriples = actualNode.collectAllNodesInRoutingTable();
            System.out.println("Node table " + Hex.toHexString(actualNode.getNodeID()) + "( " + nodeTriples.size() + ") : " + nodeTriples);
        }

        for (P2PNode actualNode : actualNodes) {
            new NodeLookupOperation(actualNode, actualNode.getNodeID(), (_done) -> {}).execute();

            actualNode.waitForAllOperations();
        }

        for (P2PNode actualNode : actualNodes) {
            List<Pair<NodeTriple, Integer>> nodeTriples = actualNode.collectAllNodesInRoutingTable();
            System.out.println("Node table " + Hex.toHexString(actualNode.getNodeID()) + "( " + nodeTriples.size() + ") : " + nodeTriples);
        }

        String message = "Ola a todos";

        System.out.println("Message content: " + Hex.toHexString(message.getBytes()));

        new StoreOperation(actualNodes[0], new StoredKeyMetadata(message.getBytes(), message.getBytes(),
                actualNodes[0].getNodeID())).execute();

        actualNodes[0].waitForAllOperations();

        try {
            Thread.sleep(1000);

            new ContentLookupOperation(actualNodes[1], message.getBytes(), (_result) -> {
                System.out.println("Found result " + Hex.toHexString(_result));
            }).execute();

            actualNodes[1].waitForAllOperations();

            nodes[0].blockUntilShutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
