/**
 * Class for the client manager.
 */

package client;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMgr {

    private final Set<IClient> clientList;

    public ClientMgr() {
        // Allow concurrent access
        this.clientList = Collections.newSetFromMap(new ConcurrentHashMap<>());
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
