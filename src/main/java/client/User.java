/**
 * Run the client to join a board.
 */

package client;

import server.IBoardMgr;

import javax.swing.*;
import java.rmi.Naming;
import java.rmi.RemoteException;

public class User {

    public static void main(String[] args) {

        // Default IP address and port for server
        String serverIP = "localhost";
        String serverPort = "3200";
        // Default username for users
        String username = "Alien";

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

            // Client login
            if (!server.validUsername(username)) {
                System.out.println("The name has been taken!\nPlease enter a new one.");
                System.exit(0);
            }
            IClient client = new Client(server, username);
            try {
                server.login(client);
            } catch(RemoteException e) {
                System.err.println("Login error, unable to connect to server!");
                System.exit(0);
            }

            // Judge client's access
            if (client.getAccess()) {
                // Render UI
                client.renderUI(server);
            } else {
                server.quitClient(username);
                JOptionPane.showMessageDialog(null,
                        "Access denied!\nPlease contact the manager to obtain access.",
                        "Warning", JOptionPane.INFORMATION_MESSAGE);
                client.forceQuit();
            }

        } catch(Exception e) {
            System.err.println("Connection error!");
            System.exit(0);
        }
    }

}
