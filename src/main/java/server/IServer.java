/**
 * Interface for server side.
 * Methods below can be called by clients remotely.
 */

package server;

import client.IClient;
import message.IMessage;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface IServer extends Remote {

    // Record clients connected to the server
    void login(IClient client) throws RemoteException;

    // Get list of clients
    Set<IClient> getClients() throws RemoteException;

    // Client quits the whiteboard
    void quitClient(String name) throws RemoteException;

    // Manager kicks out a client
    void kickClient(String name) throws RemoteException;

    // Remove all the clients
    void removeAllClients() throws IOException;

    // Broadcast updates to all clients
    void broadCastMsg(IMessage msg) throws RemoteException;

    // Send the current canvas to newly joined clients
    byte[] sendCurrentCanvas() throws IOException;

    // Send existing canvas to all clients when the manager opens it
    void sendExistCanvas(byte[] canvas) throws IOException;

    // Clean the shared canvas
    void cleanCanvas() throws RemoteException;

    // Send the current chat history to newly joined clients
    void sendCurrentChat() throws IOException;

}
