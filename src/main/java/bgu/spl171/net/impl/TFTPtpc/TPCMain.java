package bgu.spl171.net.impl.TFTPtpc;

import bgu.spl171.net.impl.TFTP.TFTPEncoderDecoder;
import bgu.spl171.net.impl.TFTP.TFTPProtocol;
import bgu.spl171.net.srv.Server;

public class TPCMain {

	public static void main(String[] args) {

		Server<?> f = Server.threadPerClient(7777, () -> new TFTPProtocol(), () -> new TFTPEncoderDecoder());
		f.serve();
	}

}
