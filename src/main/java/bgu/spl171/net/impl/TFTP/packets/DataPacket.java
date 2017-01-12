package bgu.spl171.net.impl.TFTP.packets;

import java.io.FileOutputStream;
import java.io.IOException;

public class DataPacket implements TFTPPacket {

	private String filename;
	private short blockNum;
	private byte[] data;
	private int size;

	public DataPacket(short blockNum, byte[] data) {
		this.blockNum = blockNum;
		this.data = data;
		this.size = data.length;
		filename = null;
	}

	@Override
	public void execute() {

		FileOutputStream output = null;
		try {
			output = new FileOutputStream(filename, true);
			output.write(data);
		} catch (IOException e) {
			// TODO: Send Error
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				// TODO: Send Error
			}
		}
		// TODO: Send Ack with this.blockNum
	}

	@Override
	public TFTPPacket getNextResult() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	@Override
	public short getOpcode() {
		return 3;
	}

	public short getBlockNum() {
		return blockNum;
	}

	public int getSize() {
		return this.size;
	}
}