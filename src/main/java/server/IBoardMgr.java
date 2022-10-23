/**
 * Interface for managing the white board.
 * Methods below can be called by clients remotely.
 */

package server;

import client.IClient;
import canvas.ICanvasMsg;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface IBoardMgr extends Remote {

    // Record clients connected to the server
    void login(IClient client) throws RemoteException;

    // Check if the name is duplicated
    boolean invalidUsername(String username) throws RemoteException;

    // Get list of clients
    Set<IClient> getClients() throws RemoteException;

    // Sync client lists in all clients
    void syncClientList() throws RemoteException;

    // Client quits the whiteboard
    void quitClient(String username) throws RemoteException;

    // Manager kicks out a client
    void kickClient(String username) throws RemoteException;

    // Remove all the clients
    void removeAllClients() throws IOException;

    // Broadcast updates of canvas to all clients
    void broadcastMsg(ICanvasMsg draw) throws RemoteException;

    // Send the current canvas to newly joined clients
    byte[] sendCurrentCanvas() throws IOException;

    // Send existing canvas to all clients when the manager opens it
    void sendExistCanvas(byte[] canvas) throws IOException;

    // Clean the shared canvas
    void cleanCanvas() throws RemoteException;

    // Send the new chat to the chat window
    void broadcastChat(String chat) throws RemoteException;

    // Send the current chat history to newly joined clients
    void syncChatHistory(IClient client) throws IOException;

}
