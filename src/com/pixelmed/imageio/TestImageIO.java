/* Copyright (c) 2004-2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */
package com.pixelmed.imageio;

import java.io.*;
import java.util.*;
import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*;
import javax.imageio.*;
import javax.imageio.spi.*;
import javax.swing.*; 
import javax.swing.event.*; 

public class TestImageIO extends JFrame {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/imageio/TestImageIO.java,v 1.4 2016/01/16 15:01:48 dclunie Exp $";

	class SingleImagePanel extends JComponent {
	
		BufferedImage image;
	
		SingleImagePanel(String args[]) throws Exception {
			File f = new File(args[0]);
			try {
				if (args.length == 1) {
					image = ImageIO.read(f);
				}
				else {
					Iterator readers = ImageIO.getImageReadersByFormatName(args[1]);
					int whichReader = Integer.valueOf(args[2]).intValue();
					int i=0;
					while (readers.hasNext()) {
						ImageReader reader = (ImageReader)readers.next();
						if (i == whichReader) {
							ImageReaderSpi spi = reader.getOriginatingProvider();
System.out.println("Using reader "+i+" from "+spi.getDescription(Locale.US)+" "+spi.getVendorName()+" "+spi.getVersion());
//while (true) {
							reader.setInput(ImageIO.createImageInputStream(f));
							image = reader.read(0);
//}
							break;
						}
					}
				}
				if (image == null) {
					throw new Exception("Couldn't find a reader");
				}
System.out.println("Image width="+image.getWidth()+" height="+image.getHeight());
				setSize(image.getWidth(),image.getHeight());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void paintComponent(Graphics g) {
//System.out.println("SingleImagePanel.paintComponent()");
			Graphics2D g2d=(Graphics2D)g;
			g2d.drawImage(image,0,0,this);
		}
	}

	TestImageIO(String args[]) throws Exception {

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});

		Container content = getContentPane();
		//content.setLayout(new GridLayout(1,1));
		SingleImagePanel panel = new SingleImagePanel(args);
		content.add(panel);
		setSize(panel.getWidth(),panel.getHeight());
		//pack();
	}

	public static void main(String args[]) {
	
		//javax.imageio.spi.IIORegistry.getDefaultInstance().registerApplicationClasspathSpis();
		javax.imageio.ImageIO.scanForPlugins();
		
		javax.imageio.ImageIO.setUseCache(false);		// no slow disk caches for us !
	
		String[] formats=ImageIO.getReaderFormatNames();
		for (int i=0; formats != null && i<formats.length; ++i) {
System.out.println(formats[i]);
			Iterator readers = ImageIO.getImageReadersByFormatName(formats[i]);
			while (readers.hasNext()) {
				ImageReader reader = (ImageReader)readers.next();
				ImageReaderSpi spi = reader.getOriginatingProvider();
System.out.println("\t"+spi.getDescription(Locale.US)+" "+spi.getVendorName()+" "+spi.getVersion());
			}
		}
		try {
			TestImageIO f = new TestImageIO(args);
			f.setVisible(true);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
