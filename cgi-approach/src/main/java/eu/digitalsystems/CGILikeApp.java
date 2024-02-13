package eu.digitalsystems;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Hello world!
 */
public class CGILikeApp {

    private final String secretPassword = "hemlighet";
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
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

    public void stop() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new RuntimeException("I expect exactly single integer parameter. It is TCP port number to start listening on.");
        }
        final int parsedPortArgument = Integer.parseInt(args[0]);
        final CGILikeApp server = new CGILikeApp();
        server.start(parsedPortArgument);
    }
}
