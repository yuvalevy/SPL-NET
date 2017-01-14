package bgu.spl171.net.impl.TFTP.packets;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.impl.TFTP.TFTPProtocol.FileStatus;

public class DirListPacket implements TFTPPacket {

	private final int MAXPACKETSIZE = 512;
	private ConcurrentHashMap<String, FileStatus> files;
	private String seperator;
	private int start;
	private byte[] completedFiles;
	private Short blockNum;

	public DirListPacket() {
		this.blockNum = 1;
		this.start = 0;
		this.seperator = "/0";
	}

	@Override
	public void execute() {
		fileStringByteByStatus(FileStatus.COMPLETE);

	}

	@Override
	public TFTPPacket getNextResult() {

		DataPacket nextPacket = null;
		int length = this.completedFiles.length;

		if (start > length) {
			return null;
		} else if (start == length) {
			nextPacket = new DataPacket(blockNum);
		} else {
			nextPacket = createDataPacket(blockNum, start);
		}
		start = start + MAXPACKETSIZE;
		blockNum++;

		return nextPacket;

	}

	@Override
	public short getOpcode() {
		return 6;
	}

	private void fileStringByteByStatus(FileStatus status) {
		String filesString = "";
		for (Map.Entry<String, FileStatus> entry : files.entrySet()) {
			if (entry.getValue() == status) {
				filesString += entry.getKey() + seperator;
			}
		}
		completedFiles = filesString.getBytes();
	}

	private DataPacket createDataPacket(short blockNum, int start) {
		int dataSize = MAXPACKETSIZE;

		if (start + MAXPACKETSIZE > this.completedFiles.length) {
			dataSize = this.completedFiles.length - start;
		}

		byte[] data = Arrays.copyOfRange(this.completedFiles, start, start + dataSize);

		DataPacket dataPacket = new DataPacket(blockNum, data);

		// Complete
		return dataPacket;
	}

}