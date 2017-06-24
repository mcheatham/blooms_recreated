package org.knoesis.blooms;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;


/**
 * @author prateekjain
 *
 */
public class WikipediaCategorySearch {

	private String url = "";
	private String format = "";
	private int resultLimit;
	private String term = "";
	private boolean includeHiddenCat = false;
	
	
	/**
	 * @param url instance of WikipediaURLS
	 * @param term string to be searched
	 * @param format instance of WikipediaSearchFormat
	 * @param resultLimit number of search results to be returned, default is 500
	 * @param includeHiddenCat true if hidden categories are to be included in results
	 * @param size number of results to be returned
	 * 
	 */
	public WikipediaCategorySearch(String url, String term, String format, 
			int resultLimit, boolean includeHiddenCat) {
		this.url = url;
		this.term = term;
		this.format = format;
		this.resultLimit = resultLimit;
		this.includeHiddenCat = includeHiddenCat;
	}

	
	public HashSet<String> invokeService() throws Exception {
		
		String hidden = "!hidden";
		if (this.includeHiddenCat == true)
			hidden = "hidden";
		
		try {
			this.term = URLEncoder.encode(this.term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		String serviceURL = this.url + this.term + "&" + this.format + "&cllimit=" + 
			this.resultLimit + "&clshow=" + hidden;
		
		InvokeWikipediaWebService invokeService = new InvokeWikipediaWebService(serviceURL);
		String catContent = invokeService.invokeWebService();
		
//		System.out.println(serviceURL);
//		System.out.println(catContent);
		
		SAXWikiWSParser parser = new SAXWikiWSParser(catContent);
		List<String> catList = parser.parse();
		
		HashSet<String> result = new HashSet<String>();
		for (String s: catList) {
			if (s.contains("isambiguation") || s.contains("ikipedia")) continue;
			s = s.replaceAll("Category:", "");
			result.add(s);
		}
		
		return result;
	}
}
