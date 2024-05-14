import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * This program calculates network configurations for IP addresses in IPv4 CIDR
 * notation.
 * 
 * @author Yiting Yao
 *
 */
public class IPCalculator {

	public static void main(String[] args) {
		// Initialize a scanner to capture user input.
		Scanner scanner = new Scanner(System.in);
		// Prompt the user to enter an IP address in CIDR format.
		//CIDR notation is expressed as an IP address followed by a slash ('/') and a number, indicating how many bits of the address are used for network parts. 
		try {// Read the full line of CIDR input from the user.
			System.out.print("Enter an IP address in CIDR notation: \n> ");
			String fullInput = scanner.nextLine(); // Read the full line input.

			if (!fullInput.contains("/")) {//Check if the input contains a '/', which is required for correct CIDR notation.
				System.err.println("CIDR notation requires a slash ('/') followed by the network bits.");
				return;
			}
			// Split the input into two parts at the '/', this expects exactly two segments: IP address and network bits.
			String[] parts = fullInput.split("/");
			if (parts.length != 2) {
				System.err.println("Please enter a valid CIDR notation (e.g., 192.168.1.1/24).");
				return;
			}
			//Validate the format of the IP part against a regex that checks for valid IPv4 addresses.
			//The regex validates for four octets, each ranging from 0 to 255, separated by dots, no leading zeros for each octet.
			String ipString = parts[0];
			if (!ipString.matches(
					"\\b(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.(?:[0-9]{1,2}|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\b")) {
				System.err.println("Invalid IP address format. Please enter a valid IP address.");
				return;
			}
			//In CIDR notation, network bits are the number of bits designated for the network segment of an IP address. 
			//Check for a valid user input for network bits.
			//Integer.parseInt is a built-in method in Java that converts a string into an integer. 
			int networkBits;
			try {// Convert the second part of the split input (network bits) from string to integer.
				networkBits = Integer.parseInt(parts[1]);
			} catch (NumberFormatException e) {//An exception that Integer.parseInt throws if the provided does not contain a parsable integer.
				System.err.println("Invalid format for network bits. Please enter a number.");
				return;
			}
			// Validate the range of network bits to ensure they are between 0 and 32.
			if (networkBits < 0 || networkBits > 32) {
				System.err.println("Invalid network bits. Please enter a value between 0 and 32.");
				return;
			}
			// Convert the string IP address into an InetAddress object for further processing.
			InetAddress ipBytes = InetAddress.getByName(ipString);
			// Convert the InetAddress object into a 32-bit integer representation of the IP address.
			int ipInt = InetAddressToInt(ipBytes);

			// Calculate the number of bits for hosts within the subnet by subtracting the network bits from 32.
			int hostBits = 32 - networkBits;
			// This operation forms a mask with the leftmost bits set to 1 (network portion) and the rightmost bits set to 0 (host portion).
			// The 0b prefix signifies that the number is in binary format. 
			int subnetMask = 0b11111111111111111111111111111111 << hostBits;

			//The wildcard mask is computed by taking the bitwise complement (inversion) of the subnet mask.
			//Where '~' is the bitwise complement operator. 
			int wildCard = ~subnetMask;
			//Subnet address is calculated by applying a bitwise AND operation between the IP address and the subnet mask, leaving the network segment of the IP.
			int subnet = ipInt & subnetMask;
			//Broadcast address is calculated by applying a bitwise OR operation between the IP address and the wildcard mask.
			int broadcast = ipInt | wildCard;
			//Calculate the range of IP addresses within the subnet that can be assigned to a device or a host, where 'hostMin' is the first and 'hostMax' is the last. 
			int hostMin = subnet + 1;
			int hostMax = broadcast - 1;
			// Determine the IP class of the given InetAddress and calculate the default number of network bits (DSM) for that class.
			char ipClass = findIpClass(ipBytes);
			// DSM (Default Subnet Mask) bits refer to the number of fixed network bits for a given IP class. 
			int dsmBits = getDSM(ipClass);
			// Calculate the difference between the total network bits provided and the default subnet mask bits to determine the number of bits borrowed for subnetting.
			int borrowedBits = networkBits - dsmBits;
			// Calculate the total number of subnets that can be created with the borrowed bits.
			int totalNumberSubnets = (int) Math.pow(2.0, borrowedBits);

			// Calculate the total number of usable host addresses per subnet. 
			//The subtraction accounts for two addresses in every subnet that cannot be assigned to individual devices: the network address (identifies the subnet itself) and broadcast address. 
			//The number of hosts per subnet must be an integer, the result from Math.pow is explicitly cast to an int to remove decimal part.
			int hostsPerSubnet = (int) Math.pow(2.0, hostBits) - 2;

			System.out.println("Output:");
			System.out.printf("IP Address: \t%s \t\t%s \n", ipBytes.getHostAddress(), Binary(ipBytes));
			System.out.printf("Subnet Mask: \t%s = %d \t%s \n", intToInetAddress(subnetMask).getHostAddress(),
					networkBits, Binary(subnetMask));
			System.out.printf("Wildcard Mask: \t%s \t\t%s \n", intToInetAddress(wildCard).getHostAddress(),
					Binary(wildCard));
			System.out.printf("Subnet(network address):%s/%d \t%s (Class %c) \n",
					intToInetAddress(subnet).getHostAddress(), networkBits, Binary(subnet), ipClass);
			System.out.printf("Broadcast: \t%s \t\t%s \n", intToInetAddress(broadcast).getHostAddress(),
					Binary(broadcast));
			System.out.printf("First Host IP Address: %s \t%s \n", intToInetAddress(hostMin).getHostAddress(),
					Binary(hostMin));
			System.out.printf("Last Host IP Address: %s \t%s \n", intToInetAddress(hostMax).getHostAddress(),
					Binary(hostMax));
			System.out.printf("Number of borrowed bits =%d \n", borrowedBits);
			System.out.printf("Number of host bits =%d \n", hostBits);
			System.out.printf("Number of usable hosts addresses per subnet =%d \n", hostsPerSubnet);
			System.out.printf("Total number of subnets =%d \n", totalNumberSubnets);

		} catch (UnknownHostException e) {// Thrown by InetAddress.getByName(ipString) if the provided IP address cannot be resolved.
			System.err.println("Failed to resolve the IP address.");
		} finally {
			scanner.close();
		}

	}

	/**
	 * This method retrieves the IP address as a byte array, where each byte
	 * corresponds to one octect. Since Java does not support unsigned bytes
	 * natively, this method uses Byte.toUnsignedInt to correctly handle byte
	 * values, ensuring they are interpreted as positive integers between 0 and 255.
	 * 
	 * 
	 * @param ip: The InetAddress object representing the IP address.
	 * @return The 32-bit interger representation of the provided IP address.
	 */
	private static int InetAddressToInt(InetAddress ip) {
		// Java does not natively support unsigned bytes, which range from 0 to 255.
		// Byte refers to the Byte class from the java.lang package and accesses the method statically which is used to convert each signed byte (which can have negative values)
		// into an equivalent positive integer. This ensures that the IP address is handled correctly
		// without misinterpretation of byte values. The OR operation ('|') combines these values into a single integer.

		// The getAddress() extracts the IP address from the InetAddress object as a byte array. 
		// Each byte represents one octet of the IP address.
		byte[] octets = ip.getAddress();

		//Initializes an integer (result) to store the final converted value of the IP address.
		//Takes the integer result of shifting the current octet (converted to an unsigned integer) left by designated bits and performs a bitwise OR with the current value of result.
		//The process accumulates each octet, starting from the most significant (leftmost) to the least significant (rightmost).
		int result = 0;
		result |= Byte.toUnsignedInt(octets[0]) << 24; //Position the first octet in the highest byte.
		result |= Byte.toUnsignedInt(octets[1]) << 16; //Postion the second octet in the second highest byte.
		result |= Byte.toUnsignedInt(octets[2]) << 8; //Postion the third octet in the third byte.
		result |= Byte.toUnsignedInt(octets[3]) << 0; //Position the fourth octet in the lowest byte.

		// Return the fully compiled 32-bit integer, a decimal representation of the binary combination of the four octets of the IP address.
		return result;
	}

	/**
	 * This method converts a 32-bit integer representation of an IP address back
	 * into an InetAddress object.
	 * 
	 * @param ip: The IP address represented as a 32-bit integer.
	 * @return InetAddress: The 'InetAddress' object represents the IP address in
	 *         standard human-readable IP address format.
	 */
	private static InetAddress intToInetAddress(int ip) {
		// Define a mask to perform AND operation.
		// In the AND operation each bit is compared: if both corresponding bits are 1, the result is 1; otherwise, it's 0.
		// The bitwise operations are able to be performed without explicitly converting to binary because the integers
		// inherently exists in binary form in computer's memory.
		final int mask = 0b00000000000000000000000011111111;
		// Shift the IP address right by 24 bits and apply the mask to extract the first octet.
		int oct1 = (ip >> 24) & mask;
		// Shift the IP address right by 16 bits and apply the mask to extract the second octet.
		int oct2 = (ip >> 16) & mask;
		// Shift the IP address right by 8 bits and apply the mask to extract the third octet.
		int oct3 = (ip >> 8) & mask;
		// Apply the mask directly to extract the fourth octet without shifting.
		int oct4 = (ip >> 0) & mask;
		// Create an array of bytes from the extracted octets. Cast each integer to a byte.
		byte[] octets = { (byte) oct1, (byte) oct2, (byte) oct3, (byte) oct4 };

		try {// The InetAddress.getByAddress(octets) function takes byte array and attempts to construct an InetAddress object from it. 
				//If the byte array forms a valid IP address, the function returns an InetAddress object for it. 
				//If not, it throws an UnknownHostException.
			return InetAddress.getByAddress(octets);

		} catch (UnknownHostException e) {//In the case when the byte array doesn't represent a valid IP address format, which will not be accepted by the network layer.
			System.err.println("Failed to convert integer to InetAddress: " + e.getMessage());

		}

		return null;
	}

	/**
	 * This method converts an InetAddress object into a binary string
	 * representation of its IP address. The IP address is retrieved as a byte array
	 * from the InetAddress object, formatted each byte into an 8-bit binary string,
	 * and concatenated with dots for readability. It is called by the Binary method
	 * for integer IP.
	 * 
	 * @param ip: The InetAddress object representing the IP address to be
	 *            converted.
	 * @return result: String representation of the IP address in dotted binary
	 *         format divided in octets.
	 */
	private static String Binary(InetAddress ip) {
		// Extracts the IP address as an array of bytes.
		byte[] octets = ip.getAddress();
		// Converts each byte to a binary string padded with zeros to ensure it is 8 bits long.
		String result = String.format("%s.%s.%s.%s", getByteBinaryPadded(octets[0]), getByteBinaryPadded(octets[1]),
				getByteBinaryPadded(octets[2]), getByteBinaryPadded(octets[3]));
		// Returns the formatted binary string.
		return result;
	}

	/**
	 * This method serves as a wrapper that first converts the integer IP to an
	 * InetAddress object, then utilizes the `Binary(InetAddress ip)` method to
	 * convert this object into a binary string. The conversion to InetAddress is
	 * necessary to correctly format and segment the IP address into binary octets.
	 * 
	 * @param ip: The IP address represented as a 32-bit integer.
	 * @return The binary string representation of the IP address, formatted with
	 *         each octet separated by dots.
	 */
	private static String Binary(int ip) {

		// Convert the integer IP to an InetAddress object. 
		InetAddress ipInet = intToInetAddress(ip);
		// Calls the `Binary(InetAddress ip)` method to proceed with formatting.
		return Binary(ipInet);
	}

	/**
	 * This method determines the class of an IP address based on its first octet.
	 * Private IPv4 addresses are categorized into one of five classes (A, B, C, D,
	 * E).
	 * 
	 * @param ip: The InetAddress object from which the IP class is determined.
	 * @return Char representing the class of the IP address (A, B, C, D, E).
	 * 
	 */
	private static char findIpClass(InetAddress ip) {
		// Retrieves the IP address as a byte array from the InetAddress object.
		byte[] octets = ip.getAddress();
		// Convert the first byte of the IP address to an unsigned integer. 
		//This conversion is necessary because Java bytes are signed and can hold negative values, which are not valid for IP address octets. 
		//Using Byte.toUnsignedInt ensures all byte values are interpreted correctly as 0-255.
		int firstOctet = Byte.toUnsignedInt(octets[0]);

		// Checks the range of the first octet to determine the IP classification.
		if (firstOctet <= 127)// Class A: 0-127
			return 'A';

		if (firstOctet <= 191)// Class B: 128-191
			return 'B';

		if (firstOctet <= 223)// Class C: 192-223
			return 'C';

		if (firstOctet <= 239)// Class D: 224-239 (Multicasting)
			return 'D';

		return 'E';// Class E: 240-255, used for special purposes and not typically used in general networking.
	}

	/**
	 * This method returns the default subnet mask (DSM) bits based on the class of
	 * the IP address.
	 * 
	 * @param ipClass: The character indicating the class of the IP (A, B, C, D, E).
	 * @return The number of bits in the network portion of the subnet mask for the
	 *         given class, or 0 for classes D and E which are not typically used
	 *         for standard subnetting.
	 */
	private static int getDSM(char ipClass) {

		switch (ipClass) {//Takes an uppercase or lowercase character ipClass as input and returns an integer.
		// If the class is A
		case 'A':
		case 'a':
			return 8; //Class A addresses are designated with 8 network bits.

		// If class is B
		case 'B':
		case 'b':
			return 16; //Class B addresses are designated with 16 network bits.

		// If class is C
		case 'C':
		case 'c':
			return 24;//Class C addresses are designated with 24 network bits. 
		}

		// Else return 0
		return 0;//Classes D and E do not have standard DSM bits.
	}

	/**
	 * This method converts a byte to a padded binary string representation, ensures
	 * that each byte is represented as an 8-bit binary string.
	 * 
	 * @param b: The byte to be converted.
	 * @return A string representation of the binary form of the byte, padded with
	 *         zeros to ensure it has 8 digits.
	 */
	private static String getByteBinaryPadded(byte b) {
		// Convert the signed byte to an unsigned integer. This is necessary because Java bytes are signed, and negative values would lead to incorrect binary representations.
		int byteAsInt = Byte.toUnsignedInt(b);
		// Convert the unsigned integer to a binary string. This binary representation might not include leading zeros and could be less than 8 bits.
		String binary = Integer.toBinaryString(byteAsInt);
		// Convert the binary string into an integer format to facilitate formatting in the next step. 
		int binaryInt = Integer.parseInt(binary);
		// Format the binary number as a string with exactly 8 digits, padding with leading zeros if necessary.
		String padded = String.format("%08d", binaryInt);
		// Return the consistently formatted binary string.
		return padded;
	}
}
