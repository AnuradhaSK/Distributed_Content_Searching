package lk.ac.mrt.cse.solutia.node;

public class NodeNeighbour {
    private String ip;
    private int port;

    public NodeNeighbour(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }
}
