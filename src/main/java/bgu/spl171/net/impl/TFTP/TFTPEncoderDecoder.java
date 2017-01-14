package bgu.spl171.net.impl.TFTP;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.impl.TFTP.packets.*;

public class TFTPEncoderDecoder implements MessageEncoderDecoder<TFTPPacket> {

	private class ReadingState {

		private byte[] opcArray;
		private int opsize = 0;

		private byte[] bytes;
		private short opcode;
		private int size;

		private short datasize;

		byte[] get() {
			return this.bytes;
		}

		int getOpcode() {
			return this.opcode;
		}

		boolean isWaitingForEnder() {
			return (this.opcode == 1) || (this.opcode == 2) || (this.opcode == 5) || (this.opcode == 7)
					|| (this.opcode == 8);
		}

		boolean next(byte nextByte) {

			boolean res = false;

			if (this.opsize < 2) { // opcode not yet found

				this.opcArray[this.opsize] = nextByte;
				this.opsize++;

				if (this.opsize == 2) { // opcode now found
					this.opcode = bytesToShort(this.opcArray, 0);

					if ((res = isParamlessPackets())) {
						initCounters();
					}
				}

			} else {

				if ((res = addByteByOpcode(nextByte))) {
					initCounters();
				}

			}

			return res;

		}

		private boolean addByteByOpcode(byte nextByte) {

			if (isWaitingForEnder()) { // READ, WRITE, LOGIN, DELETE

				if (isEnder(nextByte)) {
					return true;
				}

				pushByte(nextByte);
			}

			pushByte(nextByte);

			if (this.opcode == 3) { // DATA
				return isFinishedReadingData(nextByte);
			}

			if (this.opcode == 4) { // ACK
				return this.size == 2;
			}

			// problem...
			return false;
		}

		private void initCounters() {
			this.opcode = -1;
			this.size = 0;
			this.opsize = 0;
		}

		private boolean isFinishedReadingData(byte nextByte) {

			if (this.size == 2) { // getting 'DATA size'
				this.datasize = bytesToShort(this.bytes, 0);
			} else {
				// finished reading all data
				return ((this.size - 4) == this.datasize);
			}

			return false;

		}

		/**
		 * Checks id the current opcode (this.opcode) is one of {6, 10}, and if
		 * so, returns true.
		 *
		 * @return true if packet is {6, 10} false otherwise
		 */
		private boolean isParamlessPackets() {

			if ((this.opcode == 6) || (this.opcode == 10)) {
				return true;
			}

			return false;
		}

		private void pushByte(byte nextByte) {
			if (this.size >= this.bytes.length) {
				this.bytes = Arrays.copyOf(this.bytes, this.size * 2);
			}

			this.bytes[this.size++] = nextByte;
		}

	}

	private ReadingState readingState;
	private byte[] bytes;
	private final byte ENDER = '\0';

	public TFTPEncoderDecoder() {
		this.readingState = new ReadingState();
	}

	@Override
	public TFTPPacket decodeNextByte(byte nextByte) {

		boolean isDone = false;
		isDone = this.readingState.next(nextByte);
		if (isDone) {
			return decodePacket(this.readingState.get());
		}
		return null;
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
			return encodeData((DataPacket) message);
		case 4: // ACK
			return encodeAck((AckPacket) message);
		case 5: // ERROR
			return encodeError((ErrorPacket) message);
		case 9: // BCAST
			return encodeBcast((BCastPacket) message);
		}

		return $;
	}

	private short bytesToShort(byte[] byteArr, int start) {
		short result = (short) ((byteArr[start] & 0xff) << 8);
		result += (short) (byteArr[start + 1] & 0xff);
		return result;
	}

	private TFTPPacket decodeAck() {
		short blocknum = bytesToShort(this.bytes, 0);
		return new AckPacket(blocknum);
	}

	private TFTPPacket decodeData() {
		short blocknum = bytesToShort(this.bytes, 2);
		byte[] data = null;
		System.arraycopy(this.bytes, 4, data, 0, this.bytes.length);
		return new DataPacket(blocknum, data);
	}

	private ErrorPacket decodeError() {
		short code = bytesToShort(this.bytes, 0);
		byte[] stringasbytes = Arrays.copyOfRange(this.bytes, 2, this.bytes.length - 2);
		return new ErrorPacket(code, new String(stringasbytes));
	}

	private TFTPPacket decodePacket(byte[] bytes) {

		switch (this.readingState.getOpcode()) {

		case 1: // READ
			return getInstanceForStringParam(ReadPacket.class);
		case 2: // WRITE
			return getInstanceForStringParam(WritePacket.class);
		case 3: // DATA
			return decodeData();
		case 4: // ACK
			return decodeAck();
		case 5: // ERROR
			return decodeError();
		case 7:// LOGIN
			return getInstanceForStringParam(LoginPacket.class);
		case 8: // DETELE
			return getInstanceForStringParam(DeletePacket.class);
		}

		return null;
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
	 * Getting a class whom first constructor is getting only String. Then
	 * decodes the bytes to String and returns new object
	 *
	 * @param classObj
	 *            class which extends TFTPPacket to instance
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T extends TFTPPacket> T getInstanceForStringParam(Class<T> classObj) {

		String param = new String(this.bytes);

		try {
			return (T) classObj.getConstructors()[0].newInstance(param);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| SecurityException e) {
		}
		return null;
	}

	/**
	 * @param nextByte
	 * @return whether this byte is the ENDER byte
	 */
	private boolean isEnder(byte nextByte) {
		return (nextByte == this.ENDER);
	}

	private byte[] shortToBytes(short num) {
		byte[] bytesArr = new byte[2];
		bytesArr[0] = (byte) ((num >> 8) & 0xFF);
		bytesArr[1] = (byte) (num & 0xFF);
		return bytesArr;
	}

}
