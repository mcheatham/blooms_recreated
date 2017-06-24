package org.knoesis.utils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Scanner;

import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

public class StringUtils {
	
	private static HashSet<String> stopwords = new HashSet<String>();
	private boolean removeStopwords;
	
	
	public StringUtils(boolean removeStopwords) {
		
		this.removeStopwords = removeStopwords;
		
		if (removeStopwords) {
			try {
				Scanner in = new Scanner(new File("stopwords.txt"));
				while (in.hasNext()) {
					stopwords.add(in.nextLine().toLowerCase().trim());
				}
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public String getString(OWLEntity e, OWLOntology ontology) {
		
		String label = e.getIRI().toString();
		
		if (label.contains("#")) {
			label = label.substring(label.indexOf('#')+1);
		}
		
		if (label.contains("/")) {
			label = label.substring(label.lastIndexOf('/')+1);
		}
		
		// break up words (camelCase)
		String s = "" + label.charAt(0);
		
		for (int i=1; i<label.length(); i++) {
			
			if (Character.isUpperCase(label.charAt(i)) && 
					!Character.isUpperCase(label.charAt(i-1))) {
				s += " ";
			} 
			
			s += label.charAt(i);
		}
		
		s = s.toLowerCase();
		s = s.replaceAll("-", " ");
		s = s.replaceAll("_", " ");
		
		if (!removeStopwords) return s;
		
		String result = "";
		String[] words = s.split("[ ]");
		for (String word: words) {
			if (!stopwords.contains(word)) {
				result += word + " ";
			}
		}
		
		return result.trim();
		
//    	Set<OWLAnnotation> labels = e.getAnnotations(
//    			ontology, new OWLAnnotationPropertyImpl(
//    			OWLDataFactoryImpl.getInstance(), 
//    			IRI.create("http://www.w3.org/2000/01/rdf-schema#label")));
    	
//        if (labels != null && labels.size() > 0) {
//    		label = ((OWLAnnotation) labels.toArray()[0]).getValue().toString();
//    		if (label.startsWith("\"")) {
//    			label = label.substring(1);
//    		}
//    		
//    		if (label.contains("\"")) {
//    			label = label.substring(0, label.lastIndexOf('"'));
//    		}
//    	} else {
//    		label = label.replaceAll("-|_", " ");
//    	}

	}
	
	
	public static String getNamespace(URI file) {
		
		// find the base namespace -- xml:base
		String namespace = null;
		Scanner input;
		try {
			input = new Scanner(file.toURL().openStream());

			while (input.hasNext()) {
				String current = input.nextLine();
				if (current.contains("xml:base")) {
					namespace = current.substring(
							current.indexOf("xml:base")+10, 
							current.lastIndexOf('\"'));
					namespace = namespace.trim();
					if (namespace.contains(" ")) {
						namespace = namespace.substring(0, 
								namespace.indexOf(" ")-1);
					}
					break;
				}
			}
			input.close();
		} catch (IOException e) { e.printStackTrace(); }
		
		if (namespace == null) {
			namespace = "";
		}
		return namespace;
		
	}

}
