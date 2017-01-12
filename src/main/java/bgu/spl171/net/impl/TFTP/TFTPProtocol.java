package bgu.spl171.net.impl.TFTP;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.impl.TFTP.packets.*;
import bgu.spl171.net.srv.LogedInConnection;

enum State {

	RUTINE, WRITE, SEND, DISCONNECTED

}

public class TFTPProtocol implements BidiMessagingProtocol<TFTPPacket> {

	private LogedInConnection activeConnections;
	private Connections<TFTPPacket> connections;
	private int connectionId;

	private boolean shouldTerminate;
	private State state;

	private String userName;
	private String writePath;
	private TFTPPacket savedPacket;

	public TFTPProtocol() {
		this.state = State.DISCONNECTED;
		this.shouldTerminate = false;
		this.activeConnections = LogedInConnection.getInstance();
	}

	@Override
	public void process(TFTPPacket packet) {

		short opcode = packet.getOpcode();

		switch (this.state) {

		case RUTINE:

			rutine(packet, opcode);
			break;

		case SEND:

			if (opcode != 4) {
				send(new ErrorPacket("Currently on READ state. Expected acknowledgment packet."));
			} else {
				continueSending();
			}
			break;

		case WRITE:

			if (opcode != 3) {
				send(new ErrorPacket("Currently on WRITE state. Expected data packet."));
			} else {
				continueWrite((DataPacket) packet);
			}
			break;

		case DISCONNECTED:

			if (opcode == 7) {
				login((LoginPacket) packet);
			} else {
				send(new ErrorPacket(6));
			}
			break;

		}
	}

	@Override
	public boolean shouldTerminate() {
		return this.shouldTerminate;
	}

	@Override
	public void start(int connectionId, Connections<TFTPPacket> connections) {
		this.connections = connections;
		this.connectionId = connectionId;
	}

	private void continueSending() {

		TFTPPacket nextResult = this.savedPacket.getNextResult();

		if (nextResult != null) {
			send(nextResult);
		} else { // exiting from SEND state
			this.state = State.RUTINE;
			this.savedPacket = null;
		}

	}

	private void continueWrite(DataPacket packet) {

		packet.setFilename(this.writePath);
		packet.execute();

		TFTPPacket nextResult = packet.getNextResult();
		send(nextResult);

		if ((nextResult.getOpcode() == 5) || (packet.getSize() < 512)) {
			this.state = State.RUTINE;
		}

	}

	private void delete(DeletePacket packet) {

		packet.execute();
		String filename = packet.getFilename();

		// only one error / ack is send
		TFTPPacket msg = packet.getNextResult();
		send(msg);

		sendBcast(filename, 0);
	}

	private void login(LoginPacket login) {

		if (this.state == State.DISCONNECTED) {
			send(new ErrorPacket(7));
			return;
		}

		this.userName = login.getUsername();

		boolean isAdded = this.activeConnections.login(this.connectionId, this.userName);
		if (isAdded) {

			send(new AckPacket(0));
			this.state = State.RUTINE;

		} else {
			send(new ErrorPacket(7));
		}
	}

	private void logout() {

		this.activeConnections.logout(this.userName);
		send(new AckPacket(0));

		this.state = State.DISCONNECTED;
		this.shouldTerminate = true;
		this.connections.disconnect(this.connectionId);

	}

	private void rutine(TFTPPacket packet, short opcode) {

		switch (opcode) {

		case 1:
			startSend(packet);
			break;

		case 2:
			write((WritePacket) packet);
			break;

		case 6:
			startSend(packet);
			break;

		case 8:
			delete((DeletePacket) packet);
			break;

		case 10:
			logout();
			break;

		default:
			if ((opcode > 10) | (opcode < 0)) {
				send(new ErrorPacket(4));
			} else {
				send(new ErrorPacket("Unexpected packet type " + opcode));
			}
			break;
		}
	}

	/**
	 * assuming packet != null
	 *
	 * @param packet
	 */
	private void send(TFTPPacket packet) {
		this.connections.send(this.connectionId, packet);
	}

	/**
	 *
	 * @param filename
	 * @param added
	 *            deleted (0) or added (1)
	 */
	private void sendBcast(String filename, int added) {

		BCastPacket bcast = new BCastPacket(filename, added);

		for (Integer id : this.activeConnections.getIds()) {
			this.connections.send(id, bcast);
		}
	}

	private void startSend(TFTPPacket packet) {

		packet.execute();

		this.state = State.SEND;
		this.savedPacket = packet;

		continueSending();
	}

	private void write(WritePacket packet) {

		packet.execute();

		this.state = State.WRITE;
		this.savedPacket = packet;
		this.userName = packet.getFilename();

	}
}
