package Auction.Client;

import java.io.EOFException;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PeerHandler extends Thread {

    private final Socket connection;
    private final String sharedDirectory;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public PeerHandler(Socket connection, String sharedDirectory) {
        this.connection = connection;
        this.sharedDirectory = sharedDirectory;

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

                if ("EXIT".equals(msg)) {
                    break;
                }
            }
        } catch (EOFException | SocketException e) {
            //System.out.println("Peer disconnected.");
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
        if (msg == null || msg.trim().isEmpty()) {
            return "Invalid peer request";
        }

        String[] parts = msg.split("\\|");
        String command = parts[0];

        switch (command) {
            case "TRANSACTION_REQUEST":
                if (parts.length < 2 || parts[1].trim().isEmpty()) {
                    return "Missing metadata filename";
                }

                System.out.println("Transaction request received for file: " + parts[1]);
                return sendMetadataFile(parts[1]);

            case "CHECK_ACTIVE":
                return "ACTIVE";

            case "EXIT":
                return "Peer connection closed";

            default:
                return "Invalid peer command";
        }
    }

    private String sendMetadataFile(String metadataFileName) {
        try {
            File file = new File(sharedDirectory, metadataFileName);

            if (!file.exists()) {
                return "FILE_NOT_FOUND";
            }

            String content = Files.readString(Path.of(file.getAbsolutePath()));

            boolean deleted = file.delete();
            if (!deleted) {
                return "FILE_DELETE_FAILED";
            }

            System.out.println("Transferred and deleted file: " + metadataFileName);
            System.out.print("Choose action: ");

            return "FILE_TRANSFER|" + metadataFileName + "|" + content;
        } catch (Exception e) {
            e.printStackTrace();
            return "TRANSFER_ERROR|" + e.getMessage();
        }
    }}