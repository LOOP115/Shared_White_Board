/**
 * Class for managing the white board.
 * RMI methods are implemented here for clients to call remotely.
 */

package server;

import client.IClient;
import client.ClientMgr;
import canvas.ICanvasMsg;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;

public class BoardMgr extends UnicastRemoteObject implements IBoardMgr, Serializable {

    private final ClientMgr manager;

    private IClient clientManager;

    public BoardMgr() throws RemoteException {
        manager = new ClientMgr();
    }

    @Override
    public void login(IClient client) throws RemoteException {
        // The first client is the manager
        if (this.manager.hasNoClient()) {
            client.setAsManager();
            this.clientManager = client;
            client.setName("(Host) " + client.getName());
            this.manager.addClient(client);
            syncClientList();
            try {
                syncChatHistory(client);
            } catch (IOException e) {
                System.out.println("Error syncing chat history!");
            }
            return;
        }

        // Other clients need to be approved by the manager to join in
        boolean access = true;
        try {
            access = client.needAccess(client.getName());
        } catch (Exception e) {
            System.out.println("Unable to get access to the canvas!");
        }

        if (access) {
            this.manager.addClient(client);
            syncClientList();
            try {
                syncChatHistory(client);
            } catch (IOException e) {
                System.out.println("Error syncing chat history!");
            }
        } else {
            try {
                client.setAccess(false);
            } catch (Exception e) {
                System.out.println("Unable to set access!");
            }
        }
    }

    @Override
    public boolean validUsername(String username) throws RemoteException {
        boolean res = true;
        for (IClient c : getClients()) {
            if (username.equals(c.getName()) || c.getName().equals("(Host) " + username)) {
                res = false;
            }
        }
        return res;
    }

    @Override
    public Set<IClient> getClients() throws RemoteException {
        return this.manager.getClientList();
    }

    @Override
    public void syncClientList() throws RemoteException {
        for (IClient c: this.manager.getClientList()) {
            c.syncClientList(this.manager.getClientList());
        }
    }

    @Override
    public void quitClient(String name) throws RemoteException {
        for (IClient c: this.manager.getClientList()) {
            if (c.getName().equals(name)) {
                this.manager.delClient(c);
                syncClientList();
                System.out.println(name + "has left");
                return;
            }
        }
    }

    @Override
    public void kickClient(String name) throws RemoteException {
        for (IClient c: this.manager.getClientList()) {
            if (c.getName().equals(name)) {
                try {
                    c.forceQuit();
                } catch (IOException e) {
                    System.out.println("Cannot force quit!");
                }
                this.manager.delClient(c);
                syncClientList();
                System.out.println(name + "has been kicked out");
                return;
            }
        }
    }

    @Override
    public void removeAllClients() throws IOException {
        for (IClient c: this.manager.getClientList()) {
            this.manager.delClient(c);
            c.forceQuit();
        }
        System.out.println("Manager has end the session");
    }

    @Override
    public void broadcastMsg(ICanvasMsg draw) throws RemoteException {
        for (IClient c: this.manager.getClientList()) {
            c.syncCanvas(draw);
        }
    }

    @Override
    public byte[] sendCurrentCanvas() throws IOException {
        return this.clientManager.getCurrentCanvas();
    }

    @Override
    public void sendExistCanvas(byte[] canvas) throws IOException {
        for (IClient c: this.manager.getClientList()) {
            c.overrideCanvas(canvas);
        }
    }

    @Override
    public void cleanCanvas() throws RemoteException {
        for (IClient c: this.manager.getClientList()) {
            c.cleanCanvas();
        }
    }

    @Override
    public void broadcastChat(String msg) throws RemoteException {
        for (IClient c: this.manager.getClientList()) {
            c.syncChat(msg);
        }
    }

    @Override
    public void syncChatHistory(IClient client) throws IOException {
        client.syncChatHistory(this.clientManager.getChatHistory());
    }

}
