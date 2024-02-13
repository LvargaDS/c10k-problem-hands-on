package eu.digitalsystems;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class SinglePortListener implements Runnable {
    private final int myPort;
    private final AtomicInteger successCount;
    private final AtomicInteger startAttemptsCount;
    private final String secretPassword = "hemlighet";
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;


    public SinglePortListener(int myPort, AtomicInteger successCount, AtomicInteger startAttemptsCount) {
        this.myPort = myPort;
        this.successCount = successCount;
        this.startAttemptsCount = startAttemptsCount;
    }

    public void startListening(int port) throws IOException {
        startAttemptsCount.incrementAndGet();
        serverSocket = new ServerSocket(port);
        successCount.incrementAndGet();
        clientSocket = serverSocket.accept();
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        out.println("try to guess some secret. It is single word and if you hit it, I will congratulate you.");
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.equals(secretPassword)) {
                out.println("You hit it. My secret was " + secretPassword + ". Congratulation. All your base belong to us.");
            } else {
                out.println("no");
            }
        }
    }

    @Override
    public void run() {
        try {
            this.startListening(this.myPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
