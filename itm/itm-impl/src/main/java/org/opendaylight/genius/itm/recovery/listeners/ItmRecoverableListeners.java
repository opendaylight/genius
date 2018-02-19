package org.opendaylight.genius.itm.recovery.listeners;

public interface ItmRecoverableListeners {
    /**
     * register a recoverable listener.
     */
    abstract void registerListener();

    /**
     * Deregister a recoverable listener.
     */
    void deregisterListener();
}
