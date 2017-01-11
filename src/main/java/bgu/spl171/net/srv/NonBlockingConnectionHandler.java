package bgu.spl171.net.srv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.api.MessagingProtocol;
import bgu.spl171.net.api.bidi.ConnectionHandler;

public class NonBlockingConnectionHandler<T> implements ConnectionHandler<T> {

	private static final int BUFFER_ALLOCATION_SIZE = 1 << 13; // 8k
	private static final ConcurrentLinkedQueue<ByteBuffer> BUFFER_POOL = new ConcurrentLinkedQueue<>();

	// TODO: change to bidi protocol
	private final MessagingProtocol<T> protocol;
	private final MessageEncoderDecoder<T> encdec;
	private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
	private final SocketChannel chan;
	private final Reactor<T> reactor;

	public NonBlockingConnectionHandler(MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol,
			SocketChannel chan, Reactor<T> reactor) {
		this.chan = chan;
		this.encdec = reader;
		this.protocol = protocol;
		this.reactor = reactor;
	}

	private static ByteBuffer leaseBuffer() {
		ByteBuffer buff = BUFFER_POOL.poll();
		if (buff == null) {
			return ByteBuffer.allocateDirect(BUFFER_ALLOCATION_SIZE);
		}

		buff.clear();
		return buff;
	}

	private static void releaseBuffer(ByteBuffer buff) {
		BUFFER_POOL.add(buff);
	}

	@Override
	public void close() {
		try {
			this.chan.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public Runnable continueRead() {
		ByteBuffer buf = leaseBuffer();

		boolean success = false;
		try {
			success = this.chan.read(buf) != -1;
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		if (success) {
			buf.flip();
			return () -> {
				try {
					while (buf.hasRemaining()) {
						T nextMessage = this.encdec.decodeNextByte(buf.get());
						if (nextMessage != null) {
							T response = this.protocol.process(nextMessage);
							if (response != null) {
								this.writeQueue.add(ByteBuffer.wrap(this.encdec.encode(response)));
								this.reactor.updateInterestedOps(this.chan,
										SelectionKey.OP_READ | SelectionKey.OP_WRITE);
							}
						}
					}
				} finally {
					releaseBuffer(buf);
				}
			};
		} else {
			releaseBuffer(buf);
			close();
			return null;
		}

	}

	public void continueWrite() {
		while (!this.writeQueue.isEmpty()) {
			try {
				ByteBuffer top = this.writeQueue.peek();
				this.chan.write(top);
				if (top.hasRemaining()) {
					return;
				} else {
					this.writeQueue.remove();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				close();
			}
		}

		if (this.writeQueue.isEmpty()) {
			if (this.protocol.shouldTerminate()) {
				close();
			} else {
				this.reactor.updateInterestedOps(this.chan, SelectionKey.OP_READ);
			}
		}
	}

	public boolean isClosed() {
		return !this.chan.isOpen();
	}

	@Override
	public void send(T msg) {

	}

}
