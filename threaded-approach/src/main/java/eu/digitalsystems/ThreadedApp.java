package eu.digitalsystems;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadedApp {
    private static final int LISTENERS_COUNT = 20_000;
    private final AtomicInteger successCount = new AtomicInteger();
    private final AtomicInteger startAttemptsCount = new AtomicInteger();


    public static void main(String[] args) throws IOException {
        final ThreadedApp app = new ThreadedApp();

        Thread startingThread = new Thread(() -> {
            System.out.println("going to try to start " + LISTENERS_COUNT + " tcp listeners. Each with its own thread.");
            for (int i = 0; i < LISTENERS_COUNT; i++) {
                final Thread t = app.getThreadForPort(i);
                // do not block shutdown.
                t.setDaemon(true);
                t.start();
            }
        });
        startingThread.start();

        long waitTimeMs = 0;
        while (app.startAttemptsCount.get() != LISTENERS_COUNT && waitTimeMs < 30 * 1000) {
            System.out.println("Still working. Started " + app.successCount.get() + " listeners. " + app.startAttemptsCount.get() + " attempts till now.");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            waitTimeMs += 50;
        }
        System.out.println("everything started. Press enter key to finish main method and exit app.");
        System.out.println("There were " + app.startAttemptsCount.get() + " start attempts (should be equal to " + LISTENERS_COUNT + "). " + app.successCount.get() + " successful listening starts.");
        System.in.read();
    }

    private Thread getThreadForPort(int i) {
        final int port = 10000 + i;
        return new Thread(new SinglePortListener(port, successCount, startAttemptsCount));
    }
}
