package bgu.spl171.net.impl.TFTP;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.impl.TFTP.packets.*;

public class TFTPProtocol implements BidiMessagingProtocol<TFTPPacket> {

	enum State {
		RUTINE, WRITE, SEND, DISCONNECTED
	}

	private static ConcurrentHashMap<String, FileStatus> files;

	private static ConcurrentHashMap<String, Integer> userNames = new ConcurrentHashMap<String, Integer>();
	private Connections<TFTPPacket> connections;

	private int connectionId;
	private boolean shouldTerminate;

	private State state;
	private String username;
	private String writePath;

	private short currentBlockNumber;
	private TFTPPacket savedPacket;

	public TFTPProtocol() {
		this.state = State.DISCONNECTED;
		this.shouldTerminate = false;
		files = new ConcurrentHashMap<String, FileStatus>();
	}

	public Collection<Integer> getIds() {
		return userNames.values();
	}

	@Override
	public void process(TFTPPacket packet) {

		short opcode = packet.getOpcode();

		switch (this.state) {

		case RUTINE:
			rutine(packet);
			break;

		case SEND:

			if (opcode != 4) {
				send(new ErrorPacket("Currently on READ state. Expected acknowledgment packet."));
				backToRutine();
			} else {
				continueSending((AckPacket) packet);
			}
			break;

		case WRITE:

			if (opcode != 3) {
				send(new ErrorPacket("Currently on WRITE state. Expected data packet."));
				backToRutine();
			} else {
				continueWrite((DataPacket) packet);
			}
			break;

		case DISCONNECTED:
			disconnected(packet, opcode);
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

	private void backToRutine() {
		this.state = State.RUTINE;
		this.writePath = "";
		this.savedPacket = null;
		this.currentBlockNumber = 0;
	}

	private void continueSending(AckPacket packet) {

		if (packet.getBlockNum() != this.currentBlockNumber) {
			send(new ErrorPacket("Expected " + this.currentBlockNumber + " block number and got " + packet.getBlockNum()
					+ " block number."));
			backToRutine();
		}

		TFTPPacket nextResult = this.savedPacket.getNextResult();

		if (nextResult != null) {
			send(nextResult);
		} else { // exiting from SEND state
			backToRutine();
		}

	}

	private void continueWrite(DataPacket packet) {

		packet.setFilename(this.writePath);
		packet.execute();

		TFTPPacket nextResult = packet.getNextResult();
		send(nextResult);

		if ((nextResult.getOpcode() == 5) || (packet.getSize() < 512)) {

			if (packet.getSize() < 512) {
				files.put(this.writePath, FileStatus.COMPLETE);
				sendBcast(this.writePath, '1');
			} else { // need to delete the rest written file because there was
						// an error
				((WritePacket) this.savedPacket).delete();
			}

			backToRutine();
		}

	}

	private void delete(DeletePacket packet) {

		packet.setFiles(files);
		packet.execute();
		String filename = packet.getFilename();

		// only one error / ack is send
		TFTPPacket msg = packet.getNextResult();
		send(msg);

		files.remove(filename);
		sendBcast(filename, '0');
	}

	private void disconnected(TFTPPacket packet, short opcode) {

		if (opcode == 7) {
			login((LoginPacket) packet);
		} else if (isUndefindedOpcode(opcode)) {
			send(new ErrorPacket((short) 4));
		} else {
			send(new ErrorPacket((short) 6));
		}
	}

	private boolean isUndefindedOpcode(short opcode) {
		return (opcode > 10) || (opcode < 0);
	}

	private void login(LoginPacket login) {

		if (this.state != State.DISCONNECTED) { // means user already logged in
			send(new ErrorPacket((short) 7));
			return;
		}

		this.username = login.getUsername();
		boolean tryLogin = userNames.putIfAbsent(this.username, this.connectionId) == this.connectionId;

		if (tryLogin) {
			send(new AckPacket((short) 0));
			this.state = State.RUTINE;
		} else {
			send(new ErrorPacket((short) 7));
		}
	}

	private void logout() {

		userNames.remove(this.username);
		send(new AckPacket((short) 0));

		this.state = State.DISCONNECTED;
		this.shouldTerminate = true;
	}

	private void rutine(TFTPPacket packet) {

		short opcode = packet.getOpcode();
		switch (opcode) {

		case 1:
			startSend(packet);
			break;

		case 2:
			write((WritePacket) packet);
			break;

		case 6:
			DirListPacket dir = (DirListPacket) packet;
			dir.setFiles(files);
			startSend(packet);
			break;

		case 7:
			login((LoginPacket) packet);
			break;

		case 8:
			delete((DeletePacket) packet);
			break;

		case 10:
			logout();
			break;

		default:
			if (isUndefindedOpcode(opcode)) {
				send(packet.getNextResult());
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
		this.currentBlockNumber = 1;
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
