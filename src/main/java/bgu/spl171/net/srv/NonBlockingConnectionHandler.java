package bgu.spl171.net.srv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.ConnectionHandler;
import bgu.spl171.net.api.bidi.Connections;

public class NonBlockingConnectionHandler<T> implements ConnectionHandler<T> {

	private static final int BUFFER_ALLOCATION_SIZE = 1 << 13; // 8k
	private static final ConcurrentLinkedQueue<ByteBuffer> BUFFER_POOL = new ConcurrentLinkedQueue<>();

	private final BidiMessagingProtocol<T> protocol;
	private final MessageEncoderDecoder<T> encdec;
	private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
	private final SocketChannel chan;
	private final Reactor<T> reactor;
	private Connections<T> connections;
	private int connectionId;

	public NonBlockingConnectionHandler(MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol,
			SocketChannel chan, Reactor<T> reactor, Connections<T> connections) {
		this.chan = chan;
		this.encdec = reader;
		this.protocol = protocol;
		this.reactor = reactor;
		this.connections = connections;
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
			this.connections.disconnect(this.connectionId);
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
							this.protocol.process(nextMessage);
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
		if (msg != null) {
			this.writeQueue.add(ByteBuffer.wrap(this.encdec.encode(msg)));
			this.reactor.updateInterestedOps(this.chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		}
	}

}
