package bgu.spl171.net.impl.TFTPtpc;

import bgu.spl171.net.impl.TFTP.TFTPEncoderDecoder;
import bgu.spl171.net.impl.TFTP.TFTPProtocol;
import bgu.spl171.net.srv.Server;

public class TPCMain {

	public static void main(String[] args) {

		// for (int i = 0; i < 100; i++) {
		//
		// File dir = new File("Files");
		// dir.mkdirs();
		// String filename = "Files/" + i + "linoylinoylinoylinoy.txt";
		// File file = new File(filename);
		//
		// try (FileOutputStream output = new FileOutputStream(filename, true))
		// {
		// file.createNewFile();
		// output.write("shor".getBytes());
		// output.flush();
		//
		// } catch (IOException e) {
		// }
		//
		// }
		Server<?> f = Server.threadPerClient(7777, () -> new TFTPProtocol(), () -> new TFTPEncoderDecoder());
		f.serve();
	}

}
