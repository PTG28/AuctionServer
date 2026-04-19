package Auction.Server;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        try {
            new Server().openServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void openServer() throws Exception {
        ServerSocket serverSocket = new ServerSocket(8080, 10);
        System.out.println("Server Started at 8080");

        AuctionManager auctionManager = new AuctionManager();
        auctionManager.setDaemon(true);
        auctionManager.start();

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Client connected: " + client.getInetAddress());

            Thread t = new ClientHandler(client);
            t.start();
        }
    }
}
