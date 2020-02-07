package lk.ac.mrt.cse.solutia.node;

import lk.ac.mrt.cse.solutia.bootsrtap_server.Neighbour;
import lk.ac.mrt.cse.solutia.utils.Config;

import java.util.*;
import java.io.*;
import java.net.*;

import static java.lang.String.format;

public class Node implements Runnable {

    private String ip;
    private int port;
    private String username;
    private ArrayList<NodeNeighbour> neighboursList = new ArrayList<NodeNeighbour>();
    private String[] files; //files that owned by the node
    HashMap<String, String> queries = new HashMap<String, String>();

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

//            while (true) {
//                if (userInput.equals(Config.UNREG)) {//do the job number 1
//                    System.out.println("done with job number 1");
//
//                } else if (userInput.equals(Config.SER)) {//do the job number 2
//                    System.out.println("done with job number 2");
//
//                }
//                else {//inform user in case of invalid choice.
//                    System.out.println("Invalid command.");
//                }
//            }

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

    private void sendUnRegRequest() throws IOException{
        DatagramSocket socket= new DatagramSocket();
        String message= Config.UNREG +" "+ ip +" "+ port +" "+ username;
        int msgLength = message.length()+5;
        message = format("%04f", msgLength)+" "+ message;
        InetAddress address = InetAddress.getByName(serverHostName);
        DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, serverHostPort);
        socket.send(request);
        System.out.println("Request sent: "+ message);
    }

    private List<String> search(String query) {
        List<String> resultFiles = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(query, " ");

        while(st.hasMoreTokens()) {
            String token = st.nextToken();
            for (String file: files) {
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
        while (true) {

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

                    String length = st.nextToken();
                    String command = st.nextToken();

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
                        String status = st.nextToken();
                        if(status.equals("0")) {
                            for (NodeNeighbour n : neighboursList) {
                                String message = Config.LEAVE + " " + ip + " " + port;
                                message = format("%04d", message.length() + 5) + " " + message;
                                InetAddress address = InetAddress.getByName(n.getIp());
                                DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, n.getPort());
                                sock.send(request);
                                System.out.println("Request sent: " + message);
                            }
                        }
                        else if(status.equals("9999")){
                            System.out.println("Error while unregistering the node");
                        }


                    } else if (command.equals(Config.LEAVE)) {
                            String leaveIP= st.nextToken();
                            String message= Config.LEAVEOK;
                            int leavePort= Integer.parseInt(st.nextToken());
                            for(NodeNeighbour n : neighboursList){
                                if(n.getPort()== leavePort){
                                    if(neighboursList.remove(n)){
                                        message= message+" 0";
                                    }
                                    else{
                                        message= message+" 9999";
                                    }
                                    message= String.format("%04d", message.length()+5)+ " "+ message;
                                    DatagramPacket request= new DatagramPacket(message.getBytes(),
                                            message.getBytes().length, incoming.getAddress(), incoming.getPort());
                                    sock.send(request);
                                    System.out.println("Request sent: "+ message);
                                }
                            }

                    } else if (command.equals(Config.LEAVEOK)) {
                        String status= st.nextToken();
                        if(status.equals("0")){
                            System.out.println("Leave Successful");
                        }
                        else if(status.equals("9999")){
                            System.out.println("Leave Faild");
                        }


                    } else if (command.equals(Config.ECHO)) {

                    } else if (command.equals(Config.ECHO)) {

                    } else if (command.equals(Config.ECHO)) {
                    }

                }
            } catch (IOException e) {
                System.err.println("IOException " + e);
            }

        }
    }
}
