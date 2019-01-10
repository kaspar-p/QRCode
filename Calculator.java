package qrcode;
import java.math.*;
import java.util.ArrayList;

public class Calculator {
	// Precomputed log and anti-log tables for RS encoder
	public static int[] GF_exp = new int[512];
	public static int[] GF_log = new int[256];
	
	public Calculator() {
		// Compute the Calculator.GF_exp and Calculator.GF_log tables
		initTables(0);
	}
	
	// Create the precomputed Calculator.GF_exp and Calculator.GF_log tables
	public static void initTables(int prime) {
		// default value
		if (prime == 0) {
			prime = 0x11d;
		}
		
		int x = 1;
				
		for (int i = 0; i < 255; i++) {
			Calculator.GF_exp[i] = x;
			Calculator.GF_log[x] = i;
			
			x = GF_mult_noLUT(x, 2, prime, true, 256);
		}
		
		for (int i = 255; i < 512; i++) { 
			Calculator.GF_exp[i] = Calculator.GF_exp[i - 255];
		}
	}
	
	// Same as addition in a Galois field
	public static int GF_sub(int x, int y) {
		return x ^ y;
	}
	
	// Adding numbers
	public static int GF_add(int x, int y) {
		return x ^ y;
	}
	
	// Division
	public static int GF_div(int x, int y) {
		if(x == 0) {
			return 0;
		} 
		
		if(y == 0) {
			System.out.println("Division by 0 attempted, exiting program!");
			System.exit(0);
		}
		int inner = newMod(Calculator.GF_log[x] - Calculator.GF_log[y] + 255, 255);		
		int calc = Calculator.GF_exp[inner];		
		return calc;
	}
	
	public static int[] generateGenerator(int numSymbols) {
		// Generate the irreducible polynomial that is required for RS encoding
		
		ArrayList<Integer> generator = new ArrayList<Integer>();
		generator.add(1);
		
		for (int i = 0; i < numSymbols; i++) {
			ArrayList<Integer> term2 = new ArrayList<Integer>();
			term2.add(1);
			term2.add(GF_pow(2, i));
			
			generator = Decoder.convertBack(polynomial_mult(Decoder.convertList(generator), Decoder.convertList(term2)));
		}
		
		return Decoder.convertList(generator);
	}
	
	public static int[] polynomial_add(int[] p, int[] q) {
		int[] r = new int[Math.max(p.length, q.length)];
			
		// Fill r with all the p values
		for (int i = 0; i < p.length; i++) {
			int l = i + r.length - p.length;
			r[l] = p[i];
		}
		
		// Now go through and XOR all the values - 001 + 101 = 100
		for (int i = 0; i < q.length; i++) {
			int l = i + r.length - q.length;
			r[l] ^= q[i];
		}
		
		return r;
	}
	
	// Evaluates a polynomial at a certain x value - Horner's method for optimization
	public static int polynomial_eval(int[] p, int x) {
		int y = p[0];
		
		for (int i = 1; i < p.length; i++) {
			y = (GF_mult(y, x)) ^ (p[i]);
		}
		
		return y;
	}
	
	// Multiply two polynomials
	public static int[] polynomial_mult(int[] p1, int[] p2) {
		int[] r = new int[p1.length + p2.length - 1];
		
		for (int j = 0; j < p2.length; j++) {
			for(int i = 0; i < p1.length; i++) {
				r[i + j] ^= GF_mult(p1[i], p2[j]);
			}
		}
		
		return r;
	}
	
	// Divides two polynomials using a method called Extended Synthetic division - faster than long division
	// Opposed to the other functions here, the coefficients of Calculator are kept in a list backwards than the others
	// 1 + 2x + 5x^5 is [5, 2, 1] not [1, 2, 5] like it would be in the other functions
	public static int[][] polynomial_divide(int[] dividend, int[] divisor) {
		
		// Copy the dividend array
		int[] quo = new int[dividend.length];
		for (int i = 0; i < dividend.length; i++) {
			quo[i] = dividend[i];
		}
		
		// Do the recursive algorithm
		for (int i = 0; i < dividend.length - (divisor.length - 1); i++) {
			int coef = quo[i];
			
			// Since log(0) is undefined
			if(coef != 0) {
				
				for (int j = 0; j < divisor.length; j++) {
					
					// Since log(0) is undefined again
					if (divisor[j] != 0) {
						quo[i + j] ^= GF_mult(divisor[j], coef);
					}
				}
			}
		}
		
		// Since the remainder and quotient have the same degree, and degree == length - 1
		// We compute the index of the seperation
		int seperator = -(divisor.length - 1);
				
		ArrayList<Integer> remainder = new ArrayList<Integer>();
		ArrayList<Integer> quotient = new ArrayList<Integer>();
		
		// Fill quotient and remainder arrays
		for(int i = 0; i < quo.length + seperator; i++) {
		    quotient.add(quo[i]);
		}

		for(int i = quo.length + seperator; i < quo.length; i++) {
		    remainder.add(quo[i]);
		}
		
		// First half then second half
		int[][] finalResult = new int[][] {Decoder.convertList(quotient), Decoder.convertList(remainder)};
		return finalResult;
	}
	
	// Multiplies a polynomial by a scalar - polynomial is array
	public static int[] polynomial_scale(int[] poly, int scalar) {
		int[] r = new int[poly.length];
		
		for (int i = 0; i < poly.length; i++) {
			r[i] = GF_mult(poly[i], scalar);
		}
		
		return r;
	}
	
	public static int GF_pow(int x, int power) {
		return GF_exp[Math.floorMod(GF_log[x] * power, 255)];
	}
	
	public static int newMod(int n, int mod) {
		return (((n % mod) + mod) % mod);
	}
	
	public static int GF_inverse(int x) {
		return GF_exp[255 - GF_log[x]];
		// This is the same as GF_div(1, x) but faster
	}
	
	// Multiplication
	public static int GF_mult(int x, int y) {
		
		if(x == 0 || y == 0) {
			return 0;
		}
		
		return Calculator.GF_exp[Calculator.GF_log[x] + Calculator.GF_log[y]];
	}
	
	// Function to multiply binary numbers within GF(2^8)
	// Carry-less bitwise multiplication
	public static int GF_carrylessMult(int num1, int num2) {
		int z = 0; // The final result
		int i = 0; // The iterator
		
		while ((num2 >> i) > 0) {
						
			if ((num2 & (1 << i)) != 0 ? true : false) {
				z ^= num1 << i;
			}
						
			i++;
		}
		
		return z;
	}
	
	// The no lookup table mult with prime reduction
	public static int GF_mult_modReduct(int num1, int num2, int prime) {
		int result = GF_carrylessMult(num1, num2);
		
		if (prime > 0) {
			result = GF_carrylessDiv(result, prime);
		}
		
		return result;
	}
	
	public static int GF_mult_noLUT (int x, int y, int prime, boolean isCarryless, int fieldCharactersFull) {
		if (fieldCharactersFull == 0) {
			fieldCharactersFull = 256;
		}
		
		int r = 0;
		
		while (y > 0) {
			if ((y & 1) != 0) {
				if(isCarryless) {
					r ^= x;
				} else {
					r+= x;
				}
			}
			
			y = y >> 1;
			x = x << 1;
			
			if(prime > 0 && (x & fieldCharactersFull) != 0) {
				x = x ^ prime;
			}
		}
		
		return r;
	}
	
	// A function to divide numbers without carrying, without the Galois Field in terms of 2^8
	// Also called GF(2^8). Carry-less bit-wise division...
	public static int GF_carrylessDiv(int dividend, int divisor) {
		int dl1 = bitLength(dividend);
		int dl2 = bitLength(divisor);
		
		if (dl1 < dl2) {
			return dividend;
		}
		
		for (int i = dl1 - dl2; i >= -1; i--) {
			
			if ((dividend & (1 << i + dl2 - 1)) != 0 ? true : false) {
				dividend ^= divisor << 1;
			}
		}
		
		return dividend;
	}
	
	// One liner - convert num to string and string to bigInteger
	public static int bitLength(int num) {
		return new BigInteger(Integer.valueOf(num).toString()).bitLength();
	}
}
