package bgu.spl171.net.impl.TFTPtpc;

import bgu.spl171.net.impl.TFTP.TFTPEncoderDecoder;
import bgu.spl171.net.impl.TFTP.TFTPProtocol;
import bgu.spl171.net.srv.Server;

public class TPCMain {

	public static void main(String[] args) {

		if (args.length != 1) {
			System.out.println("Exspecting only port number.");
			return;
		}
		int port = 0;

		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.out.println("Exspecting only port number.");
			return;
		}

		Server<?> f = Server.threadPerClient(port, () -> new TFTPProtocol(), () -> new TFTPEncoderDecoder());
		f.serve();
	}

}
