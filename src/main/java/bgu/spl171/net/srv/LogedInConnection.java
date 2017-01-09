package bgu.spl171.net.srv;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class LogedInConnection {

	private ConcurrentHashMap<String, Integer> userNames;

	private static class SingletonHolder {
		private static LogedInConnection instance = new LogedInConnection();
	}

	private LogedInConnection() {
		this.userNames = new ConcurrentHashMap<String, Integer>();
	}

	public static LogedInConnection getInstance() {
		return SingletonHolder.instance;
	}

	public boolean login(int connectionId, String userName) {

		int value = userNames.putIfAbsent(userName, connectionId);

		if (value != connectionId) { // user with this username already exists
			return false;
		}

		return true;
	}

	public void logout(String userName) {
		this.userNames.remove(userName);
	}

	public Collection<Integer> getIds() {
		return this.userNames.values();
	}
}
