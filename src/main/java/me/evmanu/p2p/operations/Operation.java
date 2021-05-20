package me.evmanu.p2p.operations;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public interface Operation {

    /**
     * An executor that runs the lookup operations on a timer to prevent problems when all first alpha nodes
     */
    static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    public void execute();

}