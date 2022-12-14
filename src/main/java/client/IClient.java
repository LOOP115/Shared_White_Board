/**
 * Interface for client.
 */

package client;

import canvas.ICanvasMsg;

import javax.swing.*;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;

public interface IClient extends Remote {

    // Get client's username
    String getUsername() throws RemoteException;

    // Change client's username
    void setUsername(String s) throws RemoteException;

    // Set the client as manager
    void setAsManager() throws RemoteException;

    // Client requires access to the canvas
    boolean needAccess(String username) throws RemoteException;

    // Get client's access status
    boolean getAccess() throws RemoteException;

    // Change client's access
    void setAccess(boolean access) throws RemoteException;

    // Update the client list
    void syncClientList(Set<IClient> clientList) throws RemoteException;

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
    DefaultListModel<String> getChatHistory() throws IOException;

    // Sync chat history for newly joined clients
    void syncChatHistory(DefaultListModel<String> chatHistory) throws RemoteException;

    // Configure buttons and windows
    void configUI() throws RemoteException;

    // Initialise and render the UI
    void renderUI() throws RemoteException;

}
