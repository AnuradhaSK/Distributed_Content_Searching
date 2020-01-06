package lk.ac.mrt.cse.solutia.messages;

import lk.ac.mrt.cse.solutia.node.Node;

public class SearchQuery {
    private Node sourceNode;
    private Node destinationNode;
    private String query;
    private int hops;

    public SearchQuery(Node sourceNode, Node destinationNode, String query, int hops){
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.query = query;
        this.hops = hops;
    }

    public SearchQuery(Node sourceNode, Node destinationNode, String query){
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.query = query;
    }

    public Node getSourceNode() {
        return sourceNode;
    }

    public Node getDestinationNode() {
        return destinationNode;
    }

    public String getQuery() {
        return query;
    }

    public int getHops() {
        return hops;
    }

    public void setSourceNode(Node sourceNode) {
        this.sourceNode = sourceNode;
    }

    public void setDestinationNode(Node destinationNode) {
        this.destinationNode = destinationNode;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setHops(int hops) {
        this.hops = hops;
    }
}
