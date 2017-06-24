package org.knoesis.blooms;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;

/**
 * @author prateekjain
 *
 */
public class WikipediaArticleSearch {

	private static String ARTICLE_LINKS_URL = "https://en.wikipedia.org/w/api.php?action=query&"
			+ "prop=links&titles=";

	private String url= "";
	private String format = "";
	private int resultLimit;
	private String term = "";


	public WikipediaArticleSearch(String url, String term, String format, 
			int resultLimit) {

		this.url = url;
		this.term = term.trim();
		this.format = format;
		this.resultLimit = resultLimit;
	}


	public HashSet<String> invokeService() throws Exception {

		try {
			this.term = URLEncoder.encode(this.term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		String serviceURL = this.url + this.term + "&" + this.format + "&srlimit=" + 
				this.resultLimit;
		
//		System.out.println(serviceURL);

		InvokeWikipediaWebService invokeWS = new InvokeWikipediaWebService(serviceURL);
		String content = invokeWS.invokeWebService();
		
//		System.out.println(content);

		HashSet<String> result = new HashSet<String>();

		// Wikipedia disambiguation pages use the phrases "may refer to" or 
		// "may stand for" in the first sentence, according to their style guide. 
		// Checking for these words in the snippet allows us to save an API call
		// for each title (to get the categories). Note that this only expands
		// first-level disambiugation pages (i.e. if one disambiguation page 
		// contains a link to another, the second level page will be added directly
		// rather than expanded).

		// for each article...
		String copy = new String(content);

		while (copy.contains("<p ns=\"0\"")) {

			int startIndex = copy.indexOf("<p ns=\"0\"");
			int endIndex = copy.indexOf("/>", startIndex);
			String entry = copy.substring(startIndex, endIndex);
			copy = copy.substring(endIndex);
			
//			System.out.println("\t" + entry);

			// get the title and snippet
			String title = getTagValue("title", entry);
			String snippet = getTagValue("snippet", entry);
			
//			System.out.println("\t\t" + title);
//			System.out.println("\t\t" + snippet);

			// if the snippet contains one of the magic words that
			// indicate this is a disambiguation page, 
			if ((title != null && title.contains("disambiguation")) || 
					(snippet != null && (snippet.contains("may refer to") || 
					snippet.contains("may stand for") || snippet.contains("may also refer to")))) {

				// need to do another query to get all of the links (or page titles)
				// on this page
				String betterTitle = null;
				try {
					betterTitle = URLEncoder.encode(title, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				if (betterTitle == null) continue;

				String linksURL = ARTICLE_LINKS_URL + betterTitle + "&" + this.format;

				InvokeWikipediaWebService anotherWS = new InvokeWikipediaWebService(linksURL);
				String pageContent = anotherWS.invokeWebService();

				String pageCopy = new String(pageContent);

				while (pageCopy.contains("<pl ns=\"0\"")) {

					int linkStart = pageCopy.indexOf("<pl ns=\"0\"");
					int linkEnd = pageCopy.indexOf("/>", linkStart);
					String link = pageCopy.substring(linkStart, linkEnd);
					pageCopy = pageCopy.substring(linkEnd);

					// get the title and snippet
					String linkTitle = getTagValue("title", link);

					result.add(linkTitle);
				}

				// if this isn't a disambiguation page, just add its title
				// to the list
			} else if (title != null) {
				result.add(title);
			}
		}
		
		return result;
	}


	private static String getTagValue(String tag, String content) {
		int tagStart = content.indexOf(tag + "=\"") + tag.length()+2;
		if (tagStart < 0) return null;

		int tagEnd = content.indexOf("\"", tagStart);
		if (tagEnd < tagStart) return null;

		return content.substring(tagStart, tagEnd).trim();
	}
}
