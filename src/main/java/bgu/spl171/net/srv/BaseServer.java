package bgu.spl171.net.srv;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.api.bidi.BidiMessagingProtocol;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

	private final int port;
	private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
	private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
	private ServerSocket sock;

	private ServerConnections<T> connections;

	public BaseServer(int port, Supplier<BidiMessagingProtocol<T>> protocolFactory,
			Supplier<MessageEncoderDecoder<T>> encdecFactory) {

		this.port = port;
		this.protocolFactory = protocolFactory;
		this.encdecFactory = encdecFactory;
		this.sock = null;
		this.connections = new ServerConnections<T>();
	}

	@Override
	public void serve() {

		try (ServerSocket serverSock = new ServerSocket(port)) {

			this.sock = serverSock; // just to be able to close

			while (!Thread.currentThread().isInterrupted()) {

				Socket clientSock = serverSock.accept();

				BidiMessagingProtocol<T> protocol = protocolFactory.get();
				BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<T>(clientSock, encdecFactory.get(),
						protocol);

				protocol.start(sock.hashCode(), connections);

				connections.addConnection(handler, handler.hashCode());
				execute(handler);
			}
		} catch (IOException ex) {
		}

		System.out.println("server closed!!!");
	}

	@Override
	public void close() throws IOException {
		if (sock != null)
			sock.close();
	}

	protected abstract void execute(BlockingConnectionHandler<T> handler);

}
