package bgu.spl171.net.srv;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.api.bidi.BidiMessagingProtocol;

public class Reactor<T> implements Server<T> {

	private final int port;
	private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
	private final Supplier<MessageEncoderDecoder<T>> readerFactory;
	private final ActorThreadPool pool;
	private Selector selector;

	private Thread selectorThread;
	private final ConcurrentLinkedQueue<Runnable> selectorTasks = new ConcurrentLinkedQueue<>();
	private ServerConnections<T> connections;

	public Reactor(int numThreads, int port, Supplier<BidiMessagingProtocol<T>> protocolFactory,
			Supplier<MessageEncoderDecoder<T>> readerFactory) {

		this.pool = new ActorThreadPool(numThreads);
		this.port = port;
		this.protocolFactory = protocolFactory;
		this.readerFactory = readerFactory;
		this.connections = new ServerConnections<T>();
	}

	void updateInterestedOps(SocketChannel chan, int ops) {
		final SelectionKey key = chan.keyFor(this.selector);
		if (Thread.currentThread() == this.selectorThread) {
			key.interestOps(ops);
		} else {
			this.selectorTasks.add(() -> {
				key.interestOps(ops);
			});
			this.selector.wakeup();
		}
	}

	@Override
	public void close() throws IOException {
		this.selector.close();
	}

	@Override
	public void serve() {
		this.selectorThread = Thread.currentThread();

		System.out.println("server is waiting for a client!!!");
		try (Selector selector = Selector.open(); ServerSocketChannel serverSock = ServerSocketChannel.open()) {

			this.selector = selector; // just to be able to close

			serverSock.bind(new InetSocketAddress(this.port));
			serverSock.configureBlocking(false);
			serverSock.register(selector, SelectionKey.OP_ACCEPT);

			while (!Thread.currentThread().isInterrupted()) {

				selector.select();
				runSelectionThreadTasks();

				for (SelectionKey key : selector.selectedKeys()) {

					if (!key.isValid()) {
						continue;
					} else if (key.isAcceptable()) {
						handleAccept(serverSock, selector);
					} else {
						handleReadWrite(key);
					}
				}

				selector.selectedKeys().clear(); // clear the selected keys set
													// so that we can know about
													// new events

			}

		} catch (ClosedSelectorException ex) {
			// do nothing - server was requested to be closed
		} catch (IOException ex) {
			// this is an error
			ex.printStackTrace();
		}

		System.out.println("server closed!!!");
		this.pool.shutdown();
	}

	private void handleAccept(ServerSocketChannel serverChan, Selector selector) throws IOException {
		SocketChannel clientChan = serverChan.accept();
		clientChan.configureBlocking(false);
		BidiMessagingProtocol<T> protocol = this.protocolFactory.get();
		int connectionId = clientChan.hashCode();

		final NonBlockingConnectionHandler<T> handler = new NonBlockingConnectionHandler<T>(this.readerFactory.get(),
				protocol, clientChan, this, this.connections, connectionId);

		protocol.start(connectionId, this.connections);
		this.connections.addConnection(handler, connectionId);

		clientChan.register(selector, SelectionKey.OP_READ, handler);
	}

	private void handleReadWrite(SelectionKey key) {
		@SuppressWarnings("unchecked")
		NonBlockingConnectionHandler<T> handler = (NonBlockingConnectionHandler<T>) key.attachment();
		if (key.isReadable()) {
			Runnable task = handler.continueRead();
			if (task != null) {
				this.pool.submit(handler, task);
			}
		}

		if (key.isWritable()) {
			handler.continueWrite();
		}
	}

	private void runSelectionThreadTasks() {
		while (!this.selectorTasks.isEmpty()) {
			this.selectorTasks.remove().run();
		}
	}

}
