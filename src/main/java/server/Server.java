/**
 * Server class for the shared white board system.
 * RMI methods are implemented here for clients to call remotely.
 */

package server;

import client.IClient;
import message.IMessage;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

public class Server extends UnicastRemoteObject implements IServer, Serializable {
    
    protected Server() throws RemoteException {
    }

    @Override
    public void login(IClient client) throws RemoteException {

    }

    @Override
    public Set<IClient> getClients() throws RemoteException {
        return null;
    }

    @Override
    public void quitClient(String name) throws RemoteException {

    }

    @Override
    public void kickClient(String name) throws RemoteException {

    }

    @Override
    public void removeAllClients() throws IOException {

    }

    @Override
    public void broadCastMsg(IMessage msg) throws RemoteException {

    }

    @Override
    public byte[] sendCurrentCanvas() throws IOException {
        return new byte[0];
    }

    @Override
    public void sendExistCanvas(byte[] canvas) throws IOException {

    }

    @Override
    public void cleanCanvas() throws RemoteException {

    }

    @Override
    public void sendCurrentChat() throws IOException {

    }

}
