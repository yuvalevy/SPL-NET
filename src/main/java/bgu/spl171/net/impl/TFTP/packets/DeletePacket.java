package bgu.spl171.net.impl.TFTP.packets;

import java.io.File;

public class DeletePacket implements TFTPPacket {

	private String filename;
	private TFTPPacket response;

	public DeletePacket(String filename) {
		this.filename = filename;
	}

	@Override
	public void execute() {
		File file = new File(filename);
		if (!file.exists()) {
			this.response = new ErrorPacket((short) 1);
		} else {
			file.delete();
			this.response = new AckPacket((short) 0);
		}
	}

	@Override
	public TFTPPacket getNextResult() {
		return response;
	}

	@Override
	public short getOpcode() {
		return 8;
	}

	public String getFilename() {
		return this.filename;
	}

}