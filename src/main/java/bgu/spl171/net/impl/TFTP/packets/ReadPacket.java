package bgu.spl171.net.impl.TFTP.packets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class ReadPacket implements TFTPPacket {

	private byte[] datas;
	private String fileName;
	private TFTPPacket error;
	private short blockNum;
	private int start;
	private final int MAXPACKETSIZE = 512;

	public ReadPacket(String fileName) {
		this.fileName = fileName;
		this.blockNum = 1;
		this.start = 0;
		this.error = null;
	}

	@Override
	public void execute() {

		Path path = Paths.get("Files/" + fileName);
		try {
			datas = Files.readAllBytes(path);
		} catch (FileNotFoundException e) {
			this.error = new ErrorPacket((short) 1);
		} catch (IOException e) {
			this.error = new ErrorPacket((short) 2);
		}
	}

	@Override
	public TFTPPacket getNextResult() {

		if (error != null) {
			return error;
		}

		if (start > this.datas.length) {
			return null;
		}

		DataPacket nextPacket = null;
		nextPacket = createDataPacket(blockNum, start);
		start = start + MAXPACKETSIZE;
		blockNum++;

		return nextPacket;
	}

	private DataPacket createDataPacket(short blockNum, int start) {

		int dataSize = MAXPACKETSIZE;

		if (start + MAXPACKETSIZE > this.datas.length) {
			dataSize = this.datas.length - start;
		}

		byte[] data = Arrays.copyOfRange(this.datas, start, start + dataSize);

		DataPacket dataPacket = new DataPacket(blockNum, data);

		// Complete
		return dataPacket;
	}

	@Override
	public short getOpcode() {
		return 1;
	}
}