package Auction.Server;

import Auction.model.Item;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


public class Server {

    public static final List<Item> items = new ArrayList<>();
    public static int nextItemId = 1;

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

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Client connected: " + client.getInetAddress());

            Thread t = new ClientHandler(client);
            t.start();
        }
    }
}