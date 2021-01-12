/* Copyright (c) 2015, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.imageio;

// follow the pattern described in "http://docs.oracle.com/javase/1.5.0/docs/guide/imageio/spec/extending.fm3.html"

import org.w3c.dom.*;
import javax.xml.parsers.*; // Package name may change in J2SDK 1.4

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataFormat;
import javax.imageio.metadata.IIOMetadataNode;

public class JPEGLosslessMetadata extends IIOMetadata {

	private static final String identString = "@(#) $Header: /userland/cvs/codec/com/pixelmed/imageio/JPEGLosslessMetadata.java,v 1.2 2015/10/19 15:34:42 dclunie Exp $";

	static final String nativeMetadataFormatName = "com.pixelmed.imageio.JPEGLosslessMetadata_0.1";
	static final String nativeMetadataFormatClassName = "com.pixelmed.imageio.JPEGLosslessMetadata";
	
	// Keyword/value pairs
	List keywords = new ArrayList();
	List values = new ArrayList();

	public JPEGLosslessMetadata() {
		super(
			  false/*standardMetadataFormatSupported*/,
			  nativeMetadataFormatName,
			  nativeMetadataFormatClassName,
			  null/*extraMetadataFormatNames*/,
			  null/*extraMetadataFormatClassNames*/);
	}
	
	public IIOMetadataFormat getMetadataFormat(String formatName) {
		if (!formatName.equals(nativeMetadataFormatName)) {
			throw new IllegalArgumentException("Bad format name!");
		}
		return JPEGLosslessMetadataFormat.getDefaultInstance();
	}
	
	public Node getAsTree(String formatName) {
		if (!formatName.equals(nativeMetadataFormatName)) {
			throw new IllegalArgumentException("Bad format name!");
		}
		
		// Create a root node
		IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);
		
		// Add a child to the root node for each keyword/value pair
		Iterator keywordIter = keywords.iterator();
		Iterator valueIter = values.iterator();
		while (keywordIter.hasNext()) {
			IIOMetadataNode node = new IIOMetadataNode("KeywordValuePair");
			node.setAttribute("keyword", (String)keywordIter.next());
			node.setAttribute("value", (String)valueIter.next());
			root.appendChild(node);
		}
		
		return root;
	}
	
	public boolean isReadOnly() {
		return true;	// since no writer plug-in
	}
	
	public void reset() {
		this.keywords = new ArrayList();
		this.values = new ArrayList();
	}
	
	public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
		if (!formatName.equals(nativeMetadataFormatName)) {
			throw new IllegalArgumentException("Bad format name!");
		}
		
		Node node = root;
		if (!node.getNodeName().equals(nativeMetadataFormatName)) {
			throw new IIOInvalidTreeException("Root must be " + nativeMetadataFormatName,node);
		}
		node = node.getFirstChild();
		while (node != null) {
			if (!node.getNodeName().equals("KeywordValuePair")) {
				throw new IIOInvalidTreeException("Node name not KeywordValuePair!",node);
			}
			NamedNodeMap attributes = node.getAttributes();
			Node keywordNode = attributes.getNamedItem("keyword");
			Node valueNode = attributes.getNamedItem("value");
			if (keywordNode == null || valueNode == null) {
				throw new IIOInvalidTreeException("Keyword or value missing!",node);
			}
			
			// Store keyword and value
			keywords.add((String)keywordNode.getNodeValue());
			values.add((String)valueNode.getNodeValue());
			
			// Move to the next sibling
			node = node.getNextSibling();
		}
	}
}

