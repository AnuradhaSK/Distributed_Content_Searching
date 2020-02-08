package lk.ac.mrt.cse.solutia.node;

//import com.sun.deploy.util.StringUtils;
import lk.ac.mrt.cse.solutia.utils.Config;

import java.util.*;

import java.nio.file.Files;
import java.util.Random;
import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.Instant;

import static java.lang.String.format;

public class Node implements Runnable {

    private String ip;
    private int port;
    private String username;
    private DatagramSocket socket;
    private ArrayList<NodeNeighbour> neighboursList = new ArrayList<NodeNeighbour>();
    private ArrayList<String> files = new ArrayList<>(); //files that owned by the node
    private HashMap<String, ArrayList<SearchResult>> resultsOfQueriesInitiatedByThisNode = new HashMap<>(); //FileName->resultID-><"node:port:file1:file1:file3">
    private HashMap<String, String> queryList = new HashMap<>(); //<QueryID,who sent it to this node>
    private HashMap<String, Instant> querySearchStartTime = new HashMap<>(); //<QueryID,searchStartTime>
    private ArrayList<String> queriesInitiatedByThisNode = new ArrayList<>();

    private int receivedQueryMessagesCount = 0;
    private int forwardedQueryMessagesCount = 0;
    private int answeredQueryMessagesCount = 0;

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
                this.fileGenerate();
            } else if (nodeCount.equals("1")) {
                // request is successful, 1 contact will be returned
                String[] neighbour1 = reply.substring(13).split("\\s+");
                neighboursList.add(new NodeNeighbour(neighbour1[0], Integer.parseInt(neighbour1[1]), neighbour1[2]));
                System.out.println("Request is successful. " + username + " registered as second node in the system. Sending 1 node contact to join with...");
                this.fileGenerate();
                sendJoinRequests();
            } else if (nodeCount.equals("2")) {
                // request is successful, 2 contacts will be returned
                String[] neighbour1 = reply.substring(13).split("\\s+");
                neighboursList.add(new NodeNeighbour(neighbour1[0], Integer.parseInt(neighbour1[1]), neighbour1[2]));
                neighboursList.add(new NodeNeighbour(neighbour1[3], Integer.parseInt(neighbour1[4]), neighbour1[5]));
                System.out.println("Request is successful. Sending 2 node contacts to join with...");
                this.fileGenerate();
                sendJoinRequests();
            } else {
                String errorCode = reply.substring(11, 15);
                if (errorCode.equals("9999")) {
                    // failed, there is some error in the command
                    System.out.println("Command failed. There is some error in the command. Retry node initiation.");
                    System.exit(0);
                } else if (errorCode.equals("9998")) {
                    // failed,  already registered to you, unregister first
                    System.out.println("Command failed. Node is already registered. Unregister using \'UNREG\' command.");
                    sendUnRegRequest();
                    System.exit(0);
                } else if (errorCode.equals("9997")) {
                    // failed,   registered to another user, try a different IP and port
                    System.out.println("Command failed. IP and port already in use. Retry initiation with different IP and port");
                    System.exit(0);
                } else if (errorCode.equals("9996")) {
                    // failed,  can’t register. BS full.
                    System.out.println("Cannot register more nodes. Server is full.");
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

        String message = Config.JOIN + " " + ip + " " + port + " " + username;
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
        message = format("%04d", msgLength) + " " + message;
        InetAddress address = InetAddress.getByName(serverHostName);
        DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, serverHostPort);
        socket.send(request);
        System.out.println("Request sent: " + message);
    }

    private ArrayList<String> search(String query) throws IOException {
        ArrayList<String> resultFiles = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(query.trim(), "_");

        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            for (String file : files) {
                if (Arrays.asList(file.toLowerCase().split(" ")).contains(token.toLowerCase())) {
                    String delemeteredName = "";
                    for (String i : file.split(" ")) {
                        delemeteredName = delemeteredName + i + "_";
                    }
                    String fileNew = delemeteredName.substring(0, delemeteredName.length() - 1);
                    if (!resultFiles.contains(fileNew)) {
                        resultFiles.add(delemeteredName.substring(0, delemeteredName.length() - 1));
                    }
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

                StringTokenizer st = new StringTokenizer(dataReceived, " ");

                String firstToken = st.nextToken();
                String command = "";
                String length = "";

                //Handles separately because they are user initiated commands
                if (firstToken.equals(Config.SEARCHFILE) || firstToken.equals(Config.DOWNLOAD) ||
                        firstToken.equals(Config.GETSTATS) || firstToken.equals(Config.CLEARSTATS) ||
                        firstToken.equals(Config.SHOWROUTES) || firstToken.equals(Config.LEAVENET)) {
                    command = firstToken;
                } else {
                    length = firstToken;
                    command = st.nextToken();
                }

                if (command.equals(Config.JOIN)) {
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
                    String reply = Config.JOINOK + " 0";
                    int msgLength = reply.length() + 5;
                    reply = format("%04d", msgLength) + " " + reply;

                    String ip = st.nextToken();
                    int port = Integer.parseInt(st.nextToken());
                    String username = st.nextToken();

                    System.out.println(ip + ":" + port + " is joining node " + username);
                    neighboursList.add(new NodeNeighbour(ip, port, username));

                    DatagramPacket dpReply = new DatagramPacket(reply.getBytes(), reply.getBytes().length, incoming.getAddress(), incoming.getPort());
                    sock.send(dpReply);
                } else if (command.equals(Config.JOINOK)) {
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
                    String status = st.nextToken();
                    if (status.equals("0")) {
                        System.out.println("Join successful");
                    } else if (status.equals("9999")) {
                        System.out.println("Error while adding new node to routing table");
                    }
                } else if (command.equals(Config.NODEUNREG)) {
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
                    sendUnRegRequest();

                } else if (command.equals(Config.UNROK)) {
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
                    for (NodeNeighbour n : neighboursList) {
                        String message = Config.LEAVE + " " + ip + " " + port;
                        message = format("%04d", message.length() + 5) + " " + message;
                        InetAddress address = InetAddress.getByName(n.getIp());
                        DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, n.getPort());
                        sock.send(request);
                        System.out.println("Request sent: " + message);
                    }


                } else if (command.equals(Config.LEAVE)) {
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
                    String leaveIP = st.nextToken();
                    String message = Config.LEAVEOK;
                    int leavePort = Integer.parseInt(st.nextToken());
                    for (NodeNeighbour n : neighboursList) {
                        if (n.getIp().equals(leaveIP) && n.getPort() == leavePort) {
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
                            break;
                        }
                    }

                } else if (command.equals(Config.LEAVEOK)) {
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
                    String status = st.nextToken();
                    if (status.equals("0")) {
                        System.out.println("Leave Successful");
                    } else if (status.equals("9999")) {
                        System.out.println("Leave Faild");
                    }

                } else if (command.equals(Config.SEARCHFILE)) {
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
                    // SEARCHFILE hopsToSearch query
                    int initialHopCount = Integer.parseInt(st.nextToken());
                    String query = "";
                    while (st.hasMoreTokens()) {
                        query = query + "_" + st.nextToken();
                    }
                    query = query.substring(1, query.length());
                    System.out.println("Searching for : '" + query + "'");
                    String queryID = this.username + "_" + queriesInitiatedByThisNode.size();
                    queriesInitiatedByThisNode.add(queryID);
                    queryList.put(queryID, ip + ":" + port);
                    Instant searchStartTime = Instant.now();
                    querySearchStartTime.put(queryID, searchStartTime);
                    ArrayList<String> searchResults = search(query);
                    if (searchResults.size() > 0) {
                        answeredQueryMessagesCount++;
                        sendLocalSearchResults(0, searchResults, queryID);
                    }
                    if (initialHopCount > 0) {
                        initiateRemoteSearch(query, initialHopCount - 1, queryID, initialHopCount, ip, port);
                    }

                } else if (command.equals(Config.SER)) {
                    receivedQueryMessagesCount++;
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
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
                            answeredQueryMessagesCount++;
                            sendLocalSearchResults(initialHopCount - hopsLeft, searchResults, queryID);
                        }
                        if (hopsLeft > 0) {
                            initiateRemoteSearch(query, hopsLeft - 1, queryID, initialHopCount, searchNodeIP, Integer.parseInt(searchNodePort));
                        }
                    }

                } else if (command.equals(Config.SEROK)) {
                    //length SEROK no_files IP port hopsWhenFound filename1 filename2 ... ... queryID
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
                    int fileCount = Integer.parseInt(st.nextToken());
                    String IPHavingFile = st.nextToken();
                    String portHavingFile = st.nextToken();
                    String hopsWhenFound = st.nextToken();
                    ArrayList<String> resultFileList = new ArrayList<>();
                    for (int i = 0; i < fileCount; i++) {
                        resultFileList.add(st.nextToken());
                    }
                    String queryID = st.nextToken();
                    if (queryList.get(queryID).split(":")[0].equals(this.ip) &&
                            Integer.parseInt(queryList.get(queryID).split(":")[1]) == (this.port)) {
                        ArrayList<SearchResult> resultsPerFileName;
                        for (String file : resultFileList) {
                            if (resultsOfQueriesInitiatedByThisNode.containsKey(file)) {
                                resultsPerFileName = resultsOfQueriesInitiatedByThisNode.get(file);
                            } else {
                                resultsPerFileName = new ArrayList<>();
                            }
                            Instant searchEndTime = Instant.now();
                            Instant searchStartTime = querySearchStartTime.get(queryID);
                            Duration timeElapsed = Duration.between(searchStartTime, searchEndTime);
                            System.out.println("File Name : '" + file + "' (' nodeIP:'" + IPHavingFile + "' nodePort:'" + portHavingFile + "' hopsWhenFound:'" +
                                    hopsWhenFound + "')\n timeElapsedForSearching: " + timeElapsed.toMillis() + "ms");
                            resultsPerFileName.add(new SearchResult(file, IPHavingFile, portHavingFile, hopsWhenFound, timeElapsed.toMillis()));
                            resultsOfQueriesInitiatedByThisNode.put(file,resultsPerFileName);
                        }
                    } else {
                        forwardSearchResults(Integer.parseInt(hopsWhenFound), resultFileList, queryID);
                    }
                } else if (command.equals(Config.DOWNLOAD)) {
                    System.out.println("Message received from address " + incoming.getAddress().getHostAddress() + ":" +
                            incoming.getPort() + " - " + dataReceived);
                    //DOWNLOAD filename
                    String filename = st.nextToken();
                    System.out.println(filename);
                    System.out.println(resultsOfQueriesInitiatedByThisNode.size());
                    if (resultsOfQueriesInitiatedByThisNode.containsKey(filename)) {
                        ArrayList<SearchResult> resultsPerFileName = resultsOfQueriesInitiatedByThisNode.get(filename);
                        System.out.println(resultsPerFileName.size());
                        int min_hop = Integer.parseInt(resultsPerFileName.get(0).getHopsToReach());
                        SearchResult selected_node = resultsPerFileName.get(0);
                        for (SearchResult result : resultsPerFileName) {
                            if(Integer.parseInt(result.getHopsToReach()) < min_hop) {
                                min_hop = Integer.parseInt(result.getHopsToReach());
                                selected_node = result;
                            }
                        }
                        this.download(selected_node.getHostIP(),filename);


                    } else {
                        System.out.println("File you requested to download is not available in search results");
                    }
                } else if (command.equals(Config.GETSTATS)) {
                    System.out.println("Node : " + username + " - " + ip + ":" + port + " search statistics");
                    System.out.println("Queries received : " + receivedQueryMessagesCount);
                    System.out.println("Queries answered : " + answeredQueryMessagesCount);
                    System.out.println("Queries forwarded : " + forwardedQueryMessagesCount);

                } else if (command.equals(Config.CLEARSTATS)) {
                    this.resultsOfQueriesInitiatedByThisNode = new HashMap<>();
                    this.queryList = new HashMap<>();
                    this.queriesInitiatedByThisNode = new ArrayList<>();
                    receivedQueryMessagesCount = 0;
                    forwardedQueryMessagesCount = 0;
                    answeredQueryMessagesCount = 0;
                } else if (command.equals(Config.SHOWROUTES)) {
                    System.out.println("Routing table for node : " + username + " - " + ip + ":" + port);
                    System.out.println("Username | IP Address | Port");
                    for (NodeNeighbour node : neighboursList) {
                        System.out.println(node.getUsername() + " | " + node.getIp() + " | " + node.getPort());
                    }
                }
                else if (command.equals(Config.LEAVENET)) {
                    sendUnRegRequest();
                } else {
                    System.out.println("Invalid Command!");
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
                forwardedQueryMessagesCount++;
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

    private void download(String ip, String filename) throws IOException {
        filename = String.join("%20",filename.split("_"));
        URL url = null;
        try {
            url = new URL("http://"+ ip +":8080/downloadFile/" + filename);
        } catch (MalformedURLException e) {
            throw new Error("Either no legal protocol could be found in a specification string or the string could not be parsed");
        }
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            con.setRequestMethod("GET");        //creating a GET request
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        //set timeouts
        con.setConnectTimeout(15000);
        con.setReadTimeout(15000);

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (con.getInputStream())));

        String output;
        System.out.println("Starting Downloading .... \n");
        String path = Config.DOWNLOADED +"/"+ String.join(" ", filename.split("%20"));
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        while ((output = br.readLine()) != null) {
            writer.write(output);
            writer.write("\n");
        }
        writer.close();
        con.disconnect();
        System.out.println("Downloading Completed\n");
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
        File directory = new File(Config.DOWNLOADED);
        if (! directory.exists()){
            directory.mkdir();
        }
        File directory2 = new File(Config.FILECONTAINER);
        if (! directory2.exists()){
            directory2.mkdir();
        }
    }

}