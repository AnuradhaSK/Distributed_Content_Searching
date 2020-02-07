package lk.ac.mrt.cse.solutia.node;

public class SearchResult {

    private String filename;
    private String hostIP;
    private String hostPort;
    private String hopsToReach;
    private long timeElapsed;

    public SearchResult(String filename, String hostIP, String hostPort, String hopsToReach, long timeElapsed) {
        this.filename = filename;
        this.hostIP = hostIP;
        this.hostPort = hostPort;
        this.hopsToReach = hopsToReach;
        this.timeElapsed = timeElapsed;
    }

    public String getFilename() {
        return filename;
    }

    public String getHostIP() {
        return hostIP;
    }

    public String getHostPort() {
        return hostPort;
    }

    public String getHopsToReach() {
        return hopsToReach;
    }

    public long getTimeElapsed() {
        return timeElapsed;
    }

}
