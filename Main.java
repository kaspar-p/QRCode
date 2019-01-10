package qrcode;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class Main {

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Please upload a picture of a QR code to be decompressed:");
		String testPath = "/Users/kasparpoland/Desktop/bloop.png";
		
		Decoder decoder = new Decoder();
		Encoder encoder = new Encoder();
		
		int prime = 0x11d;
		
		Calculator.initTables(prime);
		
		/*
		
		int k = inputMessage.size();
		System.out.println("k: " + k);
		System.out.println("n - k: " + (n - k));

		int[] encoded = encoder.encodeMessage(Decoder.convertList(inputMessage), 10);
		int[] encodedPrev = Decoder.copyArray(encoded);
		
		// Mess up the input a little bit
		encoded[0] = 9;
		encoded[1] = 7;
		encoded[2] = 5;
		encoded[3] = 8;
				
		int[] corrupted = Decoder.copyArray(encoded);
		
		int[] syndromes = decoder.calculateSyndromes(encoded, n - k);
		
		System.out.print("Corrupted Syndrome: ");
		for (int c : syndromes) {
			System.out.print(c + ", ");
		}
		System.out.println("");
		
		int[] errorLocatorPolynomial = decoder.calculateErrorLocatorPolynomial(syndromes, n - k, 0);
		
		int[] errorPositions = decoder.findErrors(Decoder.reverseArray(errorLocatorPolynomial), encoded.length);
		
		System.out.print("Error Positions: ");
		for (int c : errorPositions) {
			System.out.print(c + ", ");
		}
		System.out.println("");
		
		encoded = decoder.correctErasures(encoded, syndromes, errorPositions);
		
		System.out.println("If works, should be 0x96: 0x" + Integer.toHexString(encoded[10]));
		
		System.out.print("Original Message:  ");
		for (int c : encodedPrev) {
			System.out.print(c + ", ");
		}
		System.out.println("");
		
		System.out.print("Corrupted Message: ");
		for (int c : corrupted) {
			System.out.print(c + ", ");
		}
		System.out.println("");
		
		System.out.print("Repaired Message:  ");
		for (int c : encoded) {
			System.out.print(c + ", ");
		}
		System.out.println("");
		*/
		
		int n = 20;
		String inputMessage = "hello world";
		int k = inputMessage.length();
						
		System.out.println("k: " + k);
		System.out.println("n - k: " + (n - k));
	
		int[] asciiArr = new int[k];
		
		for(int i = 0; i < k; i++) {
			asciiArr[i] = (int) inputMessage.charAt(i);
		}
		
		System.out.print("ascii arr: ");
		for (int c : asciiArr) {
			System.out.print(c + ", ");
		}
		System.out.println("");
	
		int[] msgEnc = encoder.encodeMessage(asciiArr, (n - k));
		
		System.out.println("Message: " + inputMessage);
		
		// mess the data up a little bit
		msgEnc[0] = 0;
		msgEnc[1] = 2;
		msgEnc[2] = 2;
		msgEnc[3] = 2;
		msgEnc[4] = 2;
		msgEnc[5] = 2;
		
		StringBuilder corrupt = new StringBuilder();
		for (int b : msgEnc) {
			corrupt.append((char) b);
		}
		
		System.out.println("Corrupted Message: " + corrupt);
		
		int[] erasurePos = new int[] {0, 1, 2};
		int[][] correctedPair = decoder.correctInput(msgEnc, (n - k), erasurePos);
		
		StringBuilder finalresult = new StringBuilder();
		for (int i = 0; i < correctedPair[0].length; i++) {
			finalresult.append((char) correctedPair[0][i]);
		}
		
		System.out.println("final string: " + finalresult);
		
		System.out.print("Repaired Message: ");
		for (int c : correctedPair[0]) {
			System.out.print(c + ", ");
		}
		System.out.println("");
		
		/*
		
		// Show dialog box
		String imagePath = new FilePicker().chooseFile();
		// String imagePath = testPath;
		
		// As long as they chose an actual file, not cancelled		
		if (imagePath.charAt(0) != '/') {
			System.out.println("You cancelled the selection. Please restart the program to operate.");
			
			// Close the problem
			System.exit(0);
		} else {
			System.out.println("The path to the file you chose was: " + imagePath);
		}
		
		BufferedImage image = null;
		
		// read the image contents - or at least try to
		try {
			image = ImageIO.read(new File(imagePath));
			System.out.println("Successfully read image contents");
		} catch (IOException e) {
			System.out.println(e);
			System.out.println(e.getStackTrace());
		}
		
		// Give the decoder the information of the image
		decoder.fillInformation(image);
		
		System.out.println("This QR code should have a data capacity of: " + decoder.computeCapacity());
		
		// If image contents are read correctly
		 * 
		 * *
		 */
	}
}
