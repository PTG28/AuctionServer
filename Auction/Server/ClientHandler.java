package Auction.Server;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler extends Thread {

    private final Socket connection;
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
                System.out.println("Message from client: " + msg);

                String[] parts = msg.split(" ");

                switch(parts[0]) {

                    case "REGISTER":
                        handleRegister(parts);
                        break;

                    case "LOGIN":
                        handleLogin(parts);
                        break;

                    case "ADD_ITEM":
                        handleAddItem(parts);
                        break;

                    default:
                        out.writeUTF("Unknown command");
                        out.flush();
                }
            }

        }
        catch (EOFException | SocketException e) {
            System.out.println("Client disconnected.");
        } catch (Exception e) {
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

    // ================= METHODS =================

    private void handleRegister(String[] parts) throws Exception {
        String username = parts[1];
        String password = parts[2];

        if(ServerState.users.containsKey(username)) {
            out.writeUTF("Username exists");
        } else {
            ServerState.users.put(username, new User(username, password));
            out.writeUTF("Register successful");
        }
        out.flush();
    }

    private void handleLogin(String[] parts) throws Exception {
        String username = parts[1];
        String password = parts[2];

        User user = ServerState.users.get(username);

        if(user != null && user.password.equals(password)) {
            out.writeUTF("Login successful");
        } else {
            out.writeUTF("Login failed");
        }
        out.flush();
    }

    private void handleAddItem(String[] parts) throws Exception {

        String objectId = parts[1];
        String desc = parts[2];
        double startBid = Double.parseDouble(parts[3]);
        int duration = Integer.parseInt(parts[4]);
        String seller = parts[5];

        Item item = new Item(objectId, desc, startBid, duration, seller);

        ServerState.auctionQueue.add(item);

        out.writeUTF("Item added to auction queue");
        out.flush();
    }
}