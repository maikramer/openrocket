package net.sf.openrocket.file;

/**
 * Interface definition for services.
 */
public interface WatcherService {
    /**
     * Starts the service. This method blocks until the service has completely started.
     */
    void start() throws Exception;

    /**
     * Stops the service. This method blocks until the service has completely shut down.
     */
    void stop();
}