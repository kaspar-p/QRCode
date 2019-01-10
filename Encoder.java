package qrcode;

public class Encoder {
	
	// to make a Reed-Solomon code, you take the input message, then divide it by the irreducible polynomial, this quotient is the appended onto the original message
	public int[] encodeMessage(int[] inputMessage, int numSymbols) {
		
		if(inputMessage.length + numSymbols > 255) {
			System.out.println("The input was too big. It was: " + (inputMessage.length + numSymbols) + "big, and only data sizes up to 255 are supported. Exiting.");
			System.exit(0);
		}
		
		int[] generator = Calculator.generateGenerator(numSymbols);
		
		// Fill the concatted array
		int[] outputMessage = new int[inputMessage.length + generator.length - 1];
		
		for(int i = 0; i < inputMessage.length; i++) {
			outputMessage[i] = inputMessage[i];
		}
		
		for(int i = 0; i < inputMessage.length; i++) {
			int coef = outputMessage[i];
			
			if(coef != 0) {
				for (int j = 0; j < generator.length; j++) {
					outputMessage[i + j] ^= Calculator.GF_mult(generator[j], coef);
				}
			}
		}
		
		
		// Overwrite the quotient - only need the remainder
		for (int i = 0; i < inputMessage.length; i++) {
			outputMessage[i] = inputMessage[i];
		}
		
		return outputMessage;

	}
}
