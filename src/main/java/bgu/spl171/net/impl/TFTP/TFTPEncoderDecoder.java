package bgu.spl171.net.impl.TFTP;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.impl.TFTP.packets.AckPacket;
import bgu.spl171.net.impl.TFTP.packets.BCastPacket;
import bgu.spl171.net.impl.TFTP.packets.DataPacket;
import bgu.spl171.net.impl.TFTP.packets.ErrorPacket;
import bgu.spl171.net.impl.TFTP.packets.TFTPPacket;

public class TFTPEncoderDecoder implements MessageEncoderDecoder<TFTPPacket> {

	private final byte ENDER = '\0';

	@Override
	public TFTPPacket decodeNextByte(byte nextByte) {

		return null;
	}

	@Override
	public byte[] encode(TFTPPacket message) {

		byte[] $ = null;

		short opcode = message.getOpcode();
		switch (opcode) {

		case 3: // DATA

			$ = encodeData((DataPacket) message);

			break;

		case 4: // ACK

			$ = encodeAck((AckPacket) message);

			break;

		case 5: // ERROR

			$ = encodeError((ErrorPacket) message);

			break;

		case 9: // BCAST

			$ = encodeBcast((BCastPacket) message);

			break;
		}

		return $;
	}

	private short bytesToShort(byte[] byteArr) {
		short result = (short) ((byteArr[0] & 0xff) << 8);
		result += (short) (byteArr[1] & 0xff);
		return result;
	}

	private byte[] encodeAck(AckPacket message) {

		byte[] $ = new byte[4];

		// opcode
		byte[] temp = shortToBytes(message.getOpcode());
		$[0] = temp[0];
		$[1] = temp[1];

		// block number
		temp = shortToBytes(message.getBlockNum());
		$[2] = temp[0];
		$[3] = temp[1];

		return $;
	}

	private byte[] encodeBcast(BCastPacket message) {

		byte[] filename = message.getFilename().getBytes();

		int packetSize = 6 + filename.length;
		byte[] $ = new byte[packetSize];

		// opcode
		byte[] temp = shortToBytes(message.getOpcode());
		$[0] = temp[0];
		$[1] = temp[1];

		// TODO change after pull - get char and not int
		// Deleted/Added
		int da = message.getCreateDelete();
		if (da == 0) {
			$[2] = '0';
		} else {
			$[2] = '1';
		}

		// filename
		System.arraycopy(filename, 0, $, 3, filename.length);

		// zero byte
		$[packetSize - 1] = this.ENDER;

		return $;
	}

	private byte[] encodeData(DataPacket message) {

		int packetSize = 6 + message.getSize();
		byte[] $ = new byte[packetSize];

		// opcode
		byte[] temp = shortToBytes(message.getOpcode());
		$[0] = temp[0];
		$[1] = temp[1];

		// TODO: delete casting after pull
		// packet size
		temp = shortToBytes((short) message.getSize());

		$[2] = temp[0];
		$[3] = temp[1];

		// block #
		temp = shortToBytes(message.getBlockNum());

		$[4] = temp[0];
		$[5] = temp[1];

		byte[] data = null;// TODO message.getData();
		// n bytes
		System.arraycopy(data, 0, $, 6, data.length);

		return $;

	}

	private byte[] encodeError(ErrorPacket message) {

		String errorMsg = message.getMsg();

		int packetSize = 6 + errorMsg.length();
		byte[] $ = new byte[packetSize];

		// opcode
		byte[] temp = shortToBytes(message.getOpcode());
		$[0] = temp[0];
		$[1] = temp[1];

		// error code
		temp = shortToBytes(message.getErrorCode());
		$[2] = temp[0];
		$[3] = temp[1];

		// filename
		System.arraycopy(errorMsg, 0, $, 4, errorMsg.length());

		// zero byte
		$[packetSize - 1] = this.ENDER;

		return $;
	}

	private byte[] shortToBytes(short num) {
		byte[] bytesArr = new byte[2];
		bytesArr[0] = (byte) ((num >> 8) & 0xFF);
		bytesArr[1] = (byte) (num & 0xFF);
		return bytesArr;
	}
}
