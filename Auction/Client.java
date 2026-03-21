package Auction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client extends Thread {

    public static void main(String[] args) throws IOException {

        try {
            Scanner scan = new Scanner(System.in);

            System.out.println("Client Started");

            Socket server = null;
                server = new Socket("AuctionServer", 8080);

            ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(server.getInputStream());

            //Write and send a message
            System.out.print("Write a msg: ");
            String msg = scan.nextLine();

            out.writeUTF(msg);
            out.flush();

            String result = in.readUTF();
            server.close();

            System.out.println("Result: " + result);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }




    }


    
}
