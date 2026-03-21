package Auction;
import java.util.Scanner;
import java.lang.Thread;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {
    public static void main(String[] args) throws IOException {
        try {
            new Server().openServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

        ServerSocket serverSocket;
        Socket client = null;

        void openServer() throws IOException {
            try {
                serverSocket = new ServerSocket(8080, 10);
                System.out.println("Server Started at 8080");

                while(true){
                    client = serverSocket.accept();
                    Thread t = new ActionForClient(client);
                    t.start();
                }


                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
        }


}
