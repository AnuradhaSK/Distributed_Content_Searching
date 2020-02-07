package lk.ac.mrt.cse.solutia.node;

import lk.ac.mrt.cse.solutia.utils.Config;

import java.util.*;

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
    private ArrayList<NodeNeighbour> neighboursList = new ArrayList<NodeNeighbour>();
    private ArrayList<String> files = new ArrayList<>(); //files that owned by the node
    private HashMap<String, String> queryResults = new HashMap<String, String>();
    private ArrayList<String> queries = new ArrayList<String>();

    private String serverHostName = Config.BOOTSTRAP_IP; //Bootstrap server ip
    private int serverHostPort = Config.BOOTSTRAP_PORT; //Bootstrap server port

    public void initiateNode() {

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
            this.directoryGenerator();
            InetAddress address = InetAddress.getByName(serverHostName);
            DatagramSocket socket = new DatagramSocket();

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
                this.fileGenerate();
            } else if (nodeCount.equals("1")) {
                // request is successful, 1 contact will be returned
                String[] neighbour1 = reply.substring(13).split("\\s+");
                neighboursList.add(new NodeNeighbour(neighbour1[0], Integer.parseInt(neighbour1[1])));
                System.out.println("Request is successful. " + username + " registered as second node in the system. Sending 1 node contact to join with...");
                this.fileGenerate();
                sendJoinRequests();
            } else if (nodeCount.equals("2")) {
                // request is successful, 2 contacts will be returned
                String[] neighbour1 = reply.substring(13).split("\\s+");
                neighboursList.add(new NodeNeighbour(neighbour1[0], Integer.parseInt(neighbour1[1])));
                neighboursList.add(new NodeNeighbour(neighbour1[2], Integer.parseInt(neighbour1[3])));
                System.out.println("Request is successful. Sending 2 node contacts to join with...");
                this.fileGenerate();
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
        DatagramSocket socket = new DatagramSocket();

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
        DatagramSocket socket = new DatagramSocket();
        String message = Config.UNREG + " " + ip + " " + port + " " + username;
        int msgLength = message.length() + 5;
        message = format("%04f", msgLength) + " " + message;
        InetAddress address = InetAddress.getByName(serverHostName);
        DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, serverHostPort);
        socket.send(request);
        System.out.println("Request sent: " + message);
    }

    private String search(String query) throws IOException {
        HashMap<String, ArrayList<String>> queryResults = new HashMap<String, ArrayList<String>>();
        ArrayList<String> resultFiles = new ArrayList<String>();
        String serializedObject = "";
        StringTokenizer st = new StringTokenizer(query, " ");

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            for (String file : files) {
                if (file.toLowerCase().contains(token.toLowerCase())) {
                    resultFiles.add(file);
                }
            }
        }
        queryResults.put(ip + ":" + port, resultFiles);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream so = new ObjectOutputStream(bo);
        so.writeObject(queryResults);
        so.flush();
        serializedObject = bo.toString();
        return serializedObject;
    }

    public void run() {
        System.out.println("Node is listening on port " + port);
        DatagramSocket sock = null;
        String dataReceived;

        try {
            sock = new DatagramSocket(port);

            while (true) {
                byte[] buffer = new byte[65536];
                DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                sock.receive(incoming);

                byte[] data = incoming.getData();
                dataReceived = new String(data, 0, incoming.getLength());

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
                    String query = st.nextToken();
                    String queryID = this.username + "_" + "0";
                    queries.add(queryID);
                    String resultFiles = search(query);
                    int initialHopCount = Config.HOP_COUNT;
                    initiateRemoteSearch(query, initialHopCount, queryID);

                } else if (command.equals(Config.SER)) {
                    String searchNodeIP = st.nextToken();
                    String searchNodePort = st.nextToken();
                    String query = st.nextToken();
                    int hopsLeft = Integer.parseInt(st.nextToken());
                    String queryID = st.nextToken();
                    if (queries.contains(queryID)) {
                        System.out.println("Search request received is handled already in response to a request from another node");
                    } else {
                        queries.add(queryID);
                        // ArrayList<String> searchResults = search(query);
                        if (hopsLeft > 0) {
                            initiateRemoteSearch(query, hopsLeft - 1, queryID);
                        }
                    }

                } else if (command.equals(Config.ECHO)) {
                }

            }
        } catch (IOException e) {
            System.err.println("IOException " + e);
        }

    }


    private void initiateRemoteSearch(String query, int hopCount, String queryID) throws IOException {
        DatagramSocket sock = new DatagramSocket(port);
        for (NodeNeighbour node : neighboursList) {
            String message = Config.SER + " " + ip + " " + port + " " + query + " " + (hopCount - 1)
                    + " " + queryID;
            int msgLength = message.length() + 5;
            message = format("%04d", msgLength) + " " + message;
            DatagramPacket dpReply = new DatagramPacket(message.getBytes(), message.getBytes().length,
                    InetAddress.getByName(node.getIp()), node.getPort());
            sock.send(dpReply);
            //TODO:: TTL
        }
        int responseCounter = 0;
        while (responseCounter < neighboursList.size()) {
            byte[] buffer = new byte[65536];
            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
            sock.receive(incoming);

            byte[] data = incoming.getData();
            String dataReceived = new String(data, 0, incoming.getLength());

            if (dataReceived.equals("skip")) {
                responseCounter++;
                continue;
            } else {

            }

            StringTokenizer st = new StringTokenizer(dataReceived, " ");

            String firstToken = st.nextToken();
        }
    }


    private void fileGenerate() throws IOException {
        ArrayList<String> fileNames = new ArrayList<>();
        BufferedReader reader;
        try {
//            String path = "resources";
//            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
//            path = classLoader.getResource(path).getPath().split("target")[0].substring(1)+"src/main/resources/File Names.txt";;

            reader = new BufferedReader(new FileReader(Config.FILENAMESTEXT));
            String line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
                fileNames.add(line);
            }
            System.out.println(fileNames.get(0));
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int number_of_files = getRandomNumberInRange(3,5);
        System.out.println("number of files in the node:"+ number_of_files);
        int i = 0;
        Set file_numbers = new HashSet();
        while (i < number_of_files) {
            int file_index = getRandomNumberInRange(0,19);
            System.out.println(file_index);
            if(file_numbers.contains(file_index)){
                continue;
            }
            i+=1;
            file_numbers.add(file_index);
            String file_name = fileNames.get(file_index);

            System.out.println(file_name);
            files.add(file_name);

            int file_size = file_name.length()%10;
            if (file_size < 2){
                file_size = 2;
            }

            int FILE_SIZE = 1000 * 1000* file_size;
            String file_path = Config.FILECONTAINER +"/"+ file_name ;

            java.io.File file_write = new java.io.File(file_path);
            try (BufferedWriter writer = Files.newBufferedWriter(file_write.toPath())) {
                while (file_write.length() < FILE_SIZE) {
                    writer.write(file_name);
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Files are generated to /var/tmp/overlay/generated_file folder");
    }

    private static int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    private void directoryGenerator(){
        File directory = new File(Config.DOWNLODED);
        if (! directory.exists()){
            directory.mkdir();
        }
        File directory2 = new File(Config.FILECONTAINER);
        if (! directory2.exists()){
            directory2.mkdir();
        }
    }

}