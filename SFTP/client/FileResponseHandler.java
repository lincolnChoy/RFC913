package SFTP.client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class FileResponseHandler {
	
	private String fileToSave = "";
	private long fileSize = -1;

	private String fileToSend = "";
	private long sendFileSize = 0;
	
	public String LIST(BufferedReader inFromServer) throws IOException {
		
		String serverResponse;
		String response = "";
		while ((serverResponse = inFromServer.readLine()) != null) { 
			response += serverResponse + "\n";
			/* Stop printing when no more files to list or if not enough arguments called with LIST*/
			if (serverResponse.equals("EOF")) {
				break;
			}
			else if (serverResponse.equals("-Invalid command")) {
				System.out.println(serverResponse);
				break;
			}
			System.out.println(serverResponse);
		}
		return response;
	}
	
	
	/* Return -1 after calling send to disable flag */
	public String SEND(BufferedReader inFromServer, Socket socket) throws IOException {
		
		String serverResponse = inFromServer.readLine();
		System.out.println(serverResponse);
		
		/* Only save file if RETR was called successfully */
		if (!serverResponse.equals("-Please call RETR first") 
			&& !serverResponse.equals("-send account/password")
			&& !serverResponse.equals("-Invalid command")
		) {
			InputStream inputStream = socket.getInputStream();
			OutputStream outputStream = new FileOutputStream(fileToSave);
			
			byte[] buffer = new byte[1];
			int read = 0;
			long progress = 0;
			int nextMilestone = 0;
			
			/* Keep receiving data until reported number of bytes received */
			while (progress < fileSize) {
				read = inputStream.read(buffer);
				outputStream.write(buffer, 0, read);
				progress++;
				
				/* Print download progress for user */
				if ((double)progress/fileSize * 100 >= nextMilestone) {
					System.out.println(nextMilestone + "%");
					if (nextMilestone >= 100) {
						System.out.println(fileToSave + " successfully downloaded!");
					}
					if (fileSize < 10) {
						nextMilestone+=50;
					}
					else {
						nextMilestone+=10;
					}
					
				}
			}
			outputStream.close();
			fileSize = -1;
		}
		else {
			fileSize = -1;
		}
		
		return serverResponse;
	}
	
	
	/* Handles the server response after calling STOR then SIZE. */
	private String SIZE(BufferedReader inFromServer, Socket socket) throws IOException {
		
		String serverResponse = inFromServer.readLine();
		System.out.println(serverResponse);
		
		/* If ok to send file */
		if (serverResponse.equals("+ok, waiting for file")) {
			try {

				InputStream inputStream = new FileInputStream(fileToSend);
				DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
				
				byte[] buffer = new byte[1];
				int progress = 0;
				int count = 0;
				int nextMilestone = 0;
				
				/* Send file and update user on upload progress */
				while ((count = inputStream.read(buffer)) > 0) {
					outToClient.write(buffer, 0, count);
					progress++;
					if ((double)progress/sendFileSize * 100 >= nextMilestone) {
						System.out.println(nextMilestone + "%");
						if (nextMilestone >= 100) {
							System.out.println(fileToSend + " successfully uploaded!");
						}
						if (sendFileSize < 10) {
							nextMilestone+=50;
						}
						else {
							nextMilestone+=10;
						}
					}
				};
				outToClient.flush();
				inputStream.close();
				
				serverResponse = inFromServer.readLine();
				System.out.println(serverResponse);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		/* Handle server side error */
		else {
			fileToSend = "";
			sendFileSize = 0;
		}
		return serverResponse;
	}
	
	
	public String RETR(BufferedReader inFromServer, String[] args) throws IOException {
		
		String serverResponse = inFromServer.readLine();
		System.out.println(serverResponse);
		
		if (serverResponse.equals("-File doesn't exist") 
			|| serverResponse.equals("-send account/password")
			|| serverResponse.equals("-Invalid command")
		) {
			fileSize = -1;
		}
		/* Prepare to save file */
		else {
			fileToSave = args[1];
			fileSize =  Long.parseLong(serverResponse);
		}
		
		return serverResponse;
	}
	
	
	public String STOR(BufferedReader inFromServer, String[] args, Socket socket) throws IOException {
		
		String serverResponse = inFromServer.readLine();
		System.out.println(serverResponse);
		
		if (args.length >= 3) {
			/* Handle different responses depending on type for STOR*/
			String type = args[1].toUpperCase();
			if (type.equals("NEW")) {
				if (serverResponse.equals("+File exists, will create new generation of file") ||
					serverResponse.equals("+File does not exist, will create new file")) {
					File file = new File(args[2]);
					sendFileSize = file.length();
					fileToSend = args[2];
					
					writeAndSendSizeToServer(Long.toString(sendFileSize), socket);
					SIZE(inFromServer, socket);
				}			
			}
			else if (type.equals("OLD")) {
				if (serverResponse.equals("+Will write over old file") ||
					serverResponse.equals("+Will create new file")) {
					File file = new File(args[2]);
					sendFileSize = file.length();
					fileToSend = args[2];
					
					writeAndSendSizeToServer(Long.toString(sendFileSize), socket);
					SIZE(inFromServer, socket);
				}
			}
			else if (type.equals("APP")) {
				if (serverResponse.equals("+Will append to file") ||
					serverResponse.equals("+Will create file")) {
					File file = new File(args[2]);
					sendFileSize = file.length();
					fileToSend = args[2];
					
					writeAndSendSizeToServer(Long.toString(sendFileSize), socket);
					SIZE(inFromServer, socket);	
				}
			}
		}
		return serverResponse;
	}
	
	
	public String STOP(BufferedReader inFromServer) throws IOException {
		
		fileSize = -1;
		fileToSave = "";
		String serverResponse = inFromServer.readLine();
		System.out.println(serverResponse);
		return serverResponse;
	}
	
	
	private void writeAndSendSizeToServer(String filesize, Socket socket) throws IOException {
		
		
		/* Write to server */
		String line = "SIZE " + filesize;
		DataOutputStream outToServer = new DataOutputStream(socket.getOutputStream());
		outToServer.writeBytes(line + '\n');
	}
	
}
