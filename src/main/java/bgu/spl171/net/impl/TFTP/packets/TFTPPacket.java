package bgu.spl171.net.impl.TFTP.packets;

public interface TFTPPacket {

	void execute();

	TFTPPacket getNextResult();

	short getOpcode();
}