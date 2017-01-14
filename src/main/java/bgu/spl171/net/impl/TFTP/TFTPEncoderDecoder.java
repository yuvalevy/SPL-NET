package bgu.spl171.net.impl.TFTP;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.impl.TFTP.packets.*;

public class TFTPEncoderDecoder implements MessageEncoderDecoder<TFTPPacket> {

	private final byte ENDER = '\0';
	private byte[] opcArray;
	private byte[] bytes;
	private short opcode;
	private int size;
	private int opsize;
	private short datasize;

	// private PacketED
	public TFTPEncoderDecoder() {
		this.opcArray = new byte[2];
		this.bytes = new byte[1 << 10]; // start with 1k
		initCounters();
	}

	@Override
	public TFTPPacket decodeNextByte(byte nextByte) {

		// if (packetED == null){
		// PacketED = getPacket(firstByte);
		// }
		// else {
		// return PacketED.nextByte();
		// }

		// isDone = readingState.next(nextByte);
		// if (isDone) {
		// return decodePacket();
		// }
		// return null;

		TFTPPacket $ = null;
		if (this.opsize < 2) { // opcode not yet found

			this.opcArray[this.opsize] = nextByte;
			this.opsize++;

			if (this.opsize == 2) { // opcode now found

				this.opcode = bytesToShort(this.opcArray, 0);
				$ = decodeParamlessPackets();

			}

		} else if (this.opsize == 2) { // already found opcode

			$ = decodeParamsPackets(nextByte);
		}

		if ($ != null) { // if we return a packet - resets the memory
			initCounters();
		}

		return $;
	}

	/**
	 * Encoding TFTPPacket packets. Only packets {3, 4 ,5 ,9} is encoded here
	 */
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

	private short bytesToShort(byte[] byteArr, int start) {
		short result = (short) ((byteArr[start] & 0xff) << 8);
		result += (short) (byteArr[start + 1] & 0xff);
		return result;
	}

	private TFTPPacket decodeAck() {
		TFTPPacket $ = null;
		if (this.size == 2) {
			short blocknum = bytesToShort(this.bytes, 0);
			$ = new AckPacket(blocknum);
		}
		return $;
	}

	private TFTPPacket decodeData(byte nextByte) {

		TFTPPacket $ = null;
		if (this.size == 2) { // getting 'DATA size'
			this.datasize = bytesToShort(this.bytes, 0);

		} else if (this.size > 2) { // after getting the size

			// one byte is not yet inserted - checking the input without it
			// (4 bytes for data size and block#)
			if ((this.size - 3) == this.datasize) { // finished reading all
													// data
				pushByte(nextByte);
				short blocknum = bytesToShort(this.bytes, 2);
				byte[] data = null;
				System.arraycopy(this.bytes, 4, data, 0, this.bytes.length);
				$ = new DataPacket(blocknum, data);
			}
		}
		return $;
	}

	private ErrorPacket decodeError(byte nextByte) {

		ErrorPacket $ = null;

		if (nextByte == this.ENDER) {

			short code = bytesToShort(this.bytes, 0);

			byte[] stringasbytes = Arrays.copyOfRange(this.bytes, 2, this.bytes.length - 2);
			$ = new ErrorPacket(code, new String(stringasbytes));
		}
		return $;
	}

	// private TFTPPacket decodePacket() {
	// switch (this.opcode) {
	//
	// }
	// }

	/**
	 * Checks id the current opcode (this.opcode) is one of {6, 10}, and if so,
	 * returns new instance of them.
	 *
	 * @return {6, 10} packet if the current opcode is fitting, null otherwise
	 */
	private TFTPPacket decodeParamlessPackets() {

		TFTPPacket $ = null;
		switch (this.opcode) {
		case 6:
			$ = new DirListPacket();
			break;
		case 10:
			$ = new DisconnectPacket();
			break;
		}
		return $;
	}

	/**
	 * Decodes TFTPPacket packets. Only packets {1, 2, 3, 4 ,5 ,8} is encoded
	 * here
	 *
	 * @param nextByte
	 * @return TFTPPacket if the decoding process is finished. null otherwise
	 */
	private TFTPPacket decodeParamsPackets(byte nextByte) {

		TFTPPacket $ = null;

		switch (this.opcode) {

		case 1: // READ
			$ = getInstanceForStringParam(ReadPacket.class, nextByte);
			break;

		case 2: // WRITE
			$ = getInstanceForStringParam(WritePacket.class, nextByte);
			break;

		case 3: // DATA

			$ = decodeData(nextByte);

			break;

		case 4: // ACK
			$ = decodeAck();
			break;

		case 5: // ERROR
			$ = decodeError(nextByte);
			break;

		case 7:// LOGIN
			$ = getInstanceForStringParam(LoginPacket.class, nextByte);
			break;

		case 8: // DETELE
			$ = getInstanceForStringParam(DeletePacket.class, nextByte);
			break;

		default:
			break;
		}

		if ($ == null) {
			pushByte(nextByte);
		} else {
			this.size = 0;
			this.opsize = 0;
		}

		return $;
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

		// Deleted/Added
		$[2] = (byte) message.getCreateDelete();

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

		// packet size
		temp = shortToBytes(message.getSize());

		$[2] = temp[0];
		$[3] = temp[1];

		// block #
		temp = shortToBytes(message.getBlockNum());

		$[4] = temp[0];
		$[5] = temp[1];

		// n bytes
		System.arraycopy(message.getData(), 0, $, 6, message.getData().length);

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

	/**
	 * Getting a class whom first constructor is getting only String. If
	 * nextByte is ENDER, decodes the bytes to String and returns new object
	 *
	 * @param classObj
	 *            class which extends TFTPPacket to instance
	 * @param nextByte
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T extends TFTPPacket> T getInstanceForStringParam(Class<T> classObj, byte nextByte) {

		String param = "";

		if (nextByte == this.ENDER) {
			param = new String(this.bytes);

			try {
				return (T) classObj.getConstructors()[0].newInstance(param);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | SecurityException e) {
			}
		}
		return null;
	}

	/**
	 * resent all relevant counters
	 */
	private void initCounters() {
		this.opcode = -1;
		this.size = 0;
		this.opsize = 0;
	}

	private void pushByte(byte nextByte) {
		if (this.size >= this.bytes.length) {
			this.bytes = Arrays.copyOf(this.bytes, this.size * 2);
		}

		this.bytes[this.size++] = nextByte;
	}

	private byte[] shortToBytes(short num) {
		byte[] bytesArr = new byte[2];
		bytesArr[0] = (byte) ((num >> 8) & 0xFF);
		bytesArr[1] = (byte) (num & 0xFF);
		return bytesArr;
	}
}
