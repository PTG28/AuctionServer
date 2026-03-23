package Auction.Client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) {
        System.out.println("Client Started");

        try (
                Socket server = new Socket("localhost", 8080);
                ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(server.getInputStream());
                Scanner scan = new Scanner(System.in)
        ) {
            while (true){

                System.out.print("Write a msg: ");
                String msg = scan.nextLine();
                if(Objects.equals(msg, "exit"))
                    break;;

                out.writeUTF(msg);
                out.flush();

                String result = in.readUTF();
                System.out.println("Result: " + result);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}