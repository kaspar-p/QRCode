package qrcode;

import java.awt.image.BufferedImage;

public class Reader {
	
	
	public boolean verifyQR(BufferedImage image) {
		
		int width = image.getWidth();
		int height = image.getHeight();
		
		// Loop through the pixels
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				int index = x + width * y;
				int pixel = image.getRGB(x, y);
				
				// Colors
				int red = (pixel & 0x00ff0000) >> 16;
		        int green = (pixel & 0x0000ff00) >> 8;
		        int blue =  pixel & 0x000000ff;
				
				// The top 6 "safe" pixels of a QR code
				if (index <= width * 6) {
					if(red != 255 || green != 255 || blue != 255) {
						return false;
					}
				}
			}
		}
		
		
		//Check if the picture submitted is a QR code
		
		return true;
	}
}
