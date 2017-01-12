package bgu.spl171.net.impl.TFTP.packets;

public class DisconnectPacket implements TFTPPacket {

	@Override
	public void execute() {
		// TODO Auto-generated method stub

	}

	@Override
	public TFTPPacket getNextResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short getOpcode() {
		return 10;
	}

}