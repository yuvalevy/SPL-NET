package bgu.spl171.net.impl.TFTP.packets;

public class UndefindedPacket implements TFTPPacket {

	private short opcode;

	public UndefindedPacket(short opcode) {
		this.opcode = opcode;
	}

	@Override
	public void execute() {
	}

	@Override
	public TFTPPacket getNextResult() {
		return new ErrorPacket((short) 4);
	}

	@Override
	public short getOpcode() {
		return this.opcode;
	}

}
