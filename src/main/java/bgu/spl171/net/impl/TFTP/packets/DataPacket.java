package bgu.spl171.net.impl.TFTP.packets;

import java.io.FileOutputStream;
import java.io.IOException;

public class DataPacket implements TFTPPacket {

	private String filename;
	private short blockNum;
	private byte[] data;
	private short size;

	private TFTPPacket reponse;

	public DataPacket(short blockNum, byte[] data) {
		this.blockNum = blockNum;
		this.data = data;
		this.size = (short) data.length;// range between 0-512
		filename = null;
	}

	public DataPacket(short blockNum) {
		this(blockNum, new byte[0]);
	}

	@Override
	public void execute() {

		FileOutputStream output = null;
		try {
			output = new FileOutputStream(filename, true);
			output.write(data);
		} catch (IOException e) {
			this.reponse = new ErrorPacket((short) 2);
		} finally {
			try {
				output.close();
			} catch (IOException e) {
				this.reponse = new ErrorPacket((short) 2);
			}
		}

		if (this.reponse == null) {
			this.reponse = new AckPacket(blockNum);
		}

	}

	@Override
	public TFTPPacket getNextResult() {
		return this.reponse;
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

	public byte[] getData() {
		return data;
	}

}