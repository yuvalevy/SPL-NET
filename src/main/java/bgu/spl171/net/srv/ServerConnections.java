package bgu.spl171.net.srv;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.api.bidi.ConnectionHandler;
import bgu.spl171.net.api.bidi.Connections;

public class ServerConnections<T> implements Connections<T> {

	private ConcurrentHashMap<Integer, ConnectionHandler<T>> connections;

	public ServerConnections() {
		this.connections = new ConcurrentHashMap<Integer, ConnectionHandler<T>>();
	}

	/**
	 * Adds new connection in case connection with connectionId does not exists
	 *
	 * @param handler
	 * @param connectionId
	 * @return Connection was added - true, otherwise false
	 */
	public boolean addConnection(ConnectionHandler<T> handler, int connectionId) {
		return this.connections.putIfAbsent(connectionId, handler).equals(handler);
	}

	@Override
	public void broadcast(T msg) {

		for (ConnectionHandler<T> handler : this.connections.values()) {
			handler.send(msg);
		}

	}

	@Override
	public void disconnect(int connectionId) {

		this.connections.remove(connectionId);
	}

	@Override
	public boolean send(int connectionId, T msg) {

		ConnectionHandler<T> handler = this.connections.get(connectionId);
		boolean val = handler != null;

		if (val) {
			handler.send(msg);
		}

		return val;
	}

}
