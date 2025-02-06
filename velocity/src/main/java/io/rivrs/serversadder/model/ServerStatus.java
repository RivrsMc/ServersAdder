package io.rivrs.serversadder.model;

public enum ServerStatus {
    ONLINE,
    PRE_SHUTDOWN,
    PRE_SHUTDOWN_ACK,
    SHUTDOWN,
    SHUTDOWN_ACK,
    OFFLINE,
    RESTARTED
}
