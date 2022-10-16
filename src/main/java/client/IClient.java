/**
 * Interface for client.
 */

package client;

import canvas.Canvas;
import canvas.ICanvasMsg;
import server.IBoardMgr;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface IClient extends Remote {

    // Get client's username
    String getName() throws RemoteException;

    // Change client's username
    void setName(String s) throws RemoteException;

    // Set the client as manager
    void setAsManager() throws RemoteException;

    // Check if client is the manager
    boolean isManager() throws RemoteException;

    // Client requires access to the canvas
    boolean needAccess(String name) throws RemoteException;

    // Get client's access status
    boolean getAccess() throws RemoteException;

    // Change client's access
    void setAccess(boolean access) throws RemoteException;

    // Update the client list
    void updateClientList(Set<IClient> clientList) throws RemoteException;

    // Get client's canvas
    Canvas getCanvas() throws RemoteException;

    // Sync new updates on the canvas
    void syncCanvas(ICanvasMsg draw) throws RemoteException;

    // Clean up the canvas
    void cleanCanvas() throws RemoteException;

    // Get the current canvas layout
    byte[] getCurrentCanvas() throws IOException;

    // Override the current canvas with another one
    void overrideCanvas(byte[] canvas) throws IOException;

    // Quit the white board
    void forceQuit() throws IOException;

    // Sync new chat messages
    void syncChat(String msg) throws RemoteException;

    // Get current chat history
    byte[] getChatHistory() throws IOException;

    // Render UI
    void renderUI(IBoardMgr boardMgr) throws RemoteException;
}
