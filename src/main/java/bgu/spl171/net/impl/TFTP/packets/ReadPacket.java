package bgu.spl171.net.impl.TFTP.packets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class ReadPacket implements TFTPPacket {

	private byte[] datas;

	@Override
	public void execute() {

		Path path = Paths.get("path/to/file");
		try {
			datas = Files.readAllBytes(path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public TFTPPacket getNextResult() {

		short blockNum = 1;
		for (int i = 0; i < datas.length; i += 512) {
			DataPacket nextPacket = createDataPacket(blockNum, i);
			blockNum++;
		}

		return null;
	}

	private DataPacket createDataPacket(short blockNum, int start) {

		int dataSize = 512;

		if (start + 512 >= this.datas.length) {
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