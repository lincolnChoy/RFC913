package SFTP.client;

import java.net.*;

public class Test {
	
	public static void main(String[] args) throws Exception {
		
		/* Boolean to test from Eclipse */
		boolean runFromCMD = true;
		ClientTester client = new ClientTester(new Socket("localhost", 3000), runFromCMD, args);
		client.start();
	}
}
