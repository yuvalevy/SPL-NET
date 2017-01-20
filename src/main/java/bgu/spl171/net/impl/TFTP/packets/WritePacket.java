package bgu.spl171.net.impl.TFTP.packets;

import java.io.File;
import java.io.IOException;

public class WritePacket implements TFTPPacket {

	private String fileName;
	private TFTPPacket response;

	public WritePacket(String fileName) {
		this.fileName = fileName;
	}

	public void delete() {

		File file = new File("Files/" + this.fileName);
		file.delete();
	}

	@Override
	public void execute() {

		File dir = new File("Files");
		dir.mkdirs();
		String string = "Files/" + this.fileName;
		System.out.println("file name: " + string);
		File file = new File(string);

		try {

			if (file.createNewFile()) {
				this.response = new AckPacket((short) 0);
			} else {
				this.response = new ErrorPacket((short) 5); // file already
															// exists
			}

		} catch (IOException e) {
			this.response = new ErrorPacket((short) 2);
		}
	}

	public String getFilename() {
		return this.fileName;
	}

	@Override
	public TFTPPacket getNextResult() {
		return this.response;
	}

	@Override
	public short getOpcode() {
		return 2;
	}

}