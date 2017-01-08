package bgu.spl171.net.srv;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.api.bidi.ConnectionHandler;

public class PasswordlessRegistrationServer<T> {

	private ServerConnections<T> connections;
	private Server<T> server;
	private ConcurrentHashMap<String, Integer> users;
	private int count;

	public PasswordlessRegistrationServer(Server<T> server, ServerConnections<T> connections) {

		this.count = 0;
		this.server = server;
		this.connections = connections;
		this.users = new ConcurrentHashMap<String, Integer>();
	}

	public boolean registerUser(String username) {

		if (users.containsKey(username)) {
			return false;
		}

		ConnectionHandler<T> handler = null;
		connections.addConnection(handler, count);
		count++;

		return true;
	}

}
