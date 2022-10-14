/**
 * Server side of the system.
 */

package server;

import javax.swing.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server {

    public static void main(String[] args) {

        String port = "3200";

        try {
            IBoardServer server = new BoardServer();
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(port));
            registry.bind("WhiteBoard", server);
            JOptionPane.showMessageDialog(null, "Server is started!");
        } catch (Exception e) {
            System.out.println("Error starting the server");
        }
    }

}
