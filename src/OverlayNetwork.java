import bootsrtap_server.BootstrapServer;
import node.Node;

public class OverlayNetwork {

    public static void main(String args[]) {

        if (args.length < 1) {
            System.out.println("Invalid number of parameters");
            return;
        } else {
            String mode = args[0];
            System.out.println(mode);

            if (mode.equals("1")) {
                //Starts the bootstrap server

                BootstrapServer server = new BootstrapServer();
                server.startServer();
            } else if (mode.equals("2")) {
                //Starts a node with the given port and ip address

                String ip = args[1];
                String port = args[2];
                String username = args[3];

                Node node = new Node(ip, port, username);
                node.initiateNode();

                //TODO:Validate arguments
            }

        }
    }
}

