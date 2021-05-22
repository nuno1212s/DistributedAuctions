package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.CRChallenge;
import me.evmanu.p2p.kademlia.NodeTriple;
import me.evmanu.p2p.kademlia.P2PNode;

public class RequestCRCOperation implements Operation {

    private final P2PNode center;

    private final NodeTriple destination;

    private final long requestedChallenge;

    private boolean finished = false;

    public RequestCRCOperation(P2PNode center, NodeTriple destination) {
        this.center = center;
        this.destination = destination;

        this.requestedChallenge = CRChallenge.generateRandomChallenge();
    }

    @Override
    public void execute() {

        this.center.registerOngoingOperation(this);

        this.center.storeCRCRequest(destination, this.requestedChallenge);

        this.center.getClientManager().requestCRCFromNode(this.center, this, destination, this.requestedChallenge);
    }

    public void handleRequestFailed() {
        this.center.handleFailedNodePing(this.destination);

        setFinished(true);
    }

    public void handleRequestResponse(long challenge, long response) {

        System.out.println("Received CRC response from node " + this.destination);

        if (challenge != this.requestedChallenge) {
            //The challenge returned does not equal the challenge done
            System.out.println("Challenge is not the challenge that was sent " + challenge + " vs " + this.requestedChallenge);
        } else {
            if (CRChallenge.verifyCRChallenge(challenge, response)) {

                this.center.storeCRCResponse(this.destination, challenge, response);

                System.out.println("CRC challenge verified, node authenticated.");

                //The challenge is correct so we can finally add this node to our routing table
                //The response gets stored so if we see this same node later, we already know that it is a
                //Valid node.
                this.center.handleSeenNode(destination);
            } else {
                //The CR challenge is not correct

                System.out.println("Challenge is not correct for node " + destination);
            }
        }

        setFinished(true);
    }

    public void setFinished(boolean finished) {
        this.finished = finished;

        if (finished) {
            this.center.registerOperationDone(this);
        }
    }

    @Override
    public boolean hasFinished() {
        return this.finished;
    }
}
