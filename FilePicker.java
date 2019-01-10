package qrcode;

import java.awt.FileDialog;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

public class FilePicker {
	
	public String chooseFile() {
		// Create the dialog box
		FileDialog dialog = new FileDialog((JFrame) null, "Select File to Open", FileDialog.LOAD);
		
		// Show the dialog box
		dialog.setDirectory("/Desktop");
		dialog.setVisible(true);
		
		// The file that the user chooses
		String filepath = dialog.getDirectory() + dialog.getFile();
				
		// Return the path at the end
		return filepath;
	}
}
