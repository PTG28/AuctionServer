package Auction.Client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class PeerConnector {

    public String requestMetadataFile(String host, int port, String metadataFileName, String buyerSharedDirectory) {
        try (
                Socket socket = new Socket(host, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            out.writeUTF("TRANSACTION_REQUEST|" + metadataFileName);
            out.flush();

            String response = in.readUTF();
            String[] parts = response.split("\\|", 3);

            if (parts.length >= 3 && "FILE_TRANSFER".equals(parts[0])) {
                String fileName = parts[1];
                String content = parts[2];

                Path outputPath = Path.of(buyerSharedDirectory, fileName);
                Files.writeString(outputPath, content);

                return "Transaction completed successfully. File stored at: " + outputPath;
            }

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "Unable to connect to seller peer: " + e.getMessage();
        }
    }

    public boolean checkActive(String host, int port) {
        try (
                Socket socket = new Socket(host, port);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            out.writeUTF("CHECK_ACTIVE");
            out.flush();

            String response = in.readUTF();
            return "ACTIVE".equals(response);
        } catch (Exception e) {
            return false;
        }
    }
}