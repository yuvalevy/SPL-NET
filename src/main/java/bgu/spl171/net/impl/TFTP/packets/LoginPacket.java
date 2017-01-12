package bgu.spl171.net.impl.TFTP.packets;

public class LoginPacket implements TFTPPacket {

	private String username;

	public LoginPacket(String username) {
		this.username = username;
	}

	@Override
	public void execute() {
	}

	@Override
	public TFTPPacket getNextResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public short getOpcode() {
		return 7;
	}

	public String getUsername() {
		return this.username;
	}
}