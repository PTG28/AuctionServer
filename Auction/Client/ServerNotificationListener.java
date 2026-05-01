package Auction.Client;

import java.io.ObjectInputStream;

public class ServerNotificationListener extends Thread {

    private final ObjectInputStream in;

    public ServerNotificationListener(ObjectInputStream in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            while (true) {
                String msg = in.readUTF();

                if (msg.startsWith("NOTIFICATION|")) {
                    String notification = msg.substring("NOTIFICATION|".length());
                    System.out.println("\n[SERVER NOTIFICATION] " + notification);
                } else {
                    synchronized (ClientResponseBuffer.LOCK) {
                        ClientResponseBuffer.lastResponse = msg;
                        ClientResponseBuffer.LOCK.notifyAll();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Notification listener stopped.");
        }
    }
}