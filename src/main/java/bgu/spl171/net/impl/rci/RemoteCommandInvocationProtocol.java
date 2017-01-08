package bgu.spl171.net.impl.rci;

import java.io.Serializable;

import bgu.spl171.net.api.MessagingProtocol;

public class RemoteCommandInvocationProtocol<T> implements MessagingProtocol<Serializable> {

	private T arg;

	public RemoteCommandInvocationProtocol(T arg) {
		this.arg = arg;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Serializable process(Serializable msg) {
		return ((Command<T>) msg).execute(this.arg);
	}

	@Override
	public boolean shouldTerminate() {
		return false;
	}

}
