package Auction.Server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

public class ClientHandler extends Thread {

    Scanner scan = new Scanner(System.in);
    private Socket connection;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket connection) {
        this.connection = connection;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
            //out.writeUTF("Welcome to Amesi Dimoprasia");
            out.flush();
            in = new ObjectInputStream(connection.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true){

            String msg = in.readUTF();
            String response = handleMessage(msg);

            out.writeUTF(response);
            out.flush();
            }

        }
        catch (EOFException | SocketException e) {
            System.out.println("Client disconnected.");
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (connection != null && !connection.isClosed()) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String handleMessage(String msg){
        System.out.println("Message from client: " + msg);
        switch (msg){
            case "1":
                System.out.print("Please provide the name of the file which includes the item(s) for sale: ");
                String file = scan.nextLine();
                sellItem(file);
                break;

            case "2":
                listItem();
                break;

            case "3":
                System.out.println("Thank you for your attendance! Hope to see you back soon");
                break;
            default:
                return "Server received: " + msg;
        }
        return "Server received: " + msg;
    }
    private String sellItem(String file) {
        File itemfile = new File(file);

        return null;
    }

    private String listItem(){
        return null;
    }
}