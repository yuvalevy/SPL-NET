package bgu.spl171.net.impl.TFTP.packets;

public class DisconnectPacket implements TFTPPacket {

	@Override
	public void execute() {
		// does nothing
	}

	@Override
	public TFTPPacket getNextResult() {
		return null;
	}

	@Override
	public short getOpcode() {
		return 10;
	}

}