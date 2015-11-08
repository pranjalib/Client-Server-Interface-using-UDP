package proj;

import java.io.*; //for reading files
import java.net.*; //networking classes
import java.util.*;

public class FileServer {

	public static final String newLine = "\r\n";
	public static final String requestPattern = "^ENTS/[0-9]+.[0-9]+ Request\r\n[A-Za-z]([A-Za-z0-9]|_)*.[A-Za-z0-9]+\r\n[0-9]+\r\n$";

	// path to files
	public static String fileLocation = "c:/users/sai/Desktop/";

	public static void main(String[] args) throws Exception {

		final int serverPortNum = 5111; // port for sending response and
										// receiving request
										// should be same as server

		// creating UDP packet
		byte[] receivedBytes = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receivedBytes,
				receivedBytes.length);
				
		// socket for sending and receiving messages
		// on port number serPortNum
		DatagramSocket socket1 = new DatagramSocket(serverPortNum);

		while (true) { // loop for receiving requests
			System.out.println("Waiting for a message from Client");
			// receive request from client
			socket1.receive(receivePacket);
			// get sender's address
			InetAddress clientAddress = receivePacket.getAddress();
			// get sender's port number
			int clientPort = receivePacket.getPort();
			// get length of received packet
			int dataLength = receivePacket.getLength();

			System.out.print("\nMessage received from client.");
			// create request string from received bytes
			String request = new String(receivedBytes, 0, dataLength);
			// create response string from request message
			String response = GetResponseString(request);
			// getting bytes from response

			byte[] sendBytes = response.getBytes();

			// creating UDP packet for send packet
			DatagramPacket sendPacket = new DatagramPacket(sendBytes,
					sendBytes.length, clientAddress, clientPort);
			
			// send UDP packet
			socket1.send(sendPacket);

			System.out.print("\nSending response.");

			receivePacket.setLength(receivedBytes.length);

		}
		// the socket is not closed as the program runs forever
	}

	// method for generating response from a request
	public static String GetResponseString(String request) {
		String response = "ENTS/1.0 Response" + newLine; // creating response
															// string header
		String status = "0";
		String content = "";

		if (!CheckIntegrity(request)) { // set status as 1 if integrity check
										// fails
			status = "1";

		}
		if(status=="0")
		{
		if (!IsWellFormed(request)) { // set status as 2 if request is not well
										// formed
			status = "2";
			System.out.println("Invalid Request!");
		}
		}

		if (status == "0") { // split request
			String[] parts = request.split(newLine);

			String c = parts[0].substring(5, 8); // get version number

			if (!c.equals("1.0")) { // set status as 4 if incorrect version
				status = "4";

			}

			System.out.println("File: " + parts[1]);

			if (status == "0") { // try reading file
				try {
					content = readFile(parts[1]);

				} catch (Exception e) {

					status = "3"; // set status as 3 if error in reading file

				}
			}
		}
		response = response + status + newLine; // append status to response
		// append content length and content to response
		response = response + content.length() + content + newLine;
		// append checksum to response
		String checksum = GetCheckSum(response);

		response = response + checksum + newLine;
		//System.out.println(CheckIntegrity(response));
		// System.out.println(response);
		return response;
	}

	// method for reading file
	public static String readFile(String File) throws Exception {
		String content = "";

		BufferedReader FileReader = new BufferedReader(new FileReader(
				fileLocation + File));

		String line = null;
		while ((line = FileReader.readLine()) != null) {
			content = content + newLine + line;
		}

		return content;
	}

	// method for checking if the request if well formed
	public static boolean IsWellFormed(String request) {

		return request.matches(requestPattern);
	}

	// method for Integrity check
	public static boolean CheckIntegrity(String message) {

		String message1 = message.substring(0, message.length() - 2);

		int lastIndex = message1.lastIndexOf(newLine);

		String message2 = message1.substring(0, lastIndex + 2); // message
																// without
																// checksum
		String calculatedCheckSum = GetCheckSum(message2); // get checksum for
															// received message
		// get received checksum
		String receivedcheckSum = message1.substring(lastIndex + 2,
				message1.length());
 
		return receivedcheckSum.equals(calculatedCheckSum); // compare the two
															// checksum
	}

	// method for getting checksum given string message
	public static String GetCheckSum(String message) {

		byte[] bytes = new byte[message.length()]; // convert message into ASCII
													// values

		for (int i = 0; i < message.length(); i++) {
			bytes[i] = (byte) message.charAt(i);

		}

		return GetCheckSum(bytes);
	}

	// method for getting checksum given array of ASCII values
	public static String GetCheckSum(byte[] b) {
		// combine two bytes into one short

		int length = b.length / 2;
		length += b.length % 2;

		short[] combinedArray = new short[length];

		for (int i = 0; i < b.length / 2; i = i + 2) {

			// setting first 8 bits from even indexed character
			// and next 8 bits from following even indexed character
			// convert 8 bits ASCII codes to 16 bit numbers
			// left shift the first one by 8 places
			// and OR both to obtain the combined 16 bit number
			combinedArray[i] = (short) ((((short) b[i]) << 8) | ((short) b[i + 1]));

		}

		if (b.length % 2 == 1) {
			combinedArray[length - 1] = (short) (((short) b[b.length - 1] << 8) | ((short) 0));

		}
		// get checksum for generated array
		return GetCheckSum(combinedArray);
	}

	// method for getting checksum given array of short
	public static String GetCheckSum(short[] arr) {
		// calculating checksum using the given algorithm
		int s = 0;
		long c = 7919;
		long d = 65536;
		for (int i = 0; i < arr.length; i++) {
			int index = (s ^ arr[i]);
			// typecast to long to overcome integer overflow
			s = (int) ((c * (long) index) % d);

		}
		return Integer.toString(s);
	}
}
