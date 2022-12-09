package aqua.blatt1.broker;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/*
 * This class is not thread-safe and hence must be used in a thread-safe way, e.g. thread confined or 
 * externally synchronized. 
 */

public class ClientCollection<T> {
	private class Client {
		final String id;
		final T client;
		private Date timestamp;

		Client(String id, T client, Date timestamp) {
			this.id = id;
			this.client = client;
			this.timestamp = timestamp;
		}

		public Date getTimestamp(){
			return timestamp;
		}
		public void setTimestamp(Date timestamp){
			this.timestamp = timestamp;
		}
		public String getId() {
			return  id;
		}
	}

	private final List<Client> clients;

	public ClientCollection() {
		clients = new ArrayList<Client>();
	}

	public ClientCollection<T> add(String id, T client, Date timestamp) {
		clients.add(new Client(id, client, timestamp));
		return this;
	}

	public ClientCollection<T> remove(int index) {
		clients.remove(index);
		return this;
	}

	public int indexOf(String id) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).id.equals(id))
				return i;
		return -1;
	}

	public int indexOf(T client) {
		for (int i = 0; i < clients.size(); i++)
			if (clients.get(i).client.equals(client))
				return i;
		return -1;
	}

	public T getClient(int index) {
		return clients.get(index).client;
	}

	public int size() {
		return clients.size();
	}

	public T getLeftNeighorOf(int index) {
		return index == 0 ? clients.get(clients.size() - 1).client : clients.get(index - 1).client;
	}

	public T getRightNeighorOf(int index) {
		return index < clients.size() - 1 ? clients.get(index + 1).client : clients.get(0).client;
	}

	public void setTimestamp(int index, Date timestamp){
		clients.get(index).setTimestamp(timestamp);
	}

	public Date getTimestamp(int index){
		return clients.get(index).getTimestamp();
	}

	public String getId(int index){
		return clients.get(index).getId();
	}

}
