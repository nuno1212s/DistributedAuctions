package me.evmanu.p2p.nodeoperations;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public interface Operation {

    /**
     * An executor that runs the lookup operations on a timer to prevent problems when all first alpha nodes
     */
    static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public void execute();

    public boolean hasFinished();

}
