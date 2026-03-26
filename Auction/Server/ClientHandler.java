package Auction.Server;

import java.io.File;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

import Auction.model.Item;


public class ClientHandler extends Thread {

    //Scanner scan = new Scanner(System.in);
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
            while (true) {
                String msg = in.readUTF();
                String response = handleMessage(msg);

                out.writeUTF(response);
                out.flush();

                if (msg.equals("3"))
                    break;
            }
        } catch (EOFException | SocketException e) {
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

    private String handleMessage(String msg) {
        System.out.println("Message from client: " + msg);
        String[] parts = msg.split("\\|", 2);
        String command = parts[0];
        switch (command) {
            case "1":
                if (parts.length < 2 || parts[1].isEmpty())
                    return ("Missing file name");
                return sellItem(parts[1].trim());
            case "2":
                return listItems();

            case "3":
                return ("Thank you for your attendance! Hope to see you back soon");

            default:
                return "Unknown command";

        }
    }

    private String sellItem (String filename){
        //int id;
        String seller = null;
        String name = null;
        String description = null;
        double startPrice = 0;
        int auction_duration = 0;

        try(Scanner fileScanner = new Scanner(new File(filename))){
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(":", 2); // split only into 2 parts
                if (parts.length < 2) continue;

                String key = parts[0].trim().toLowerCase();
                String value = parts[1].trim();

                switch (key) {
                    case "seller":
                        seller = value;
                        break;
                    case "item name":
                        name = value;
                        break;
                    case "description":
                        description = value;
                        break;
                    case "starting price":
                        startPrice = Double.parseDouble(value);
                        break;
                    case "auction time":
                        auction_duration = Integer.parseInt(value);
                        break;
                }
            }

            synchronized (Server.items){
                int id = Server.nextItemId++;
                Item item = new Item(id, seller, name, description, startPrice, auction_duration);
                Server.items.add(item);
                return "Item added successfully";
            }
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    private String listItems() {
        synchronized (Server.items){
            if(Server.items.isEmpty())
                return "No items available";

            int counter = 0;
            StringBuilder sb = new StringBuilder();
            for(Item item : Server.items){
                sb.append(counter).append(". ").append(item.getName()).append("\n");
                counter++;
            }
            return sb.toString();
        }
    }

    private String listItemDetails(int index){
        synchronized (Server.items){

            if (Server.items.isEmpty()) return "No available items";

            if(index < 0 || index >= Server.items.size()) return "Invalid item number";

            Item item = Server.items.get(index);
            return item.toString();
        }
    }

        // ================= METHODS =================

        private void handleRegister (String[]parts) throws Exception {
            String username = parts[1];
            String password = parts[2];

            if (ServerState.users.containsKey(username)) {
                out.writeUTF("Username exists");
            } else {
                ServerState.users.put(username, new User(username, password));
                out.writeUTF("Register successful");
            }
            out.flush();
        }

        private void handleLogin (String[]parts) throws Exception {
            String username = parts[1];
            String password = parts[2];

            User user = ServerState.users.get(username);

            if (user != null && user.password.equals(password)) {
                out.writeUTF("Login successful");
            } else {
                out.writeUTF("Login failed");
            }
            out.flush();
        }

        private void handleAddItem (String[]parts) throws Exception {

            String objectId = parts[1];
            String desc = parts[2];
            double startBid = Double.parseDouble(parts[3]);
            int duration = Integer.parseInt(parts[4]);
            String seller = parts[5];

            Item item = new Item();

            Server.items.add(item);

            out.writeUTF("Item added to auction queue");
            out.flush();
        }
    }
