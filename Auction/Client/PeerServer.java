package Auction.Client;

import java.net.ServerSocket;
import java.net.Socket;

public class PeerServer extends Thread {

    private final int port;
    private final String sharedDirectory;

    public PeerServer(int port, String sharedDirectory) {
        this.port = port;
        this.sharedDirectory = sharedDirectory;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port, 10)) {
            System.out.println("PeerServer listening on port " + port);

            while (true) {
                Socket peerConnection = serverSocket.accept();
               // System.out.println("Peer connected: " + peerConnection.getInetAddress());

                Thread t = new PeerHandler(peerConnection, sharedDirectory);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}