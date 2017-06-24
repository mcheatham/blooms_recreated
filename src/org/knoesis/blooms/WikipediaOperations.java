package org.knoesis.blooms;

import java.util.HashSet;


/**
 * @author prateekjain
 *
 */
public class WikipediaOperations {
		
	private static String ARTICLE_SEARCH_URL = "https://en.wikipedia.org/w/api.php?action=query"
			+ "&list=search&srsearch=";
	
	private static String CATEGORY_SEARCH_URL = "https://en.wikipedia.org/w/api.php?action=query&"
			+ "prop=categories&titles=";

	private static String XML_FORMAT = "format=xml";
	
	
	// Using default values for hit limit from the GitHub code
	public static HashSet<String> getArticles(String term) throws Exception {
		return getArticles(term, 10);
	}

	
	public static HashSet<String> getArticles(String term, int hitLimit) throws Exception {
		
		WikipediaArticleSearch obj = new WikipediaArticleSearch(
				ARTICLE_SEARCH_URL, term, XML_FORMAT, hitLimit);
		
		return obj.invokeService();
	} 
	
	
	public static HashSet<String> getCategories(String term, int hitLimit) throws Exception {
		return getCategories(term, hitLimit, false);
	}
	

	public static HashSet<String> getCategories(String term, int hitLimit, 
			boolean includeHiddenCat) throws Exception {
		
		WikipediaCategorySearch obj = new WikipediaCategorySearch(
				CATEGORY_SEARCH_URL, term, XML_FORMAT, hitLimit, includeHiddenCat);
		
		return obj.invokeService();
	}
}

