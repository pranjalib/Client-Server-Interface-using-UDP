# Client-Server-Interface-using-UDP

package p1;

import java.util.*;
import java.net.*; //networking classes 
import java.io.*; //for reading files

public class FileClient {

	public static final String newLine = "\r\n";

	static int receivedFileSize = 2048; // size of the received file from the
										// server

	static Scanner input = new Scanner(System.in); // for console input

	public static void main(String[] args) throws Exception {
		boolean moreFiles = true; // boolean indicating if user wants more files
		while (moreFiles) {
			final int serverPortNum = 5111; // server port number
											// should be same as the one server
											// is listening on
			// available files for transmission
			System.out
					.println("The files available for transmission are:\n 1. scholarly_paper\n 2. directors_message\n 3. program_overview");
			// get filename from the user
			System.out.println("Enter the filename you wish to receive ");
			String file = input.nextLine();
			file = file + ".txt"; // add extension

			// get request string for a given filename
			String request = GetRequestString(file);
			byte[] requestByte = request.getBytes(); // convert request into
														// bytes

			// creating IP address object for the server machine
			InetAddress serverIp = InetAddress.getLocalHost();

			// UDP client socket for sending request and receiving response
			DatagramSocket clientSocket = new DatagramSocket();

			// creating UDP packet for sending request
			DatagramPacket sentPacket = new DatagramPacket(requestByte,
					requestByte.length, serverIp, serverPortNum);
			

			// call sendReceivePacket with packet to be sent and socket where
			// packet is to be sent
			sendReceivePacket(clientSocket, sentPacket);

			System.out.println("Do you wish to receive more files?");
			String answer = input.nextLine(); // accepting user's response to
												// receive more files
			if (answer.equals("n") || answer.equals("N")) // check if user said
															// no
			{
				moreFiles = false; // set more files as false
			}

			clientSocket.close(); // closing socket

		}

	}

	// method for sending and receiving packet
	// given socket and packet as input
	public static void sendReceivePacket(DatagramSocket clientSocket,
			DatagramPacket sentPacket) {
		
		// timeout value in milisecond
		int timeOutValue = 1000;
		// bytes for receiving file
		byte[] receivedBytes = new byte[receivedFileSize];
		// string for getting response message
		String response = "";
		// boolean indicating if transmission was successful
		boolean isSuccess = false;
		// number of attempts
		int attempt = 0;

		while (attempt < 4 && !isSuccess) { // loop until succeeded or attempts
											// are exhausted
			try {
				// sending the UDP packet to the server
				clientSocket.send(sentPacket);

				// datagram packet for receiving UDP packet
				DatagramPacket receivedPacket = new DatagramPacket(
						receivedBytes, receivedFileSize);
				// set time out value
				clientSocket.setSoTimeout(timeOutValue);
				// receiving response from serve
				clientSocket.receive(receivedPacket);
				// get response from received packet
				response = new String(receivedBytes, 0,
						receivedPacket.getLength());
				isSuccess = true; // set success as true

			} catch (InterruptedIOException e) { // catch timeout exception
				attempt++; // increasing attempts
				timeOutValue = timeOutValue * 2; // double timeout value
			} catch (IOException e) // catch any other exception
			{
				System.out.println("Error " + e);
				System.exit(1); // exit program if unknown error occurs
			}
		}

		if(isSuccess) { // process response if message was received from server
			int status = ProcessResponse(response);
			switch (status) { // take appropriate action depending on response
								// message

			case 1: // integrity check failure on server
				System.out.println("Problem occured during transmission");
				System.out
						.println("Would you like to resend the request?(y/n)");

				String usersReply = input.nextLine();
				System.out.println(usersReply);
				if (usersReply.equals("y") || usersReply.equals("Y")) {

					sendReceivePacket(clientSocket, sentPacket);
				} else {
					return;
				}
				break;

			case 5: // response message is corrupted. Resending message
				System.out
						.println("Response message is corrupted. Resending message");
				sendReceivePacket(clientSocket, sentPacket);
				break;

			default:
				return;

			}

		} else {
			System.out.println("Number of attempts exhausted");
		}

	}

	// method for getting request
	// given filename
	public static String GetRequestString(String filename) {
		String request = "ENTS/1.0 Request" + newLine + filename + newLine; // create
																			// request
																			// string
		String checkSum = GetCheckSum(request);
		// appending checksum to the request message
		request = request + checkSum + newLine;

		return request;
	}

	// method for processing response
	public static int ProcessResponse(String response) {

		String[] partsResponse = response.split(newLine); // split lines in the
															// response message
		// check integrity of the response message
		if (!CheckIntegrity(response)) {
			return 5;
		}

		String responseCode = partsResponse[1]; // get response code from
												// message

		switch (responseCode) {

		case "0": // print file if transmission is successful
			for (int i = 3; i < partsResponse.length - 1; i++) {
				System.out.println(partsResponse[i]);
			}
			System.out.println("Successful Transmission!");

			return 0;

		case "1": // integrity check fails
			System.out.println("Integrity check failure");
			return 1;

		case "2": // the syntax of request message is not correct
			System.out
					.println("The syntax of request message was not correct");
			return 2;

		case "3": // if file not found at server
			System.out.println("File not found at the server");
			return 3;

		case "4": // if the protocol version is incorrect
			System.out.println("Wrong version protocol");
			return 4;

		}
		System.out.println("Invalid status");
		return 6;

	}

	// method for Integrity check
	public static boolean CheckIntegrity(String message) {

		String message1 = message.substring(0, message.length() - 2);

		int lastIndex = message1.lastIndexOf(newLine);
		// message without checksum
		String message2 = message1.substring(0, lastIndex + 2);
		// get checksum for received message
		String calculatedCheckSum = GetCheckSum(message2);
		// get received checksum
		String receivedcheckSum = message1.substring(lastIndex + 2,
				message1.length());

		return receivedcheckSum.equals(calculatedCheckSum); // compare the two
															// checksum
	}

	// method for getting checksum given string message
	public static String GetCheckSum(String message) {

		// convert message into ASCII values
		byte[] bytes = new byte[message.length()];

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

		// allocating space for combined array
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
			// XOR s with i-th element of the combined array
			int index = (s ^ arr[i]);
			// typecast to long to overcome integer overflow
			s = (int) ((c * (long) index) % d);

		}
		return Integer.toString(s);
	}
}
