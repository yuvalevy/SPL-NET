package bgu.spl171.net.impl.TFTP.packets;

public class ErrorPacket implements TFTPPacket {

	private short code;
	private String msg;

	public ErrorPacket(short code) {
		this(code, "");
	}

	public ErrorPacket(short code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public ErrorPacket(String msg) {
		this((short) 0, msg);
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
		return 5;
	}

	// TODO:check if yuval used get code or this name
	public short getErrorCode() {
		return this.code;
	}

	public String getMsg() {
		return this.msg;
	}

}