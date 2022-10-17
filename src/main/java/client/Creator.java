/**
 * Run the first client to create a board.
 * This client will be the manager.
 */

package client;

import server.IBoardMgr;

import java.rmi.Naming;
import java.rmi.RemoteException;

public class Creator {

    public static void main(String[] args) {

        // Default IP address and port for server
        String serverIP = "localhost";
        String serverPort = "3200";
        // Default username for the first user
        String username = "Genesis";

        // Specify IP address and port from arguments
        if (args.length > 0) {
            if (args.length != 3) {
                System.out.println("Invalid arguments");
            } else {
                serverIP = args[0];
                serverPort = args[1];
                username = args[2];
            }
        }

        try {
            // Search the server
            String serverAddress = "//" + serverIP + ":"+ serverPort + "/Canvas";
            IBoardMgr server = (IBoardMgr) Naming.lookup(serverAddress);

            // Login and create the white board
            IClient client = new Client(server, username);
            try {
                server.login(client);
            } catch(RemoteException e) {
                System.err.println("Login error, unable to connect to server!");
            }
            client.renderUI(server);
        } catch(Exception e) {
            System.err.println("Connection error!");
        }
    }

}
