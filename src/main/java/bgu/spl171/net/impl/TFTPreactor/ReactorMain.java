package bgu.spl171.net.impl.TFTPreactor;

import bgu.spl171.net.impl.TFTP.TFTPEncoderDecoder;
import bgu.spl171.net.impl.TFTP.TFTPProtocol;
import bgu.spl171.net.srv.Server;

public class ReactorMain {

	public static void main(String[] args) {

		Server<?> f = Server.reactor(1, 7777, () -> new TFTPProtocol(), () -> new TFTPEncoderDecoder());
		f.serve();

	}

}
