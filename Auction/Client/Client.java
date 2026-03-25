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
            printmenu();
            while (true){

                String msg = scan.nextLine();
                if(Objects.equals(msg, "exit") || Objects.equals(msg, "3") )
                    break;;

                out.writeUTF(msg);
                out.flush();

                String result = in.readUTF();
                System.out.println("Result: " + result);
                System.out.print("Choose action or write a msg: ");

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void printmenu(){

        System.out.println("=== MENU ===");
        System.out.println("1. Sell item");
        System.out.println("2. List item");
        System.out.println("3. Exit");
        System.out.print("Choose action or write a msg: ");




    }

}