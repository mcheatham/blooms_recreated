package org.knoesis.blooms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXWikiWSParser extends DefaultHandler {

	private ArrayList<String> wikipediaCatList = new ArrayList<String>();
	private String content = "" ;
	
	
	public SAXWikiWSParser(String content) {
		this.content = content.trim();
	}

	
	private void writeToFile(){
		try {
		    BufferedWriter out = new BufferedWriter(new FileWriter("wikipedia-category.xml"));
		    out.write(this.content);
		    out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Parse the search result of Wikipedia Web Service result page and return a list of 
	 * categories to user.
	 * 
	 * @return a list containing categories
	 */
	public List<String> parse() {
		writeToFile();
		parseDocument();
		List<String> catList = getWikipediaCatList();
		return catList;
	}

	
	private void parseDocument() {
		
		SAXParserFactory spf = SAXParserFactory.newInstance();
		
		try {
			File file = new File("wikipedia-category.xml");
			InputStream inputStream = new FileInputStream(file);
			Reader reader = new InputStreamReader(inputStream, "UTF-8");
			
			InputSource is = new InputSource(reader);
			is.setEncoding("UTF-8");
			
			SAXParser sp = spf.newSAXParser();
			sp.parse(is, this);
			
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println(this.content);
		}
	}


	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) 
		throws SAXException {
		
		if (qName.equalsIgnoreCase("cl") || qName.equalsIgnoreCase("p")) {
			String category = attributes.getValue("title");
			this.wikipediaCatList.add(category);
		}
	}
	
	
	public ArrayList<String> getWikipediaCatList() {
		return this.wikipediaCatList;
	}	
}