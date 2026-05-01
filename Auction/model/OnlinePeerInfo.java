package Auction.model;

public class OnlinePeerInfo {

    private final String tokenId;
    private final String ipAddress;
    private final int port;
    private final String username;

    public OnlinePeerInfo(String tokenId, String ipAddress, int port, String username) {
        this.tokenId = tokenId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
    }

    public String getTokenId() {
        return tokenId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "OnlinePeerInfo{" +
                "tokenId='" + tokenId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                '}';
    }
}