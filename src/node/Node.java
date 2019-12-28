package node;

import java.io.*;
import java.net.*;

import static java.lang.String.format;

public class Node {

    private String ip;
    private String port;
    private String username;

    public Node(String ip, String port, String username) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        echo("Node initiated at " + ip + ":" + port + " with the username - " + username);
    }

    public void initiateNode() {

        //TODO: Code to read these from a config.properties file
        String hostname = "192.168.8.101";
        int port = 55555;

        try {
            InetAddress address = InetAddress.getByName(hostname);
            DatagramSocket socket = new DatagramSocket();

            String message = "REG " + ip + " " + port + " " + username;
            int msgLength = message.length() + 5;
            message = format("%04d", msgLength) + " " + message;
            System.out.println("Request sent: " + message);

            DatagramPacket request = new DatagramPacket(message.getBytes(), message.getBytes().length, address, port);
            socket.send(request);

            byte[] buffer = new byte[65536];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            String reply = new String(buffer, 0, response.getLength());
            System.out.println("Response received: " + reply);

            String nodeCount = reply.substring(11, 12);
            if (nodeCount.equals("0")) {
                // request is successful, no nodes in the system
            } else if (nodeCount.equals("1")) {
                // request is successful, 1 contact will be returned
            } else if (nodeCount.equals("2")) {
                // request is successful, 2 contacts will be returned
            } else {
                String errorCode = reply.substring(11, 15);
                if (errorCode.equals("9999")) {
                    // failed, there is some error in the command
                } else if (errorCode.equals("9998")) {
                    // failed,  already registered to you, unregister first
                } else if (errorCode.equals("9997")) {
                    // failed,   registered to another user, try a different IP and port
                } else if (errorCode.equals("9996")) {
                    // failed,  can’t register. BS full.
                }
            }


        } catch (SocketTimeoutException ex) {
            System.out.println("Timeout error: " + ex.getMessage());
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("Client error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    //simple function to echo data to terminal
    public static void echo(String msg) {
        System.out.println(msg);
    }
}
