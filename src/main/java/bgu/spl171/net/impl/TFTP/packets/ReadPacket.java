package bgu.spl171.net.impl.TFTP.packets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.impl.TFTP.FileStatus;

public class ReadPacket implements TFTPPacket {

	private byte[] datas;
	private String fileName;
	private TFTPPacket error;
	private short blockNum;
	private int start;
	private final int MAXPACKETSIZE = 512;
	private ConcurrentHashMap<String, FileStatus> files;

	public ReadPacket(String fileName) {
		this.fileName = fileName;
		this.blockNum = 1;
		this.start = 0;
		this.error = null;
	}

	@Override
	public void execute() {

		FileStatus fileStatus = this.files.get(this.fileName);
		if ((fileStatus == null) | (fileStatus == FileStatus.INCOMPLETE)) {
			this.error = new ErrorPacket((short) 1);
			return;
		}

		Path path = Paths.get("Files/" + this.fileName);

		try {
			this.datas = Files.readAllBytes(path);
		} catch (IOException e) {
			this.error = new ErrorPacket((short) 2);
		}
	}

	@Override
	public TFTPPacket getNextResult() {

		if (this.error != null) {
			return this.error;
		}

		DataPacket nextPacket = null;

		if (this.start > this.datas.length) {
			return null;
		} else if (this.start == this.datas.length) {
			nextPacket = new DataPacket(this.blockNum);
		} else {
			nextPacket = createDataPacket(this.blockNum, this.start);
		}

		this.start = this.start + this.MAXPACKETSIZE;
		this.blockNum++;

		return nextPacket;
	}

	@Override
	public short getOpcode() {
		return 1;
	}

	public void setFiles(ConcurrentHashMap<String, FileStatus> files) {
		this.files = files;
	}

	private DataPacket createDataPacket(short blockNum, int start) {

		int dataSize = this.MAXPACKETSIZE;

		if ((start + this.MAXPACKETSIZE) > this.datas.length) {
			dataSize = this.datas.length - start;
		}

		byte[] data = Arrays.copyOfRange(this.datas, start, start + dataSize);

		DataPacket dataPacket = new DataPacket(blockNum, data);

		// Complete
		return dataPacket;
	}

}