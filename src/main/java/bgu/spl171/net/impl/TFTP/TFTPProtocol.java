package bgu.spl171.net.impl.TFTP;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.impl.rci.Command;
import bgu.spl171.net.srv.LogedInConnection;

public class TFTPProtocol implements BidiMessagingProtocol<Command<?>> {

	private LogedInConnection activeConnections;
	private Connections<Command<?>> connections;
	private int connectionId;
	private boolean shouldTerminate;
	private boolean isConnected;

	public TFTPProtocol() {
		this.shouldTerminate = false;
		this.isConnected = false;
		activeConnections = LogedInConnection.getInstance();
	}

	@Override
	public void process(Command<?> message) {

		// if not login packet and !isConnected
		// connections.send(connectionId, "Error - 6");

		// when login package is arraived
		String userName = "get from packet";
		boolean isAdded = activeConnections.login(connectionId, userName);
		if (isAdded) {
			// connections.send(connectionId, "Ack");
			this.isConnected = true;
		} else {
			// connections.send(connectionId, "Error - 7");
		}
	}

	@Override
	public boolean shouldTerminate() {
		return shouldTerminate;
	}

	@Override
	public void start(int connectionId, Connections<Command<?>> connections) {
		this.connections = connections;
		this.connectionId = connectionId;
	}
}
