package com.todoroo.astrid.actfm.sync;

public class ActFmSyncMonitor {
    private ActFmSyncMonitor() {/**/}
    private static final ActFmSyncMonitor INSTANCE = new ActFmSyncMonitor();

    public static ActFmSyncMonitor getInstance() {
        return INSTANCE;
    }
}