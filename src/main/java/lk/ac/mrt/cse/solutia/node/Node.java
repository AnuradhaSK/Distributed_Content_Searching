package lk.ac.mrt.cse.solutia.node;

import lk.ac.mrt.cse.solutia.utils.Config;

import java.util.Scanner;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

import static java.lang.String.format;

public class Node {

    private String ip;
    private int port;
    private String username;

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
                System.out.println("Request is successful. " + username + " registered as second node in the system. Sending 1 node contact to join with...");
            } else if (nodeCount.equals("2")) {
                // request is successful, 2 contacts will be returned
                System.out.println("Request is successful. Sending 2 node contacts to join with...");
            } else {
                String errorCode = reply.substring(11, 15);
                if (errorCode.equals("9999")) {
                    // failed, there is some error in the command
                    System.out.println("Command failed. There is some error in the command. Retry node initiation.");
                } else if (errorCode.equals("9998")) {
                    // failed,  already registered to you, unregister first
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

            while (true) {
                userInput = scanner.next();

                /*switch (userInput) {
                    case "UNREG":
                        //do the job number 1
                        System.out.println("done with job number 1");
                        break;

                    case "SEARCH":
                        //do the job number 2
                        System.out.println("done with job number 2");
                        break;

                    default:
                        //inform user in case of invalid choice.
                        System.out.println("Invalid command.");*/
                //}


            }


        } catch (SocketTimeoutException ex) {
            System.out.println("Timeout error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Node error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
