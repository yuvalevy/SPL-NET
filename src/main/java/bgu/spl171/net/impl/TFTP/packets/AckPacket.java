package bgu.spl171.net.impl.TFTP.packets;

public class AckPacket implements TFTPPacket {

	private short blockNum;

	public AckPacket(int blockNum) {
		this.blockNum = (short) blockNum;
	}

	@Override
	public void execute() {

	}

	@Override
	public TFTPPacket getNextResult() {
		return null;
	}

	@Override
	public short getOpcode() {
		return 4;
	}

	public short getBlockNum() {
		return blockNum;
	}

}