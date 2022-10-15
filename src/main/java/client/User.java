package client;

import server.IBoardMgr;

import javax.swing.*;
import java.net.MalformedURLException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;

public class User {

    public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException, ServerNotActiveException {

//        if(args.length != 2) {
//        	throw new IllegalArgumentException("Need exactly two arguments.");
//        }

        try {
            //Look up the Canvas Server from the RMI name registry
//            if(!(args[0].equals("localhost") || args[0].equals("127.0.0.1"))) {
//            	System.err.println("Please enter localhost or 127.0.0.1");
//            	return;
//            }
            String hostName = "localhost";
            String portNumber = "3200";
//            String serverAddress = "//" + args[0]+":"+args[1] + "/WhiteBoardServer";
            String serverAddress = "//" + hostName +":"+ portNumber + "/WhiteBoard";

            IBoardMgr server = (IBoardMgr) Naming.lookup(serverAddress);

            // Client login
            boolean validName = false;
            String username = null;
            while(!validName) {
                username = JOptionPane.showInputDialog("Enter your name");
                if(username.equals("")) {
                    JOptionPane.showMessageDialog(null, "Name cannot be empty!");
                } else {
                    validName = true;
                }
                for (IClient c : server.getClients()) {
                    if (username.equals(c.getName()) || c.getName().equals("(Host) " + username)) {
                        validName = false;
                        JOptionPane.showMessageDialog(null, "The name has been taken!");
                    }
                }
            }
            IClient client = new Client(server, username);
            try {
                server.login(client);

            } catch(RemoteException e) {
                System.err.println("Login error, unable to connect to server!");
            }

            // Render UI
            client.renderUI(server);
            // Access denied
            if(!client.getAccess()) {
                server.quitClient(username);
            }

        } catch(ConnectException e) {
            System.err.println("Connection error!");
        } catch(Exception e) {
            System.err.println("Unable connect to the server!");
        }

    }

}
