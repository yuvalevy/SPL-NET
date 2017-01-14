package bgu.spl171.net.impl.TFTP;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.impl.TFTP.packets.*;

enum State {
	RUTINE, WRITE, SEND, DISCONNECTED
}

public class TFTPProtocol implements BidiMessagingProtocol<TFTPPacket> {

	public enum FileStatus {
		COMPLETE, INCOMPLETE
	}

	private static ConcurrentHashMap<String, FileStatus> files;

	private static ConcurrentHashMap<String, Integer> userNames;
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
		files = new ConcurrentHashMap<>();
		userNames = new ConcurrentHashMap<>();
	}

	public Collection<Integer> getIds() {
		return userNames.values();
	}

	public boolean login(int connectionId, String userName) {

		int value = userNames.putIfAbsent(userName, connectionId);

		if (value != connectionId) { // user with this username already exists
			return false;
		}

		return true;
	}

	public void logout(String userName) {
		userNames.remove(userName);
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
				send(new ErrorPacket((short) 6));
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
			if (packet.getSize() < 512) {
				files.put(this.writePath, FileStatus.COMPLETE);
				sendBcast(this.writePath, '1');
			}

		}

	}

	private void delete(DeletePacket packet) {

		packet.execute();
		String filename = packet.getFilename();

		// only one error / ack is send
		TFTPPacket msg = packet.getNextResult();
		send(msg);

		files.remove(filename);
		sendBcast(filename, '0');
	}

	private void login(LoginPacket login) {

		if (this.state == State.DISCONNECTED) {
			send(new ErrorPacket((short) 7));
			return;
		}

		this.userName = login.getUsername();

		boolean isAdded = login(this.connectionId, this.userName);
		if (isAdded) {

			send(new AckPacket((short) 0));
			this.state = State.RUTINE;

		} else {
			send(new ErrorPacket((short) 7));
		}
	}

	private void logout() {

		logout(this.userName);
		send(new AckPacket((short) 0));

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
				send(new ErrorPacket((short) 4));
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
	private void sendBcast(String filename, char added) {

		BCastPacket bcast = new BCastPacket(filename, added);

		for (Integer id : getIds()) {
			this.connections.send(id, bcast);
		}
	}

	private void startSend(TFTPPacket packet) {

		packet.execute();
		TFTPPacket result = packet.getNextResult();
		if (result.getOpcode() != 5) {
			this.state = State.SEND;
			this.savedPacket = packet;
		}
		send(result);
	}

	private void write(WritePacket packet) {

		packet.execute();
		TFTPPacket writePacket = packet.getNextResult();
		send(writePacket);

		if (writePacket.getOpcode() != 5) {
			this.state = State.WRITE;
			this.savedPacket = packet;
			this.writePath = packet.getFilename();
			files.put(this.writePath, FileStatus.INCOMPLETE);
		}

	}
}
