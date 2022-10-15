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
        manager = new ClientMgr(this);
    }

    @Override
    public void login(IClient client) throws RemoteException {
        // The first client is the manager
        if (this.manager.hasNoClient()) {
            client.setAsManager();
            clientManager = client;
            client.setName("(Host) " + client.getName());
            this.manager.addClient(client);
            syncClientList();
            return;
        }

        // Other clients need to have permission
        boolean access = true;
        try {
            access = client.needAccess(client.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Update client's access
        if (!access) {
            try {
                client.setAccess(false);
            } catch (Exception e) {
                System.out.println("Unable to set access!");
            }
        } else {
            // Add client to the list
            this.manager.addClient(client);
            syncClientList();
        }
    }

    @Override
    public boolean isManager(String username) throws RemoteException {
        return this.clientManager.getName().equals("(Host) " + username);
    }

    @Override
    public Set<IClient> getClients() throws RemoteException {
        return this.manager.getClientList();
    }

    @Override
    public void syncClientList() throws RemoteException {
        for (IClient c: this.manager.getClientList()) {
            c.updateClientList(this.manager.getClientList());
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
                    System.out.println("Can not quit!");
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
        System.out.println("Manager has closed the canvas");
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
            if (!c.isManager()) {
                c.overrideCanvas(canvas);
            }
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
    public byte[] sendChatHistory() throws IOException {
        return this.clientManager.getChatHistory();
    }

}
