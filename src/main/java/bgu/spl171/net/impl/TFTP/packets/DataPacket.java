package bgu.spl171.net.impl.TFTP.packets;

import java.io.FileOutputStream;
import java.io.IOException;

public class DataPacket implements TFTPPacket {

	private String filename;
	private short blockNum;
	private byte[] data;

	private TFTPPacket reponse;

	public DataPacket(short blockNum) {
		this(blockNum, new byte[0]);
	}

	public DataPacket(short blockNum, byte[] data) {
		this.blockNum = blockNum;
		this.data = data;
		this.filename = null;
	}

	@Override
	public void execute() {

		try (FileOutputStream output = new FileOutputStream(this.filename, true)) {
			output.write(this.data);
			output.flush();

		} catch (IOException e) {
			this.reponse = new ErrorPacket((short) 2);
		}

		if (this.reponse == null) { // if note error
			this.reponse = new AckPacket(this.blockNum);
		}

	}

	public short getBlockNum() {
		return this.blockNum;
	}

	public byte[] getData() {
		return this.data;
	}

	@Override
	public TFTPPacket getNextResult() {
		return this.reponse;
	}

	@Override
	public short getOpcode() {
		return 3;
	}

	public short getSize() {
		return (short) this.data.length;
	}

	public void setFilename(String filename) {
		this.filename = "Files/" + filename;
	}

}