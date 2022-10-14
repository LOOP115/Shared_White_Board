/**
 * Class of client using the white board
 */

package client;

import canvas.ICanvasMsg;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

public class Client extends UnicastRemoteObject implements IClient {

    protected Client() throws RemoteException {
    }

    @Override
    public String getName() throws RemoteException {
        return null;
    }

    @Override
    public void setName(String s) throws RemoteException {

    }

    @Override
    public void setAsManager() throws RemoteException {

    }

    @Override
    public boolean isManager() throws RemoteException {
        return false;
    }

    @Override
    public boolean needAccess(String name) throws RemoteException {
        return false;
    }

    @Override
    public void setAccess(boolean access) throws RemoteException {

    }

    @Override
    public void updateClientList(Set<IClient> clientList) throws RemoteException {

    }

    @Override
    public void syncCanvas(ICanvasMsg draw) throws RemoteException {

    }

    @Override
    public void cleanCanvas() throws RemoteException {

    }

    @Override
    public byte[] getCurrentCanvas() throws IOException {
        return new byte[0];
    }

    @Override
    public void overrideCanvas(byte[] canvas) throws IOException {

    }

    @Override
    public void forceQuit() throws IOException {

    }

    @Override
    public void syncChat(String msg) throws RemoteException {

    }

    @Override
    public byte[] getChatHistory() throws IOException {
        return new byte[0];
    }
}
