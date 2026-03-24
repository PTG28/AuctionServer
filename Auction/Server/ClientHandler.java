package Auction.Server;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler extends Thread {

    private Socket connection;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket connection) {
        this.connection = connection;
        try {
            out = new ObjectOutputStream(connection.getOutputStream());
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
        return "Server received: " + msg;
    }
    private String handleMessages(String msg) {
        /*
        if (msg.startsWith("SELL_ITEM|")) {
            return handleSellItem(msg);
        } else if (msg.equals("LIST_ITEMS")) {
            return handleListItems();
        } else {
            return "Unknown command";
        }
        */
         return null;
    }
}