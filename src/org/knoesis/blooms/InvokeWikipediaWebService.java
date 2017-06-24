package org.knoesis.blooms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author prateekjain
 *
 */
public class InvokeWikipediaWebService {

	String url = null;


	public InvokeWikipediaWebService(String serviceURL) {
		this.url = serviceURL;
	}


	public String invokeWebService() throws Exception {
		
		String text = "";
		
		try {
			
			URL url = new URL(this.url);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			
			boolean keepTrying = true;
			
			while (keepTrying) {
				try {
					conn.connect();
					keepTrying = false;
				} catch (ConnectException e) {
					System.err.println(e.getMessage());
				}
			}
			
			InputStream in = conn.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			
			String str;
		    while ((str = reader.readLine()) != null) {
		    	text = text + str;
		    }
		       
			conn.disconnect();
			
		} catch(IOException ex) {
			ex.printStackTrace();
			throw ex;
		}
		
	    if (text.trim().length() > 0) {
	    	return text;
	    } else {
	    	return null;
	    }
	}
}
