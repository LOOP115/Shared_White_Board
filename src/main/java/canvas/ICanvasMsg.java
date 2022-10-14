/**
 * Interface for messages on the canvas including drawings and chat.
 */

package canvas;

import java.awt.*;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ICanvasMsg extends Remote {

    String getState() throws RemoteException;

    String getMsgType() throws RemoteException;

    Color getColor() throws RemoteException;

    Point getPoint() throws RemoteException;

    String getText() throws RemoteException;

    String getClientName() throws RemoteException;

}
