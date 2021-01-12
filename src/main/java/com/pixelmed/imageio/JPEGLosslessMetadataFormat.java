/* Copyright (c) 2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.imageio;

// follow the pattern described in "http://docs.oracle.com/javase/1.5.0/docs/guide/imageio/spec/extending.fm3.html"

import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadataFormatImpl;

public class JPEGLosslessMetadataFormat extends IIOMetadataFormatImpl {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/imageio/JPEGLosslessMetadataFormat.java,v 1.2 2015/10/19 15:34:42 dclunie Exp $";
	
	// Create a single instance of this class (singleton pattern)
	private static JPEGLosslessMetadataFormat defaultInstance = new JPEGLosslessMetadataFormat();
	
	// Make constructor private to enforce the singleton pattern
	private JPEGLosslessMetadataFormat() {
		// Set the name of the root node
		// The root node has a single child node type that may repeat
		super("com.pixelmed.imageio.JPEGLosslessMetadata_0.1",
			  CHILD_POLICY_REPEAT);
		
		// Set up the "KeywordValuePair" node, which has no children
		addElement("KeywordValuePair",
				   "com.pixelmed.imageio.JPEGLosslessMetadata_0.1",
				   CHILD_POLICY_EMPTY);
		
		// Set up attribute "keyword" which is a String that is required
		// and has no default value
		addAttribute("KeywordValuePair", "keyword", DATATYPE_STRING,
					 true, null);
		// Set up attribute "value" which is a String that is required
		// and has no default value
		addAttribute("KeywordValuePair", "value", DATATYPE_STRING,
					 true, null);
	}
	
	// Check for legal element name
	public boolean canNodeAppear(String elementName,ImageTypeSpecifier imageType) {
		return elementName.equals("KeywordValuePair");
	}
	
	// Return the singleton instance
	public static JPEGLosslessMetadataFormat getDefaultInstance() {
		return defaultInstance;
	}
}
