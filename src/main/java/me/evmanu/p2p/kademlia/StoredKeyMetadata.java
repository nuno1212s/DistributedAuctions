package me.evmanu.p2p.kademlia;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
public class StoredKeyMetadata {

    private final byte[] key;

    /**
     * The node that first stored this information
     */
    private final byte[] ownerNodeID;

    private byte[] value;

    private long lastRepublished;

    private long lastUpdated;

    public StoredKeyMetadata(byte[] key, byte[] value, byte[] ownerNodeID) {

        this.key = key;
        this.ownerNodeID = ownerNodeID;
        this.value = value;

        this.lastRepublished = System.currentTimeMillis();
        this.lastUpdated = System.currentTimeMillis();
    }

    public void setValue(byte[] value) {
        this.value = value;

        this.registerUpdate();
    }

    public void registerUpdate() {
        this.lastUpdated = System.currentTimeMillis();
    }

    public void registerRepublished() {
        this.lastRepublished = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoredKeyMetadata that = (StoredKeyMetadata) o;
        return Arrays.equals(getKey(), that.getKey());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getKey());
    }
}
