package bgu.spl171.net.srv;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.api.MessagingProtocol;
import bgu.spl171.net.api.bidi.ConnectionHandler;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

	private final MessagingProtocol<T> protocol;
	private final MessageEncoderDecoder<T> encdec;
	private final Socket sock;
	private BufferedInputStream in;
	private BufferedOutputStream out;
	private volatile boolean connected = true;

	public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol) {
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

			while (this.connected && ((read = this.in.read()) >= 0)) {
				T nextMessage = this.encdec.decodeNextByte((byte) read);
				if (nextMessage != null) {
					T response = this.protocol.process(nextMessage);
					if (response != null) {
						this.out.write(this.encdec.encode(response));
						this.out.flush();
					}
				}
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void send(T msg) {
		// TODO Auto-generated method stub

	}
}
