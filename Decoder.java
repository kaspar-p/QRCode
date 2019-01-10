package qrcode;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Decoder {
	// the four options are [numeric, alphanumeric, byte, kanji]
	// they correspond to 	[0001, 0010, 0100, 1000]
	// First 4 bits of data represent this decodingMode
	private String decodingMode;
	private String mask;
	private String errorCorrectionLevel;
	private BufferedImage image;
	private int width;
	private int height;
	private int imgWidth;
	private int imgHeight;
	private int version;
	private int pixelsPerModule;
	
	// Four blank (white) modules (pixels) surrounding the QR code need to be there
	private int margin;
	
	public Decoder() {
		this.margin = 4;
	}
	
	public void fillInformation(BufferedImage image) {
		this.image = image;
		this.imgWidth = this.image.getWidth();
		this.imgHeight = this.image.getHeight();
		
		this.width = this.getSizing();	
		System.out.println("this.width: " + this.width);
		this.version = (this.width - 17) / 4;
	}
	
	private int getSizing() {
		// Begin at the top left. Keep iterating one down and one to the right until it changes.
		// Since QR codes have a one module thin band around the alignment squares, we can use this to see how
		// Many pixels one module is
		int color = this.image.getRGB(0, 0);
		int newColor = color;
		
		int current = 0;
		while (newColor == color) {
			newColor = this.image.getRGB(current + 1, current + 1);
			current++;
		}
		
		int sizing = this.imgWidth / (current + 1);
		return sizing;
	}
	
	// Calculates the polynomial that is used to locate the erasure/errata/errors within the code
	public int[] calculateErasureLocatorPolynomial (int[] ePos) {
		// In JS: [1]
		ArrayList<Integer> eLoc = new ArrayList<Integer>();
		eLoc.add(1);
		
		for (int i : ePos) {
			// QOL variables for less verboseness
			// In JS: [1]
			ArrayList<Integer> term1_AL = new ArrayList<Integer>();
			term1_AL.add(1);
			int[] term1 = convertList(term1_AL);
			
			// In JS: [GF_pow(2, i), 0]
			ArrayList<Integer> term2_AL = new ArrayList<Integer>();
			term2_AL.add(Calculator.GF_pow(2,  i));
			term2_AL.add(0);
			int[] term2 = convertList(term2_AL);
			
			int[] addition = Calculator.polynomial_add(term1, term2);
			
			int[] product = Calculator.polynomial_mult(convertList(eLoc), addition);
			
			eLoc = convertBack(product);
		}
		
		return convertList(eLoc);
	}
	
	public int[] calculateErrorLocatorPolynomial (int[] syndromes, int numSymbols, int erasureCount) {
		System.out.println("overloaded - only 2 params call");
		
		ArrayList<Integer> errorLocatorPolynomial = new ArrayList<Integer>();
		ArrayList<Integer> oldLocatorPolynomial = new ArrayList<Integer>();
		
		errorLocatorPolynomial.add(1);
		oldLocatorPolynomial.add(1);
		
		int syndromeShift = 0;
		
		if (syndromes.length > numSymbols) {
			syndromeShift = syndromes.length - numSymbols;
		}
		
		for (int i = 0; i < numSymbols - erasureCount; i++) {
			
			int K = i + syndromeShift;
						
			int delta = syndromes[K];			
			for (int j = 1; j < errorLocatorPolynomial.size(); j++) {
				int errorLocatorIndex = errorLocatorPolynomial.size() - j - 1;
								
				delta ^= Calculator.GF_mult(errorLocatorPolynomial.get(errorLocatorIndex), syndromes[K - j]);
			}
			
			oldLocatorPolynomial.add(0);
			
			if (delta != 0) {
				
				if(oldLocatorPolynomial.size() > errorLocatorPolynomial.size()) {
					ArrayList<Integer> newLocatorPolynomial = convertBack(Calculator.polynomial_scale(convertList(oldLocatorPolynomial), delta));
					
					oldLocatorPolynomial = convertBack(Calculator.polynomial_scale(convertList(errorLocatorPolynomial), Calculator.GF_inverse(delta)));
					
					errorLocatorPolynomial = newLocatorPolynomial;
				}
				
				int[] term2 = Calculator.polynomial_scale(convertList(oldLocatorPolynomial), delta);
				errorLocatorPolynomial = convertBack(Calculator.polynomial_add(convertList(errorLocatorPolynomial), term2));
			}
		}
		
		while (errorLocatorPolynomial.size() > 0 && errorLocatorPolynomial.get(0) == 0) {
			// Create an array that is shorter than the ELP by 1
			int[] shorterPoly = new int[errorLocatorPolynomial.size() - 1];
			// Drop the leading 0s
			// Skipping the first 0, since we know it is there, fill the shorter array completely with the values of the errorLocatorPolynomial
			// This creates an array that is identical, save it is one shorter and is missing the 0 up front
			// The while loop ensures we go through every single leading 0
			for (int k = 0; k < errorLocatorPolynomial.size(); k++) {
				if (k > 0) {
					shorterPoly[k - 1] = errorLocatorPolynomial.get(k);
				}
			}
			
			errorLocatorPolynomial = convertBack(shorterPoly);
		}
		
		int errs = errorLocatorPolynomial.size() - 1;
		
		if ((errs - erasureCount) * 2 + erasureCount > numSymbols) {
			System.out.println("There are too many errors here to be able to correct! Sorry!");
			System.exit(0);
		}
		
		return convertList(errorLocatorPolynomial);	
	}
	
	// For when we do not know the locations - more common
	public int[] calculateErrorLocatorPolynomial (int[] syndromes, int numSymbols, int[] erasureLocations, int erasureCount) {
		System.out.println("whole call");
		
		ArrayList<Integer> errorLocatorPolynomial = new ArrayList<Integer>();
		ArrayList<Integer> oldLocatorPolynomial = new ArrayList<Integer>();
		
		for(int i = 0; i < erasureLocations.length; i++) {
			errorLocatorPolynomial.add(erasureLocations[i]);
			oldLocatorPolynomial.add(erasureLocations[i]);
		}
		
		int syndromeShift = 0;
		
		if (syndromes.length > numSymbols) {
			syndromeShift = syndromes.length - numSymbols;
		}
		
		for (int i = 0; i < numSymbols - erasureCount; i++) {
			
			int K = erasureCount + i + syndromeShift;
						
			int delta = syndromes[K];
			
			for (int j = 1; j < errorLocatorPolynomial.size(); j++) {
				int errorLocatorIndex = errorLocatorPolynomial.size() - j - 1;
								
				delta ^= Calculator.GF_mult(errorLocatorPolynomial.get(errorLocatorIndex), syndromes[K - j]);
			}
			
			oldLocatorPolynomial.add(0);
			
			if (delta != 0) {
				
				if(oldLocatorPolynomial.size() > errorLocatorPolynomial.size()) {
					ArrayList<Integer> newLocatorPolynomial = convertBack(Calculator.polynomial_scale(convertList(oldLocatorPolynomial), delta));
					
					oldLocatorPolynomial = convertBack(Calculator.polynomial_scale(convertList(errorLocatorPolynomial), Calculator.GF_inverse(delta)));
					
					errorLocatorPolynomial = newLocatorPolynomial;
				}
				
				int[] term2 = Calculator.polynomial_scale(convertList(oldLocatorPolynomial), delta);
				errorLocatorPolynomial = convertBack(Calculator.polynomial_add(convertList(errorLocatorPolynomial), term2));
			}
		}
		
		while (errorLocatorPolynomial.size() > 0 && errorLocatorPolynomial.get(0) == 0) {
			// Create an array that is shorter than the ELP by 1
			int[] shorterPoly = new int[errorLocatorPolynomial.size() - 1];
			// Drop the leading 0s
			// Skipping the first 0, since we know it is there, fill the shorter array completely with the values of the errorLocatorPolynomial
			// This creates an array that is identical, save it is one shorter and is missing the 0 up front
			// The while loop ensures we go through every single leading 0
			for (int k = 0; k < errorLocatorPolynomial.size(); k++) {
				if (k > 0) {
					shorterPoly[k - 1] = errorLocatorPolynomial.get(k);
				}
			}
			
			errorLocatorPolynomial = convertBack(shorterPoly);
		}
		
		int errs = errorLocatorPolynomial.size() - 1;
		
		if ((errs - erasureCount) * 2 + erasureCount > numSymbols) {
			System.out.println("There are too many errors here to be able to correct! Sorry!");
			System.exit(0);
		}
		
		return convertList(errorLocatorPolynomial);
	}
	
	// From int[] to ArrayList<Integer>
	public static ArrayList<Integer> convertBack(int[] list) {
		ArrayList<Integer> returnable = new ArrayList<Integer>();
		
		for(int c : list) {
			returnable.add(c);
		}
		
		return returnable;
	}
	
	// from ArrayList<Integer> to int[]
	public static int[] convertList(ArrayList<Integer> list) {
		Object[] objList = list.toArray();
		int[] newIntList = new int[objList.length];

		for(int i = 0; i < objList.length; i++) {
			newIntList[i] = (int) objList[i];
		}
		
		return newIntList;
	}
	
	// Errata decoder
	public int[] calculateForneySyndromes(int[] syndromes, int[] erasurePositions, int inputLength) {
		int[] reversedErasurePositions = new int[erasurePositions.length];
		for (int i = 0; i < erasurePositions.length; i++) {
			reversedErasurePositions[i] = inputLength - 1 - erasurePositions[i];
		}
		
		int[] forneySyndromes = new int[syndromes.length - 1];
		
		// Skip first coefficient, which is 0 by definition
		for(int i = 1; i < syndromes.length; i++) {
			forneySyndromes[i - 1] = syndromes[i];
		}
		
		for(int i = 0; i < erasurePositions.length; i++) {
			int x = Calculator.GF_pow(2, reversedErasurePositions[i]);
			
			for (int j = 0; j < forneySyndromes.length - 1; j++) {
				forneySyndromes[j] = (Calculator.GF_mult(forneySyndromes[j], x)) ^ (forneySyndromes[j + 1]);
			}
		}
		
		return forneySyndromes;
	}
	
	// Ties everything together and actually does it all
	public int[][] correctInput (int[] inputMessage, int numSymbols, int[] erasurePositions) {
		if (inputMessage.length > 256) {
			System.out.println("Message is too large, cannot decode... exiting...");
			System.exit(0);
		}
		
		// Copy the input array
		int[] outputMessage = copyArray(inputMessage);
		
		if (erasurePositions == null) {
			erasurePositions = new int[0];
		} else {
			for (int position : erasurePositions) {
				outputMessage[position] = 0;
			}
		}
		
		if (erasurePositions.length > numSymbols) {
			// We have surpassed the Singleton Bound and cannot decode the message
			System.out.println("Singleton bound surpassed, cannot decode: too many errors. Exiting");
			System.exit(0);
		}
		
		// Calculate the syndromes for the first time
		int[] syndromes = this.calculateSyndromes(outputMessage, numSymbols);
		
		// There were no errors
		if (maxValue(syndromes) == 0) {
			System.out.println("There were no errors in the message.");
			return this.calculateParts(outputMessage, numSymbols);
		}
		
		int[] forneySyndromes = this.calculateForneySyndromes(syndromes, erasurePositions, outputMessage.length);
		
		int erasureCount = erasurePositions.length;
		int[] errorLocatorPolynomial = this.calculateErrorLocatorPolynomial(forneySyndromes, numSymbols, erasureCount);
		
		int[] reversedELP = reverseArray(errorLocatorPolynomial);
				
		int[] errorPositions = this.findErrors(reversedELP, outputMessage.length);
		
		if (errorPositions == null) {
			System.out.println("Searching for errors failed. Exiting");
			System.exit(0);
		}
		
		// Fill an array consisting of erasurePositions and errorPositions
		int[] erasuresAndErrors = new int[erasurePositions.length + errorPositions.length];
		for(int i = 0; i < erasurePositions.length; i++) {
			erasuresAndErrors[i] = erasurePositions[i];
		}
		for (int i = erasurePositions.length; i < errorPositions.length + erasurePositions.length; i++) {
			erasuresAndErrors[i] = errorPositions[i - erasurePositions.length];
		}
		
		outputMessage = this.correctErasures(outputMessage, syndromes, erasuresAndErrors);
		
		syndromes = this.calculateSyndromes(outputMessage, numSymbols);
	
		if(maxValue(syndromes) > 0) {
			System.out.println("Could not correct errors. Exiting.");
			System.exit(0);
		}
		
		return this.calculateParts(outputMessage, numSymbols);
	}
	
	public static int maxValue (int[] list) {
		int wr = 0;
		
		for(int i = 0; i < list.length; i++) {
			if(list[i] > wr) {
				wr = list[i];
			}
		}
		
		return wr;
	}
	
	private int[][] calculateParts (int[] msg, int numSymbols) {
		// Since the remainder and quotient have the same degree, and degree == length - 1
		// We compute the index of the seperation
		int seperator = msg.length - numSymbols;
						
		ArrayList<Integer> remainder = new ArrayList<Integer>();
		ArrayList<Integer> quotient = new ArrayList<Integer>();
		
		// Fill quotient and remainder arrays
		for(int i = 0; i < seperator; i++) {
			quotient.add(msg[i]);
		}

		for(int i = seperator; i < msg.length; i++) {
			remainder.add(msg[i]);
		}
		
		// First half then second half
		int[][] finalResult = new int[][] {Decoder.convertList(quotient), Decoder.convertList(remainder)};
		return finalResult;
	}
	
	public int[] findErrors (int[] errorLocatorPolynomial, int inputLength) {
		int errs = errorLocatorPolynomial.length - 1;
		
		ArrayList<Integer> errorPositions = new ArrayList<Integer>();
		
		for (int i = 0; i < inputLength; i++) {
			
			int calc = Calculator.polynomial_eval(errorLocatorPolynomial, Calculator.GF_pow(2, i));
			if (calc == 0) {
				errorPositions.add(inputLength - 1 - i);
			}
		}
				
		if(errorPositions.size() != errs) {
			System.out.println("Too many (or too few) errors found within the psuedo-Chien search, exiting...");
			System.exit(0);
		}
		
		return convertList(errorPositions);
	}
	
	// Calculates the actual errors, not just the locations of the errors. Pretty simple
	public int[] calculateErasurePolynomial(int[] syndromes, int[] errorLocator, int numSymbols) {
		// Mathematical formula (we are looking for Omega(x)
		// Omega(x) = [ Synd(x) * Error_loc(x) ] mod x^(n-k+1)
		
		int[] product = Calculator.polynomial_mult(syndromes, errorLocator);
		
		// Modder is the addition of two polynomials, which is filling arrays in Java
		int[] modder = new int[numSymbols + 2];
		modder[0] = 1;
		
		for (int i = 1; i < modder.length; i++) {
			modder[i] = 0;
		}
		
		// Return index 1 again because we have no need for the quotient, just the remainder
		int[][] Omega = Calculator.polynomial_divide(product, modder);
		return Omega[1];
	}
	
	public static int[] reverseArray(int[] list) {
		int[] reversed = new int[list.length];
				
		for(int i = 0; i < reversed.length; i++) {
			reversed[i] = list[list.length - 1 - i];
		}
		
		return reversed;
	}
	
	public static ArrayList<Integer> reverseArray(ArrayList<Integer> list) {
		ArrayList<Integer> reversed = new ArrayList<Integer>();
		
		for (int i = 0; i < list.size(); i++) {
			reversed.add(list.get(list.size() - 1 - i));
		}
		
		return reversed;
	}
	
	public int[] correctErasures (int[] inputMessage, int[] syndromes, int[] erasurePositions) {
		// This algorithm is the Forney Algorithm - computes the actual values of the errors, AKA error magnitudes
		
		int[] coefficientPositions = new int[erasurePositions.length];
		// Fill the coefficientPositions array
		for (int i = 0; i < erasurePositions.length; i++) {
			coefficientPositions[i] = inputMessage.length - 1 - erasurePositions[i];
		}
		
		int[] erasureLocatorPolynomial = this.calculateErasureLocatorPolynomial(coefficientPositions);
		
		int[] syndromeReversal2 = reverseArray(syndromes);
		int ELPLen = erasureLocatorPolynomial.length - 1;
		
		// Create an evaluator polynomial
		int[] erasureEvaluatorPolynomial = reverseArray(this.calculateErasurePolynomial(syndromeReversal2, erasureLocatorPolynomial, ELPLen));
		
		// Chien search part 2
		ArrayList<Integer> X_AL = new ArrayList<Integer>();
		
		for(int i = 0; i < coefficientPositions.length; i++) {
			int l = 255 - coefficientPositions[i];
			X_AL.add(Calculator.GF_pow(2,  -l));
		}
		
		// Convert ArrayList to int[]
		int[] X = convertList(X_AL);
		
		// The Forney Algorithm Part
		
		// Stores the corrected error magnitudes - will be subtracted to get the corrected message later
		int[] errorMagnitudePolynomial = new int[inputMessage.length];
		
		int xLength = X.length;
		
		for (int i = 0; i < xLength; i++) {
			int Xi = X[i];
			
			int Xi_inverse = Calculator.GF_inverse(Xi);
			
			ArrayList<Integer> errorLocatorPrimeTemp = new ArrayList<Integer>();
			
			for (int j = 0; j < xLength; j++) {
				if (j != i) {
					int m = Calculator.GF_mult(Xi_inverse, X[j]);
					int sub = Calculator.GF_sub(1, m);
					
					errorLocatorPrimeTemp.add(sub);
				}
			}
			
			int errorLocatorPrime = 1;
			for (int coef : convertList(errorLocatorPrimeTemp)) {
				errorLocatorPrime = Calculator.GF_mult(errorLocatorPrime, coef);
			}
			
			int y = Calculator.polynomial_eval(reverseArray(erasureEvaluatorPolynomial), Xi_inverse);
			y = Calculator.GF_mult(Calculator.GF_pow(Xi, 1), y);
					
			// Find the magnitude
			int magnitude = Calculator.GF_div(y, errorLocatorPrime);
						
			errorMagnitudePolynomial[erasurePositions[i]] = magnitude;
		}
		
		// Add it onto the end of the original message
		inputMessage = Calculator.polynomial_add(inputMessage, errorMagnitudePolynomial);
		return inputMessage;
	}
	
	public static int[] copyArray(int[] list) {
		int[] copy = new int[list.length];
		
		for (int i = 0; i < list.length; i++) {
			copy[i] = list[i];
		}
		
		return copy;
	}
	
	// this is mathematically a fourier transform, with the inverse being a Chien Search
	public int[] calculateSyndromes(int[] inputMessage, int numSymbols) {
			int[] syndromes = new int[numSymbols];

			for (int i = 0; i < numSymbols; i++) {
				int evaluation = Calculator.polynomial_eval(inputMessage, Calculator.GF_pow(2, i));
				syndromes[i] = evaluation;
			}
			
			// We add an extra 0 for the constant at the end of every polynomial, since the algorithm goes from
			// Interval [1, n - k + 1]
			int[] adjusted = new int[syndromes.length + 1];
			adjusted[0] = 0;
			
			for (int i = 1; i < adjusted.length; i++) {
				adjusted[i] = syndromes[i - 1];
			}
			
			return adjusted;
	}
	
	// Checks if the message is damaged. i.e. if any syndrome is not 0
	@SuppressWarnings("unused")
	private boolean checkDamage(int[] inputMessage, int numSymbols) {
		
		int[] syndromes = this.calculateSyndromes(inputMessage, numSymbols);
		
		int wr = 0;
		int indexOfHighest = 0;
		
		// Find highest value of the return of this.calculateSyndromes
		for (int i = 0; i < syndromes.length; i++) {
			if(syndromes[i] > wr) {
				wr = syndromes[i];
				indexOfHighest = i;
			}
		}
		
		// If they are all 0, then syndromes[0] is returned, which is still a 0 value, so we still have valid results
		return syndromes[indexOfHighest] == 0;
	}
	
	public double computeCapacity() {
		// V is the version of QR code. There are 40 possible version numbers
		// They range from 21 to 177, they are the size of the QR code
		int V = this.version;
						
		// Probably don't touch this. Gotten from that one wikiversity page
		int ternary1 = V >= 2 ? 1 : 0;
		int ternary2 = V >= 7 ? 1 : 0;
		double term1 = 16 * (Math.pow(V, 2) + 8 * V + 4);
		double term2 = 25 * Math.pow(ternary1 + Math.floor(V / 7), 2);
		double term3 = ternary2 * (36 + 40 * Math.floor(V / 7));
				
		return Math.floor(0.125 * (term1 - term2 - term3));
	}
}
