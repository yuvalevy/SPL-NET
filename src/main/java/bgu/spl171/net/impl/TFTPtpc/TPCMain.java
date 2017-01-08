package bgu.spl171.net.impl.TFTPtpc;

import bgu.spl171.net.srv.Server;

public class TPCMain {

	public static void main(String[] args) {

		Server<?> f = Server.threadPerClient(0, null, null);
		f.serve();
	}

}
