/**
 * Class for messages on the canvas
 */

package canvas;

import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class CanvasMsg extends UnicastRemoteObject implements ICanvasMsg {

    private static final long serialVersionUID = 1L;
    private final String drawState;
    private final String drawType;
    private final Color color;
    private final Point point;
    private final String text;
    private final String username;

    public CanvasMsg(String state, String msgType, Color color, Point point, String text, String username) throws RemoteException {
        this.drawState = state;
        this.drawType = msgType;
        this.color = color;
        this.point = point;
        this.text = text;
        this.username = username;
    }

    @Override
    public String getDrawState() throws RemoteException {
        return this.drawState;
    }

    @Override
    public String getDrawType() throws RemoteException {
        return this.drawType;
    }

    @Override
    public Color getColor() throws RemoteException {
        return this.color;
    }

    @Override
    public Point getPoint() throws RemoteException {
        return this.point;
    }

    @Override
    public String getText() throws RemoteException {
        return this.text;
    }

    @Override
    public String getUsername() throws RemoteException {
        return this.username;
    }

}
