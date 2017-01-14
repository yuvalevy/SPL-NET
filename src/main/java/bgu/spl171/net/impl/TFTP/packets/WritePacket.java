package bgu.spl171.net.impl.TFTP.packets;

import java.io.File;
import java.io.IOException;

public class WritePacket implements TFTPPacket {

	private String fileName;
	private TFTPPacket response;

	public WritePacket(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public void execute() {
		File file = new File("Files/" + fileName);
		try {
			file.createNewFile();
			this.response = new AckPacket((short) 0);
		} catch (IOException e) {
			this.response = new ErrorPacket((short) 5);
		}
	}

	@Override
	public TFTPPacket getNextResult() {
		return this.response;
	}

	@Override
	public short getOpcode() {
		return 2;
	}

	public String getFilename() {
		return this.fileName;
	}

}