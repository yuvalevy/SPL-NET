package bgu.spl171.net.srv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.ConnectionHandler;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

	private final BidiMessagingProtocol<T> protocol;
	private final MessageEncoderDecoder<T> encdec;
	private final Socket sock;
	private BufferedInputStream in;
	private BufferedOutputStream out;
	private volatile boolean connected = true;

	public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol) {
		this.sock = sock;
		this.encdec = reader;
		this.protocol = protocol;
	}

	@Override
	public void close() throws IOException {
		this.connected = false;
		this.sock.close();
	}

	@Override
	public void run() {

		try (Socket sock = this.sock) { // just for automatic closing
			int read;

			this.in = new BufferedInputStream(sock.getInputStream());
			this.out = new BufferedOutputStream(sock.getOutputStream());

			while (!this.protocol.shouldTerminate() && this.connected && (read = this.in.read()) >= 0) {
				T nextMessage = this.encdec.decodeNextByte((byte) read);
				if (nextMessage != null) {
					this.protocol.process(nextMessage);
				}
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void send(T msg) {

		if (msg != null) {

			try {
				this.out.write(this.encdec.encode(msg));
				this.out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
