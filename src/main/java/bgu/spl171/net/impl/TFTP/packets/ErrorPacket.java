package bgu.spl171.net.impl.TFTP.packets;

public class ErrorPacket implements TFTPPacket {

	private short code;
	private String msg;

	public ErrorPacket(int i) {
		this.code = (short) i;
		// TODO should copy error msg?
	}

	public ErrorPacket(String msg) {
		this.msg = msg;
		this.code = 0;
	}

	@Override
	public void execute() {
	}

	@Override
	public TFTPPacket getNextResult() {
		return null;
	}

	@Override
	public short getOpcode() {
		return 5;
	}

	public short getErrorCode() {
		return code;
	}

	public String getMsg() {
		return msg;
	}

}