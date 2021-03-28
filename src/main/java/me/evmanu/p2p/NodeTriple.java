package me.evmanu.p2p;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.net.Inet4Address;

@Getter
@AllArgsConstructor
public class NodeTriple {

    private final Inet4Address ipAddress;

    private final int udpPort;

    private final byte[] nodeID;

    @Setter
    private long lastSeen;

}