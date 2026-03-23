package Auction.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static void main(String[] args) {
        try {
            new Server().openServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ServerSocket serverSocket;

    void openServer() throws IOException {
        serverSocket = new ServerSocket(8080, 10);
        System.out.println("Server Started at 8080");

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Client connected: " + client.getInetAddress());

            Thread t = new ClientHandler(client);
            t.start();
        }
    }
}