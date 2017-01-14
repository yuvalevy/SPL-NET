package bgu.spl171.net.impl.TFTP.packets;

public class BCastPacket implements TFTPPacket {

	private String filename;
	private char added;

	/**
	 * 
	 * @param filename
	 * @param added
	 *            deleted (0) or added (1)
	 */
	public BCastPacket(String filename, char added) {
		this.filename = filename;
		this.added = added;
	}

	@Override
	public void execute() {
		// does nothing
	}

	@Override
	public TFTPPacket getNextResult() {
		return null;
	}

	@Override
	public short getOpcode() {
		return 9;
	}

	public String getFilename() {
		return filename;
	}

	public char getCreateDelete() {
		return added;
	}
}