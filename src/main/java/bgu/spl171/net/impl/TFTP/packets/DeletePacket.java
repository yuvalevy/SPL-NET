package bgu.spl171.net.impl.TFTP.packets;

public class DeletePacket implements TFTPPacket {

	private String filename;

	public DeletePacket(String filename) {
		this.filename = filename;
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
		return 8;
	}

	public String getFilename() {
		return this.filename;
	}

}