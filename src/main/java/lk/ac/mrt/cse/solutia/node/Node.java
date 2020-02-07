package lk.ac.mrt.cse.solutia.node;

import lk.ac.mrt.cse.solutia.bootsrtap_server.Neighbour;
import lk.ac.mrt.cse.solutia.utils.Config;

import java.util.*;

import lk.ac.mrt.cse.solutia.model.File;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;
import java.io.*;
import java.net.*;

import static java.lang.String.format;

public class Node implements Runnable {

    private String ip;
    private int port;
    private String username;
    private DatagramSocket socket;
    private ArrayList<NodeNeighbour> neighboursList = new ArrayList<NodeNeighbour>();
    //private String[] files; //files that owned by the node
    private ArrayList<String> files = new ArrayList<String>() {{
        add("Dinika");
        add("Anu");
        add("Shali");
    }};
    private HashMap<String, ArrayList<SearchResult>> resultsOfQueriesInitiatedByThisNode = new HashMap<>(); //FileName->resultID-><"node:port:file1:file1:file3">
    private HashMap<String, String> queryList = new HashMap<>(); //<QueryID,who sent it to this node>
    private ArrayList<String> queriesInitiatedByThisNode = new ArrayList<String>();

    private String serverHostName = Config.BOOTSTRAP_IP; //Bootstrap server ip
    private int serverHostPort = Config.BOOTSTRAP_PORT; //Bootstrap server port

    public void initiateNode() {

        File file = new lk.ac.mrt.cse.solutia.model.File();
        file.fileGenerate();
        String userInput;
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter node IP address : ");
        userInput = scanner.next();
        //TODO:  validate user input
        this.ip = userInput;

        System.out.println("Enter node port : ");
        userInput = scanner.next();
        //TODO:  validate user input
        this.port = Integer.parseInt(userInput);

        System.out.println("Enter node username : ");
        userInput = scanner.next();
        //TODO:  validate user input
        this.username = userInput;

        try {
            Runtime.getRuntime().exec("java -jar filetransfer-0.0.1-SNAPSHOT.jar");
            InetAddress address = InetAddress.getByName(serverHostName);
            DatagramSocket socket = new DatagramSocket(port);
            this.socket = socket;
            String message = "REG " + ip + " " + port + " " + username;
            int msgLength = message.length() + 5;
            message = format("%04d", msgLength) + " " + message;
            System.out.println("Request sent: " + message);

            DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, serverHostPort);
            socket.send(request);

            byte[] buffer = new byte[65536];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            String reply = new String(buffer, 0, response.getLength());
            System.out.println("Response received: " + reply);

            String nodeCount = reply.substring(11, 12);
            if (nodeCount.equals("0")) {
                // request is successful, no nodes in the system
                System.out.println("Request is successful. " + username + " registered as first node in the system");
            } else if (nodeCount.equals("1")) {
                // request is successful, 1 contact will be returned
                String[] neighbour1 = reply.substring(13).split("\\s+");
                neighboursList.add(new NodeNeighbour(neighbour1[0], Integer.parseInt(neighbour1[1])));
                System.out.println("Request is successful. " + username + " registered as second node in the system. Sending 1 node contact to join with...");
                sendJoinRequests();
            } else if (nodeCount.equals("2")) {
                // request is successful, 2 contacts will be returned
                String[] neighbour1 = reply.substring(13).split("\\s+");
                neighboursList.add(new NodeNeighbour(neighbour1[0], Integer.parseInt(neighbour1[1])));
                neighboursList.add(new NodeNeighbour(neighbour1[2], Integer.parseInt(neighbour1[3])));
                System.out.println("Request is successful. Sending 2 node contacts to join with...");
                sendJoinRequests();
            } else {
                String errorCode = reply.substring(11, 15);
                if (errorCode.equals("9999")) {
                    // failed, there is some error in the command
                    System.out.println("Command failed. There is some error in the command. Retry node initiation.");
                } else if (errorCode.equals("9998")) {
                    // failed,  already registered to you, unregister first
                    sendUnRegRequest();
                    System.out.println("Command failed. Node is already registered. Unregister using \'UNREG\' command.");
                } else if (errorCode.equals("9997")) {
                    // failed,   registered to another user, try a different IP and port
                    System.out.println("Command failed. IP and port already in use. Retry initiation with different IP and port");
                } else if (errorCode.equals("9996")) {
                    // failed,  can’t register. BS full.
                    System.out.println("Cannot register more node. Server is full.");
                    System.exit(0);
                }
            }

            Thread listner = new Thread(this);
            listner.start();

        } catch (SocketTimeoutException ex) {
            System.out.println("Timeout error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Node error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void sendJoinRequests() throws IOException {
        //DatagramSocket socket = new DatagramSocket(port);

        String message = Config.JOIN + " " + ip + " " + port;
        int msgLength = message.length() + 5;
        message = format("%04d", msgLength) + " " + message;

        for (NodeNeighbour node : neighboursList) {
            InetAddress address = InetAddress.getByName(node.getIp());
            DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, node.getPort());
            socket.send(request);
            System.out.println("Request sent: " + message);
        }
    }

    private void sendUnRegRequest() throws IOException {
        String message = Config.UNREG + " " + ip + " " + port + " " + username;
        int msgLength = message.length() + 5;
        message = format("%04f", msgLength) + " " + message;
        InetAddress address = InetAddress.getByName(serverHostName);
        DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, serverHostPort);
        socket.send(request);
        System.out.println("Request sent: " + message);
    }

    private ArrayList<String> search(String query) throws IOException {
        ArrayList<String> resultFiles = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(query, " ");

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            for (String file : files) {
                if (file.toLowerCase().contains(token.toLowerCase())) {
                    resultFiles.add(file);
                }
            }
        }
        return resultFiles;
    }

    public void run() {
        System.out.println("Node is listening on port " + port);
        DatagramSocket sock = null;
        String dataReceived;

        try {
            //sock = new DatagramSocket(port);
            sock = this.socket;
            while (true) {
                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                sock.receive(incoming);

                byte[] data = incoming.getData();
                dataReceived = new String(data, 0, incoming.getLength());
                dataReceived = dataReceived.trim();
                System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + " : " + incoming.getPort() + " - " + dataReceived);

                StringTokenizer st = new StringTokenizer(dataReceived, " ");

                String firstToken = st.nextToken();
                String command = "";
                String length = "";

                //Handles separately because it is a user initiated command
                if (firstToken.equals(Config.SEARCHFILE)) {
                    command = firstToken;
                } else {
                    length = firstToken;
                    command = st.nextToken();
                }

                if (command.equals(Config.JOIN)) {
                    String reply = Config.JOINOK + " 0";
                    int msgLength = reply.length() + 5;
                    reply = format("%04d", msgLength) + " " + reply;

                    String ip = st.nextToken();
                    int port = Integer.parseInt(st.nextToken());

                    System.out.println(ip + ":" + port + " is joining node " + username);
                    neighboursList.add(new NodeNeighbour(ip, port));

                    DatagramPacket dpReply = new DatagramPacket(reply.getBytes(), reply.getBytes().length, incoming.getAddress(), incoming.getPort());
                    sock.send(dpReply);
                } else if (command.equals(Config.JOINOK)) {
                    String status = st.nextToken();
                    if (status.equals("0")) {
                        System.out.println("Join successful");
                    } else if (status.equals("9999")) {
                        System.out.println("Error while adding new node to routing table");
                    }
                } else if (command.equals(Config.NODEUNREG)) {
                    sendUnRegRequest();

                } else if (command.equals(Config.UNROK)) {
                    for (NodeNeighbour n : neighboursList) {
                        String message = Config.LEAVE + " " + ip + " " + port;
                        message = format("%04d", message.length() + 5) + " " + message;
                        InetAddress address = InetAddress.getByName(n.getIp());
                        DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, n.getPort());
                        sock.send(request);
                        System.out.println("Request sent: " + message);
                    }


                } else if (command.equals(Config.LEAVE)) {
                    String leaveIP = st.nextToken();
                    String message = Config.LEAVEOK;
                    int leavePort = Integer.parseInt(st.nextToken());
                    for (NodeNeighbour n : neighboursList) {
                        if (n.getPort() == leavePort) {
                            if (neighboursList.remove(n)) {
                                message = message + " 0";
                            } else {
                                message = message + " 9999";
                            }
                            message = String.format("%04d", message.length() + 5) + " " + message;
                            DatagramPacket request = new DatagramPacket(message.getBytes(),
                                    message.getBytes().length, incoming.getAddress(), incoming.getPort());
                            sock.send(request);
                            System.out.println("Request sent: " + message);
                        }
                    }

                } else if (command.equals(Config.LEAVEOK)) {
                    String status = st.nextToken();
                    if (status.equals("0")) {
                        System.out.println("Leave Successful");
                    } else if (status.equals("9999")) {
                        System.out.println("Leave Faild");
                    }

                } else if (command.equals(Config.SEARCHFILE)) {
                    // SEARCHFILE query hopsToSearch
                    String query = st.nextToken();
                    int initialHopCount = Integer.parseInt(st.nextToken());
                    String queryID = this.username + "_" + queriesInitiatedByThisNode.size();
                    queriesInitiatedByThisNode.add(queryID);
                    queryList.put(queryID, ip + ":" + port);
                    ArrayList<String> searchResults = search(query);
                    if (searchResults.size() > 0) {
                        sendLocalSearchResults(0, searchResults, queryID);
                    }
                    if (initialHopCount > 0) {
                        initiateRemoteSearch(query, initialHopCount, queryID, initialHopCount, ip, port);
                    }

                } else if (command.equals(Config.SER)) {
                    String searchNodeIP = st.nextToken();
                    String searchNodePort = st.nextToken();
                    String query = st.nextToken();
                    int hopsLeft = Integer.parseInt(st.nextToken());
                    String queryID = st.nextToken();
                    int initialHopCount = Integer.parseInt(st.nextToken());
                    if (queryList.containsKey(queryID)) {
                        System.out.println("Search request received is handled already in response to a request from another node");
                    } else {
                        queryList.put(queryID, searchNodeIP + ":" + searchNodePort);
                        ArrayList<String> searchResults = search(query);
                        if (searchResults.size() > 0) {
                            sendLocalSearchResults(initialHopCount - hopsLeft, searchResults, queryID);
                        }
                        if (hopsLeft > 0) {
                            initiateRemoteSearch(query, hopsLeft - 1, queryID, initialHopCount, searchNodeIP, Integer.parseInt(searchNodePort));
                        }
                    }

                } else if (command.equals(Config.SEROK)) {
                    //length SEROK no_files IP port hopsWhenFound filename1 filename2 ... ... IPOfNeighborRequested PortOfNeighbourRequested
                    int fileCount = Integer.parseInt(st.nextToken());
                    String IPHavingFile = st.nextToken();
                    String portHavingFile = st.nextToken();
                    String hopsWhenFound = st.nextToken();
                    ArrayList<String> resultFileList = new ArrayList<>();
                    for (int i = 0; i < fileCount; i++) {
                        resultFileList.add(st.nextToken());
                    }
                    String queryID = st.nextToken();
                    if (queryList.get(queryID).split(":")[0].equals(this.ip)) {
                        ArrayList<SearchResult> resultsPerFileName;
                        for (String file : resultFileList) {
                            if (resultsOfQueriesInitiatedByThisNode.containsKey(file)) {
                                resultsPerFileName = resultsOfQueriesInitiatedByThisNode.get(file);
                            } else {
                                resultsPerFileName = new ArrayList<>();
                            }
                            resultsPerFileName.add(new SearchResult(file, IPHavingFile, portHavingFile, hopsWhenFound));
                            System.out.println("File Name : '" + file + "' (' nodeIP:'" + IPHavingFile + "' nodePort:'" + portHavingFile + "' hopsWheFound:'" + hopsWhenFound + "')");
                        }
                    } else {
                        forwardSearchResults(Integer.parseInt(hopsWhenFound), resultFileList, queryID);
                    }
                } else if (command.equals(Config.DOWNLOAD)) {
                    //DOWNLOAD filename
                    String filename = st.nextToken();
                    if (resultsOfQueriesInitiatedByThisNode.containsKey(filename)) {
                        ArrayList<SearchResult> resultsPerFileName = resultsOfQueriesInitiatedByThisNode.get(filename);
                    } else {
                        System.out.println("File you requested ti download is not available in search results");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("IOException " + e);
        }

    }


    private void initiateRemoteSearch(String query, int hopCount, String queryID, int initialHopCount, String senderIP, int senderPort) throws IOException {
        DatagramSocket sock = this.socket;
        for (NodeNeighbour node : neighboursList) {
            if (node.getPort() == senderPort && node.getIp().equals(senderIP)) {
                continue;
            } else {
                String message = Config.SER + " " + ip + " " + port + " " + query + " " + (hopCount)
                        + " " + queryID + " " + initialHopCount;
                int msgLength = message.length() + 5;
                message = format("%04d", msgLength) + " " + message;
                DatagramPacket dpReply = new DatagramPacket(message.getBytes(), message.getBytes().length,
                        InetAddress.getByName(node.getIp()), node.getPort());
                sock.send(dpReply);
            }
        }

    }

    private void sendLocalSearchResults(int hopsWhenFound, ArrayList<String> searchResults, String queryID) throws IOException {

        String requestorIP = queryList.get(queryID).split(":")[0];
        int requestorPort = Integer.parseInt(queryList.get(queryID).split(":")[1]);
        //length SEROK no_files IP port hopsWhenFound filename1 filename2 ... ... queryID
        String fileSet = "";
        for (String file : searchResults) {
            fileSet = file + " ";
        }
        String message = Config.SEROK + " " + searchResults.size() + " " + ip + " " + port + " " + hopsWhenFound + " " + fileSet + queryID;
        int msgLength = message.length() + 5;
        message = format("%04d", msgLength) + " " + message;

        InetAddress address = InetAddress.getByName(requestorIP);
        DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length,
                address, requestorPort);
        socket.send(request);
    }

    private void forwardSearchResults(int hopsWhenFound, ArrayList<String> searchResults, String queryID) throws IOException {

        String requestorIP = queryList.get(queryID).split(":")[0];
        int requestorPort = Integer.parseInt(queryList.get(queryID).split(":")[1]);
        //length SEROK no_files IP port hopsWhenFound filename1 filename2 ... ... queryID
        String fileSet = "";
        for (String file : searchResults) {
            fileSet = file + " ";
        }
        String message = Config.SEROK + " " + searchResults.size() + " " + ip + " " + port + " " + hopsWhenFound + " " + fileSet + queryID;
        int msgLength = message.length() + 5;
        message = format("%04d", msgLength) + " " + message;

        InetAddress address = InetAddress.getByName(requestorIP);
        DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length,
                address, requestorPort);
        socket.send(request);
    }
}