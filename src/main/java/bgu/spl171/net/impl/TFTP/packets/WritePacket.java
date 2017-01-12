package bgu.spl171.net.impl.TFTP.packets;

public class WritePacket implements TFTPPacket {

	private String fileName;

	public WritePacket(String fileName) {
		this.fileName = fileName;
	}

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
		return 2;
	}

	public String getFilename() {
		return this.fileName;
	}

}