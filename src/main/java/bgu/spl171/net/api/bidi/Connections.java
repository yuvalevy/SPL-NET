package bgu.spl171.net.api.bidi;

public interface Connections<T> {

	void broadcast(T msg);

	void disconnect(int connectionId);

	boolean send(int connectionId, T msg);
}