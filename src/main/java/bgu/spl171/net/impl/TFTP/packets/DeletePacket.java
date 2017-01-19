package bgu.spl171.net.impl.TFTP.packets;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.impl.TFTP.FileStatus;

public class DeletePacket implements TFTPPacket {

	private String filename;
	private TFTPPacket response;
	private ConcurrentHashMap<String, FileStatus> files;

	public DeletePacket(String filename) {
		this.filename = filename;
	}

	@Override
	public void execute() {

		FileStatus fileStatus = this.files.get(this.filename);
		if ((fileStatus == null) | (fileStatus == FileStatus.INCOMPLETE)) {
			this.response = new ErrorPacket((short) 1);
			return;
		}

		File file = new File(this.filename);

		synchronized (this.files) {
			if (file.delete()) {
				this.files.remove(this.filename);
				this.response = new AckPacket((short) 0);
			} else {
				this.response = new ErrorPacket((short) 2);
			}
		}

	}

	public String getFilename() {
		return this.filename;
	}

	@Override
	public TFTPPacket getNextResult() {
		return this.response;
	}

	@Override
	public short getOpcode() {
		return 8;
	}

	public void setFiles(ConcurrentHashMap<String, FileStatus> files) {
		this.files = files;
	}

}