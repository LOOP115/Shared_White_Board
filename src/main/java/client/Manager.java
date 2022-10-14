/**
 * Class for the client manager.
 */

package client;

import server.IBoardServer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Manager{

    private Set<IClient> clientList;

    public Manager(IBoardServer server) {
        // Allow concurrent access
        this.clientList = Collections.newSetFromMap(new ConcurrentHashMap<IClient, Boolean>());
    }

    // Get list of clients
    public Set<IClient> getClientList() {
        return this.clientList;
    }

    // Add a client
    public void addClient(IClient client) {
        this.clientList.add(client);
    }

    // Delete a client
    public void delClient(IClient client) {
        this.clientList.remove(client);
    }

    // Check if there is no client
    public boolean hasNoClient() {
        return this.clientList.size() == 0;
    }

}
