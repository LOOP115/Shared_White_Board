/**
 * Run the server.
 */

package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {

    public static void main(String[] args) {

        // Specify port number in arguments, default is 3200
        String port = "3200";
        if (args.length > 0) {
            if (args.length != 1) {
                System.out.println("Invalid arguments");
            } else {
                port = args[0];
            }
        }

        // Start the server, waiting for clients to connect
        try {
            IBoardMgr server = new BoardMgr();
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(port));
            registry.bind("Canvas", server);
            System.out.println("Server Running...");
        } catch (Exception e) {
            System.out.println("Error starting the server");
        }
    }

}
