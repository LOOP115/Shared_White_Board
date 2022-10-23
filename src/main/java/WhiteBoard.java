/**
 * Encapsulate server and client together in one jar file
 */

import client.Client;
import client.IClient;
import server.BoardMgr;
import server.IBoardMgr;

import javax.swing.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class WhiteBoard {

    public static void main(String[] args) {

        // Specify server or client
        if (args.length == 0) {
            System.out.println("Invalid arguments");
            System.exit(0);
        }
        String role = args[0];

        // Default IP address and port for server
        String serverIP = "localhost";
        String serverPort = "3200";

        switch (role) {
            case "StartServer":
                if (args.length > 1) {
                    if (args.length != 2) {
                        System.out.println("Invalid arguments");
                        System.out.println("Please only specify port for server to start");
                        System.exit(0);
                    } else {
                        serverPort = args[1];
                    }
                }

                // Start the server, waiting for clients to connect
                try {
                    IBoardMgr server = new BoardMgr();
                    Registry registry = LocateRegistry.createRegistry(Integer.parseInt(serverPort));
                    registry.bind("Canvas", server);
                    System.out.println("Server Running...");
                } catch (Exception e) {
                    System.out.println("Error starting the server");
                    System.exit(0);
                }
                break;

            case "CreateWhiteBoard":
                // Default username for the client manager
                String managerName = "Genesis";

                // Specify IP address and port from arguments
                if (args.length > 1) {
                    if (args.length != 4) {
                        System.out.println("Invalid arguments");
                        System.exit(0);
                    } else {
                        serverIP = args[1];
                        serverPort = args[2];
                        managerName = args[3];
                    }
                }

                try {
                    // Search the server
                    String serverAddress = "//" + serverIP + ":"+ serverPort + "/Canvas";
                    IBoardMgr server = (IBoardMgr) Naming.lookup(serverAddress);

                    // Login and create the white board
                    IClient client = new Client(server, managerName);
                    try {
                        server.login(client);
                    } catch(RemoteException e) {
                        System.err.println("Login error, unable to connect to server!");
                        System.exit(0);
                    }
                    client.renderUI(server);
                } catch(Exception e) {
                    System.err.println("Connection error!");
                    System.exit(0);
                }
                break;

            case "JoinWhiteBoard":
                // Default username for users
                String username = "Alien";

                // Specify IP address and port from arguments
                if (args.length > 1) {
                    if (args.length != 4) {
                        System.out.println("Invalid arguments");
                        System.exit(0);
                    } else {
                        serverIP = args[1];
                        serverPort = args[2];
                        username = args[3];
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
                break;

            default:
                System.out.println("Invalid mode");
                System.out.println("Running mode must be one of StartServer, CreateWhiteBoard and JoinWhiteBoard");
                System.exit(0);
        }
    }

}
