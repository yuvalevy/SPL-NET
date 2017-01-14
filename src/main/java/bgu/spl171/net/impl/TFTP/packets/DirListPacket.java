package bgu.spl171.net.impl.TFTP.packets;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.impl.TFTP.TFTPProtocol.FileStatus;

public class DirListPacket implements TFTPPacket {

	private final int MAXPACKETSIZE = 512;
	private ConcurrentHashMap<String, FileStatus> files;
	private String seperator;
	private int start;
	private String completedFiles;
	private Short blockNum;

	public DirListPacket() {
		this.completedFiles = "";
		this.blockNum = 1;
		this.start = 0;
		this.seperator = "/0";
	}

	@Override
	public void execute() {
		fileStringByStatus(FileStatus.COMPLETE);

	}

	@Override
	public TFTPPacket getNextResult() {
		if (start > this.completedFiles.length()) {
			return null;
		}
		DataPacket nextPacket = null;
		nextPacket = createDataPacket(blockNum, start);
		start = start + MAXPACKETSIZE;
		blockNum++;

		return nextPacket;

	}

	@Override
	public short getOpcode() {
		return 6;
	}

	private void fileStringByStatus(FileStatus status) {
		for (Map.Entry<String, FileStatus> entry : files.entrySet()) {
			if (entry.getValue() == status) {
				completedFiles += entry.getKey() + seperator;
			}
		}
	}

	private DataPacket createDataPacket(short blockNum, int start) {

		int dataSize = MAXPACKETSIZE;
		int length = this.completedFiles.length();

		if (start + MAXPACKETSIZE > length) {
			dataSize = length - start;
		}

		byte[] data = completedFiles.substring(start, dataSize).getBytes();

		DataPacket dataPacket = new DataPacket(blockNum, data);

		// Complete
		return dataPacket;
	}

}