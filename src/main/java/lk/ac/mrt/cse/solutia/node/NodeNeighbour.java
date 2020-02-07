package lk.ac.mrt.cse.solutia.node;

public class NodeNeighbour {
    private String ip;
    private int port;
    private String username;

    public NodeNeighbour(String ip, int port, String username) {
        this.ip = ip;
        this.port = port;
        this.username = username;
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public String getUsername() {
        return this.username;
    }
}
