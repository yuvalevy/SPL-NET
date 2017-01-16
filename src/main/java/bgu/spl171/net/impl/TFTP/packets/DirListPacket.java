package bgu.spl171.net.impl.TFTP.packets;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.impl.TFTP.TFTPProtocol.FileStatus;

public class DirListPacket implements TFTPPacket {

	private final int MAXPACKETSIZE = 512;
	private final char SEPARATOR = '\0';
	private ConcurrentHashMap<String, FileStatus> files;

	private int start;
	private byte[] completedFiles;
	private Short blockNum;

	public DirListPacket() {
		this.blockNum = 1;
		this.start = 0;
	}

	@Override
	public void execute() {
		fileStringByteByStatus(FileStatus.COMPLETE);

	}

	@Override
	public TFTPPacket getNextResult() {

		DataPacket nextPacket = null;
		int length = this.completedFiles.length;

		if (this.start > length) {
			return null;
		} else if (this.start == length) {
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
		return 6;
	}

	public void setFiles(ConcurrentHashMap<String, FileStatus> files) {
		this.files = files;
	}

	private DataPacket createDataPacket(short blockNum, int start) {
		int dataSize = this.MAXPACKETSIZE;

		if ((start + this.MAXPACKETSIZE) > this.completedFiles.length) {
			dataSize = this.completedFiles.length - start;
		}

		byte[] data = Arrays.copyOfRange(this.completedFiles, start, start + dataSize);

		DataPacket dataPacket = new DataPacket(blockNum, data);

		return dataPacket;
	}

	private void fileStringByteByStatus(FileStatus status) {

		String filesString = "";

		for (Map.Entry<String, FileStatus> entry : this.files.entrySet()) {
			if (entry.getValue() == status) {
				filesString += entry.getKey() + this.SEPARATOR;
			}
		}
		this.completedFiles = filesString.getBytes();
	}

}