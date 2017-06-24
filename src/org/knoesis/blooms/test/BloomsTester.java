package org.knoesis.blooms.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.knoesis.blooms.BloomsTree;
import org.knoesis.blooms.InvokeWikipediaWebService;
import org.knoesis.blooms.WikipediaBLOOMS;
import org.knoesis.utils.StringUtils;
import org.knoesis.utils.WordNet;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import fr.inrialpes.exmo.align.ling.JWNLAlignment;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

public class BloomsTester {
	
	static int count = 0;
	
	public static void main(String[] args) throws Exception{
		
		String dir = "data/";

		String[] tests = {"."};
		
		String alignmentType = "blooms paper";
		// options are exact match, word net, alignment api, wikipedia articles,
		// wikipedia categories, blooms paper, blooms github
		
		double threshold = 0.80;
		boolean includeProperties = false;
		boolean compareLikeTypes = true;
		boolean removeStopwords = false;
		boolean evaluateResults = false;
		
		// delete the cache if you change any of these
		int bloomsTreeDepth = 4;
		int articleLimit = 5;
		int categoryLimit = 500;
		
		for (String test: tests) {
			
			File localDir = new File(dir + test);
			
			if (alignmentType.equals("exact match"))
				runExactMatch(localDir, includeProperties, compareLikeTypes, removeStopwords);
			
			if (alignmentType.equals("word net")) 
				runWordNet(localDir, includeProperties, compareLikeTypes, removeStopwords);
			
			if (alignmentType.equals("alignment api"))
				runAlignmentAPI(localDir, 0.95);
			
			if (alignmentType.equals("wikipedia articles"))
				runWikipediaArticles(localDir, includeProperties, 
						compareLikeTypes, removeStopwords, articleLimit);
			
			if (alignmentType.equals("wikipedia categories"))
				runWikipediaCategories(localDir, includeProperties, 
						compareLikeTypes, removeStopwords, articleLimit, categoryLimit);
			
			if (alignmentType.equals("blooms paper"))
				runBLOOMSPaper(localDir, threshold, includeProperties, 
						compareLikeTypes, removeStopwords, bloomsTreeDepth, 
						articleLimit, categoryLimit);
			
			if (alignmentType.equals("blooms github"))
				runBLOOMSGithub(localDir, includeProperties, compareLikeTypes, 
						removeStopwords, articleLimit, categoryLimit);
			
			if (evaluateResults) 
				evaluateResults(localDir);
		}

		System.out.println("Done!");
	}
	
	 
	public static void runExactMatch(File dir, boolean includeProperties, 
			boolean compareLikeTypes, boolean removeStopwords) throws Exception {
		
		StringUtils stringUtils = new StringUtils(removeStopwords);
		
		for (File f: dir.listFiles()) {

			String filename = f.getName();
			
			// skip all files that aren't reference alignments
			if (!(filename.contains("-") && 
					filename.substring(filename.lastIndexOf("-"), filename.indexOf(".rdf")).length() > 2)) 
				continue; 

			// get the filenames for the ontologies involved in this reference alignment
			String s1 = filename.substring(0, filename.indexOf("-")).trim();
			s1 += ".rdf";
			s1 = dir.getAbsolutePath() + "/" + s1;

			String s2 = filename.substring(filename.indexOf("-")+1).trim();
			s2 = dir.getAbsolutePath() + "/" + s2;

			System.out.println(filename + "   " + s1 + "   " + s2);
				
			Alignment alignment = new URIAlignment();
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			IRI iriA = IRI.create(new File(s1));
			OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
			String ns1 = StringUtils.getNamespace(iriA.toURI());
			
			IRI iriB = IRI.create(new File(s2));
			OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
			String ns2 = StringUtils.getNamespace(iriB.toURI());
			
			for (OWLEntity entityA: ontA.getSignature(false)) {
				
				// skip entities not in the namespace
				if (!entityA.toString().contains(ns1)) continue;
				
				// always skip instances
				if (entityA.isOWLNamedIndividual()) continue;
				
				// skip properties if directed to do so
				if (!includeProperties && (entityA.isOWLDataProperty() || 
						entityA.isOWLObjectProperty())) continue;
				
				String labelA = stringUtils.getString(entityA, ontA);

				for (OWLEntity entityB: ontB.getSignature(false)) {

					if (!entityB.toString().contains(ns2)) continue;
				
					if (entityB.isOWLNamedIndividual()) continue;
				
					// skip properties if directed to do so
					if (!includeProperties && (entityB.isOWLDataProperty() || 
							entityB.isOWLObjectProperty())) continue;
					
					// only compare like entities
					if (compareLikeTypes) {
						if (entityA.isOWLClass() && !entityB.isOWLClass()) continue;
						if (!entityA.isOWLClass() && entityB.isOWLClass()) continue;
					}
					
					String labelB = stringUtils.getString(entityB, ontB);	
					
					// check for label match
					if (labelA.equals(labelB)) {
						alignment.addAlignCell(entityA.getIRI().toURI(), 
								entityB.getIRI().toURI(), "=", 1.0);
					}
				}
			}
			
			File outputFile = new File(dir + "/BLOOMS_results/" + filename);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), "UTF-8"), true);
			AlignmentVisitor renderer = new RDFRendererVisitor(writer);
			alignment.render(renderer);
			writer.close();
		}
	}
	
	
	public static void runWordNet(File dir, boolean includeProperties, 
			boolean compareLikeTypes, boolean removeStopwords) throws Exception {
		
		StringUtils stringUtils = new StringUtils(removeStopwords);
		
		for (File f: dir.listFiles()) {

			String filename = f.getName();
			
			// skip all files that aren't reference alignments
			if (!(filename.contains("-") && 
					filename.substring(filename.lastIndexOf("-"), filename.indexOf(".rdf")).length() > 2)) 
				continue; 

			// get the filenames for the ontologies involved in this reference alignment
			String s1 = filename.substring(0, filename.indexOf("-")).trim();
			s1 += ".rdf";
			s1 = dir.getAbsolutePath() + "/" + s1;

			String s2 = filename.substring(filename.indexOf("-")+1).trim();
			s2 = dir.getAbsolutePath() + "/" + s2;

			System.out.println(filename + "   " + s1 + "   " + s2);
				
			Alignment alignment = new URIAlignment();
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			IRI iriA = IRI.create(new File(s1));
			OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
			String ns1 = StringUtils.getNamespace(iriA.toURI());
			
			IRI iriB = IRI.create(new File(s2));
			OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
			String ns2 = StringUtils.getNamespace(iriB.toURI());
			
			for (OWLEntity entityA: ontA.getSignature(false)) {
				
				// skip entities not in the namespace
				if (!entityA.toString().contains(ns1)) continue;
				
				// always skip instances
				if (entityA.isOWLNamedIndividual()) continue;
				
				// skip properties if directed to do so
				if (!includeProperties && (entityA.isOWLDataProperty() || 
						entityA.isOWLObjectProperty())) continue;
				
				String labelA = stringUtils.getString(entityA, ontA);

				for (OWLEntity entityB: ontB.getSignature(false)) {

					if (!entityB.toString().contains(ns2)) continue;
				
					if (entityB.isOWLNamedIndividual()) continue;
				
					// skip properties if directed to do so
					if (!includeProperties && (entityB.isOWLDataProperty() || 
							entityB.isOWLObjectProperty())) continue;
					
					// only compare like entities
					if (compareLikeTypes) {
						if (entityA.isOWLClass() && !entityB.isOWLClass()) continue;
						if (!entityA.isOWLClass() && entityB.isOWLClass()) continue;
					}
					
					String labelB = stringUtils.getString(entityB, ontB);	
					
					// check for at least one Wordnet synonym in common
					HashSet<String> aSynonyms = WordNet.getInstance().getSynonyms(labelA);
					HashSet<String> bSynonyms = WordNet.getInstance().getSynonyms(labelB);
					for (String a: aSynonyms) {
						if (bSynonyms.contains(a)) {
							alignment.addAlignCell(entityA.getIRI().toURI(), 
									entityB.getIRI().toURI(), "=", 1.0);
							break;
						}
					}
				}
			}
			
			File outputFile = new File(dir + "/BLOOMS_results/" + filename);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), "UTF-8"), true);
			AlignmentVisitor renderer = new RDFRendererVisitor(writer);
			alignment.render(renderer);
			writer.close();
		}
	}
	
	
	public static void runAlignmentAPI(File dir, double threshold) throws Exception {
		
		for (File f: dir.listFiles()) {

			String filename = f.getName();
			
			// skip all files that aren't reference alignments
			if (!(filename.contains("-") && 
					filename.substring(filename.lastIndexOf("-"), filename.indexOf(".rdf")).length() > 2)) 
				continue; 

			// get the filenames for the ontologies involved in this reference alignment
			String s1 = filename.substring(0, filename.indexOf("-")).trim();
			s1 += ".rdf";
			s1 = dir.getAbsolutePath() + "/" + s1;

			String s2 = filename.substring(filename.indexOf("-")+1).trim();
			s2 = dir.getAbsolutePath() + "/" + s2;

			System.out.println(filename + "   " + s1 + "   " + s2);
				
			Alignment alignment = new URIAlignment();
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			IRI iriA = IRI.create(new File(s1));
			OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
			
			IRI iriB = IRI.create(new File(s2));
			OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
			
			AlignmentProcess a = new JWNLAlignment();
			a.init (iriA.toURI(), iriB.toURI());
			Properties properties = new Properties();
			properties.setProperty("wndict", "/usr/local/Cellar/wordnet/3.1/dict");
			a.align((Alignment) null, properties);
			
			// remove instances from the alignment
			Enumeration<Cell> cells = a.getElements();
			while (cells.hasMoreElements()) {
				
				Cell c = cells.nextElement();
				
				double strength = c.getStrength();
				if (strength <= threshold) continue;
				
				URI obj1 = new URI(c.getObject1().toString().replaceAll("<", "").replaceAll(">", ""));
				URI obj2 = new URI(c.getObject2().toString().replaceAll("<", "").replaceAll(">", ""));
				
				OWLEntity ent1 = ontA.getEntitiesInSignature(IRI.create(obj1)).iterator().next();
				OWLEntity ent2 = ontB.getEntitiesInSignature(IRI.create(obj2)).iterator().next();
				
				if (ent1.isOWLNamedIndividual() || ent2.isOWLNamedIndividual()) continue;

				alignment.addAlignCell(obj1, obj2, "=", strength);
			}
			
			File outputFile = new File(dir + "/BLOOMS_results/" + filename);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), "UTF-8"), true);
			AlignmentVisitor renderer = new RDFRendererVisitor(writer);
			alignment.render(renderer);
			writer.close();
		}
	}
	
	
	public static void runWikipediaArticles(File dir, boolean includeProperties, 
			boolean compareLikeTypes, boolean removeStopwords, int bloomsTreeBreadth) throws Exception {
		
		StringUtils stringUtils = new StringUtils(removeStopwords);
		
		for (File f: dir.listFiles()) {

			String filename = f.getName();
			
			// skip all files that aren't reference alignments
			if (!(filename.contains("-") && 
					filename.substring(filename.lastIndexOf("-"), filename.indexOf(".rdf")).length() > 2)) 
				continue; 

			// get the filenames for the ontologies involved in this reference alignment
			String s1 = filename.substring(0, filename.indexOf("-")).trim();
			s1 += ".rdf";
			s1 = dir.getAbsolutePath() + "/" + s1;

			String s2 = filename.substring(filename.indexOf("-")+1).trim();
			s2 = dir.getAbsolutePath() + "/" + s2;

			System.out.println(filename + "   " + s1 + "   " + s2);
			
			WikipediaBLOOMS blooms = WikipediaBLOOMS.getInstance();		
			Alignment alignment = new URIAlignment();
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			IRI iriA = IRI.create(new File(s1));
			OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
			String ns1 = StringUtils.getNamespace(iriA.toURI());
			
			IRI iriB = IRI.create(new File(s2));
			OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
			String ns2 = StringUtils.getNamespace(iriB.toURI());
			
			for (OWLEntity entityA: ontA.getSignature(false)) {
				
				// skip entities not in the namespace
				if (!entityA.toString().contains(ns1)) continue;
				
				// always skip instances
				if (entityA.isOWLNamedIndividual()) continue;
				
				// skip properties if directed to do so
				if (!includeProperties && (entityA.isOWLDataProperty() || 
						entityA.isOWLObjectProperty())) continue;
				
				String labelA = stringUtils.getString(entityA, ontA);
				HashSet<String> aArticles = blooms.getArticles(labelA, bloomsTreeBreadth);

				for (OWLEntity entityB: ontB.getSignature(false)) {

					if (!entityB.toString().contains(ns2)) continue;
				
					if (entityB.isOWLNamedIndividual()) continue;
				
					// skip properties if directed to do so
					if (!includeProperties && (entityB.isOWLDataProperty() || 
							entityB.isOWLObjectProperty())) continue;
					
					// only compare like entities
					if (compareLikeTypes) {
						if (entityA.isOWLClass() && !entityB.isOWLClass()) continue;
						if (!entityA.isOWLClass() && entityB.isOWLClass()) continue;
					}
					
					String labelB = stringUtils.getString(entityB, ontB);
					HashSet<String> bArticles = blooms.getArticles(labelB, bloomsTreeBreadth);
					
					for (String a: aArticles) {
						if (bArticles.contains(a)) {
							
							alignment.addAlignCell(entityA.getIRI().toURI(), 
									entityB.getIRI().toURI(), "=", 1.0);
							break;
						}
					}
				}
			}
			
			File outputFile = new File(dir + "/BLOOMS_results/" + filename);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), "UTF-8"), true);
			AlignmentVisitor renderer = new RDFRendererVisitor(writer);
			alignment.render(renderer);
			writer.close();
			
			blooms.saveCache();
		}
	}

	
	public static void runWikipediaCategories(File dir, boolean includeProperties, 
			boolean compareLikeTypes, boolean removeStopwords, int articleLimit, 
			int categoryLimit) throws Exception {
		
		StringUtils stringUtils = new StringUtils(removeStopwords);
		
		for (File f: dir.listFiles()) {

			String filename = f.getName();
			
			// skip all files that aren't reference alignments
			if (!(filename.contains("-") && 
					filename.substring(filename.lastIndexOf("-"), filename.indexOf(".rdf")).length() > 2)) 
				continue; 

			// get the filenames for the ontologies involved in this reference alignment
			String s1 = filename.substring(0, filename.indexOf("-")).trim();
			s1 += ".rdf";
			s1 = dir.getAbsolutePath() + "/" + s1;

			String s2 = filename.substring(filename.indexOf("-")+1).trim();
			s2 = dir.getAbsolutePath() + "/" + s2;

			System.out.println(filename + "   " + s1 + "   " + s2);
			
			WikipediaBLOOMS blooms = WikipediaBLOOMS.getInstance();		
			Alignment alignment = new URIAlignment();
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			IRI iriA = IRI.create(new File(s1));
			OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
			String ns1 = StringUtils.getNamespace(iriA.toURI());
			
			IRI iriB = IRI.create(new File(s2));
			OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
			String ns2 = StringUtils.getNamespace(iriB.toURI());
			
			for (OWLEntity entityA: ontA.getSignature(false)) {
				
				// skip entities not in the namespace
				if (!entityA.toString().contains(ns1)) continue;
				
				// always skip instances
				if (entityA.isOWLNamedIndividual()) continue;
				
				// skip properties if directed to do so
				if (!includeProperties && (entityA.isOWLDataProperty() || 
						entityA.isOWLObjectProperty())) continue;
				
				String labelA = stringUtils.getString(entityA, ontA);
				
				HashSet<String> aCategories = blooms.getCategories(
						labelA, articleLimit, categoryLimit);

				for (OWLEntity entityB: ontB.getSignature(false)) {

					if (!entityB.toString().contains(ns2)) continue;
				
					if (entityB.isOWLNamedIndividual()) continue;
				
					// skip properties if directed to do so
					if (!includeProperties && (entityB.isOWLDataProperty() || 
							entityB.isOWLObjectProperty())) continue;
					
					// only compare like entities
					if (compareLikeTypes) {
						if (entityA.isOWLClass() && !entityB.isOWLClass()) continue;
						if (!entityA.isOWLClass() && entityB.isOWLClass()) continue;
					}
					
					String labelB = stringUtils.getString(entityB, ontB);
					HashSet<String> bCategories = blooms.getCategories(
							labelB, articleLimit, categoryLimit);
					
					if (labelA.equals("phd thesis") && labelB.equals("masters thesis")) {
						System.out.println(aCategories);
						System.out.println("\n" + bCategories);
					}
					
					for (String a: aCategories) {
						if (bCategories.contains(a)) {
							
							if (labelA.equals("phd thesis") && labelB.equals("masters thesis")) {
								System.out.println("overlap on " + a);
							}
							
							alignment.addAlignCell(entityA.getIRI().toURI(), 
									entityB.getIRI().toURI(), "=", 1.0);
							break;
						}
					}
				}
			}
			
			File outputFile = new File(dir + "/BLOOMS_results/" + filename);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), "UTF-8"), true);
			AlignmentVisitor renderer = new RDFRendererVisitor(writer);
			alignment.render(renderer);
			writer.close();
			
			blooms.saveCache();
		}
	}
	
	
	public static void runBLOOMSPaper(File dir, double threshold, boolean includeProperties, 
			boolean compareLikeTypes, boolean removeStopwords, int bloomsTreeDepth, 
			int articleLimit, int categoryLimit) throws Exception {
		
		StringUtils stringUtils = new StringUtils(removeStopwords);
		
		for (File f: dir.listFiles()) {

			String filename = f.getName();
			System.out.println(filename);
			
			// skip all files that aren't reference alignments
			if (!(filename.contains("-") && 
					filename.substring(filename.lastIndexOf("-"), filename.indexOf(".rdf")).length() > 2)) 
				continue; 

			// get the filenames for the ontologies involved in this reference alignment
			String s1 = filename.substring(0, filename.indexOf("-")).trim();
			s1 += ".rdf";
			s1 = dir.getAbsolutePath() + "/" + s1;

			String s2 = filename.substring(filename.indexOf("-")+1).trim();
			s2 = dir.getAbsolutePath() + "/" + s2;

			System.out.println(filename + "   " + s1 + "   " + s2);
			
			// run BLOOMS
			WikipediaBLOOMS blooms = WikipediaBLOOMS.getInstance();		
			Alignment bloomsAlignment = new URIAlignment();
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			IRI iriA = IRI.create(new File(s1));
			OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
			String ns1 = StringUtils.getNamespace(iriA.toURI());
			
			IRI iriB = IRI.create(new File(s2));
			OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
			String ns2 = StringUtils.getNamespace(iriB.toURI());
			
			for (OWLEntity entityA: ontA.getSignature(false)) {
				
				// skip entities not in the namespace
				if (!entityA.toString().contains(ns1)) continue;
				
				// always skip instances
				if (entityA.isOWLNamedIndividual()) continue;
				
				// skip properties if directed to do so
				if (!includeProperties && (entityA.isOWLDataProperty() || 
						entityA.isOWLObjectProperty())) continue;
				
				String labelA = stringUtils.getString(entityA, ontA);
				
				ArrayList<BloomsTree> aForest = blooms.createBloomsForest(
						labelA, bloomsTreeDepth, articleLimit, categoryLimit);

				for (OWLEntity entityB: ontB.getSignature(false)) {

					if (!entityB.toString().contains(ns2)) continue;
				
					if (entityB.isOWLNamedIndividual()) continue;
				
					// skip properties if directed to do so
					if (!includeProperties && (entityB.isOWLDataProperty() || 
							entityB.isOWLObjectProperty())) continue;
					
					// only compare like entities
					if (compareLikeTypes) {
						if (entityA.isOWLClass() && !entityB.isOWLClass()) continue;
						if (!entityA.isOWLClass() && entityB.isOWLClass()) continue;
					}
					
					String labelB = stringUtils.getString(entityB, ontB);
					ArrayList<BloomsTree> bForest = blooms.createBloomsForest(
							labelB, bloomsTreeDepth, articleLimit, categoryLimit);

					double probASubB = blooms.getSubclassProbability(aForest, bForest);
					double probBSubA = blooms.getSubclassProbability(bForest, aForest);	

					if (probASubB >= threshold && probBSubA >= threshold) {					
						if (probASubB == probBSubA) {
							bloomsAlignment.addAlignCell(entityA.getIRI().toURI(), 
									entityB.getIRI().toURI(), "=", (probASubB + probBSubA) / 2.0);
						} 	
					}
				}
			}
			
			File outputFile = new File(dir + "/BLOOMS_results/" + filename);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), "UTF-8"), true);
			AlignmentVisitor renderer = new RDFRendererVisitor(writer);
			bloomsAlignment.render(renderer);
			writer.close();
			
			blooms.saveCache();
		}
	}
	
	
	// this considers two things to be a match if they have any first-level categories
	// or WordNet synonyms in common
	
	public static void runBLOOMSGithub(File dir, boolean includeProperties, 
			boolean compareLikeTypes, boolean removeStopwords, int articleLimit, 
			int categoryLimit) throws Exception {
		
		StringUtils stringUtils = new StringUtils(removeStopwords);
		
		for (File f: dir.listFiles()) {

			String filename = f.getName();
			
			// skip all files that aren't reference alignments
			if (!(filename.contains("-") && 
					filename.substring(filename.lastIndexOf("-"), filename.indexOf(".rdf")).length() > 2)) 
				continue; 

			// get the filenames for the ontologies involved in this reference alignment
			String s1 = filename.substring(0, filename.indexOf("-")).trim();
			s1 += ".rdf";
			s1 = dir.getAbsolutePath() + "/" + s1;

			String s2 = filename.substring(filename.indexOf("-")+1).trim();
			s2 = dir.getAbsolutePath() + "/" + s2;

			System.out.println(filename + "   " + s1 + "   " + s2);
			
			WikipediaBLOOMS blooms = WikipediaBLOOMS.getInstance();		
			Alignment alignment = new URIAlignment();
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			IRI iriA = IRI.create(new File(s1));
			OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
			String ns1 = StringUtils.getNamespace(iriA.toURI());
			
			IRI iriB = IRI.create(new File(s2));
			OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
			String ns2 = StringUtils.getNamespace(iriB.toURI());
			
			for (OWLEntity entityA: ontA.getSignature(false)) {
				
				// skip entities not in the namespace
				if (!entityA.toString().contains(ns1)) continue;
				
				// always skip instances
				if (entityA.isOWLNamedIndividual()) continue;
				
				// skip properties if directed to do so
				if (!includeProperties && (entityA.isOWLDataProperty() || 
						entityA.isOWLObjectProperty())) continue;
				
				String labelA = stringUtils.getString(entityA, ontA);
				
				HashSet<String> aCategories = blooms.getCategories(
						labelA, articleLimit, categoryLimit);

				for (OWLEntity entityB: ontB.getSignature(false)) {

					if (!entityB.toString().contains(ns2)) continue;
				
					if (entityB.isOWLNamedIndividual()) continue;
				
					// skip properties if directed to do so
					if (!includeProperties && (entityB.isOWLDataProperty() || 
							entityB.isOWLObjectProperty())) continue;
					
					// only compare like entities
					if (compareLikeTypes) {
						if (entityA.isOWLClass() && !entityB.isOWLClass()) continue;
						if (!entityA.isOWLClass() && entityB.isOWLClass()) continue;
					}
					
					String labelB = stringUtils.getString(entityB, ontB);
					HashSet<String> bCategories = blooms.getCategories(
							labelB, articleLimit, categoryLimit);
					
					boolean match = false;
					for (String a: aCategories) {
						if (bCategories.contains(a)) {				
							alignment.addAlignCell(entityA.getIRI().toURI(), 
									entityB.getIRI().toURI(), "=", 1.0);
							match = true;
							break;
						}
					}
					
					if (!match) {
						
						HashSet<String> aSynonyms = WordNet.getInstance().getSynonyms(labelA);
						HashSet<String> bSynonyms = WordNet.getInstance().getSynonyms(labelB);
						
						for (String a: aSynonyms) {
							if (bSynonyms.contains(a)) {
								alignment.addAlignCell(entityA.getIRI().toURI(), 
										entityB.getIRI().toURI(), "=", 1.0);
								break;
							}
						}
					}
				}
			}
			
			File outputFile = new File(dir + "/BLOOMS_results/" + filename);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(
					new FileOutputStream(outputFile), "UTF-8"), true);
			AlignmentVisitor renderer = new RDFRendererVisitor(writer);
			alignment.render(renderer);
			writer.close();
			
			blooms.saveCache();
		}
	}
	
	
	public static void evaluateResults(File dir) throws Exception {
		
		int truePositives = 0;
		int falseNegatives = 0;
		int falsePositives = 0;
		
		// get all of the blooms alignments
		File bloomsResults = new File(dir.getAbsolutePath() + "/BLOOMS_results/");

		for (File bloomsFile: bloomsResults.listFiles()) {
			
			if (!bloomsFile.getName().endsWith(".rdf")) continue;
			
//			System.out.println("\n" + bloomsFile.getName());
			
			// for each one, get the corresponding reference alignment
			File refFile = new File(dir.getAbsolutePath() + "/" + bloomsFile.getName());
			
			AlignmentParser parser = new AlignmentParser();
			Alignment bloomsAlignment = parser.parse(new FileReader(bloomsFile));
			Alignment refAlignment = parser.parse(new FileReader(refFile));
			
			// true positives: for each cell in the reference alignment
			// if it is in the corresponding bloom alignment, it is a true positive
//			System.out.println("\nTrue positives");
			
			Enumeration<Cell> refCells = refAlignment.getElements();
			while (refCells.hasMoreElements()) {
				
				Cell ref = refCells.nextElement();
				
				String uri1 = ref.getObject1AsURI().toString();
				String uri2 = ref.getObject2AsURI().toString();
				
				Enumeration<Cell> bloomsCells = bloomsAlignment.getElements();
				while (bloomsCells.hasMoreElements()) {
					
					Cell blooms = bloomsCells.nextElement();
					
					String uri3 = blooms.getObject1AsURI().toString();
					String uri4 = blooms.getObject2AsURI().toString();
					
					if ((uri1.equals(uri3) && uri2.equals(uri4)) || 
							(uri1.equals(uri4) && uri2.equals(uri3))) {
//						System.out.println("\t" + uri1 + " == " + uri2);
						truePositives++;
						break;
					}
				}
			}	
		
			// false negatives: for each cell in the reference alignment
			// if it is NOT in the corresponding blooms alignment, it is a false negative
//			System.out.println("\nFalse negatives");
			
			refCells = refAlignment.getElements();
			while (refCells.hasMoreElements()) {
				
				Cell ref = refCells.nextElement();
				
				String uri1 = ref.getObject1AsURI().toString();
				String uri2 = ref.getObject2AsURI().toString();
				
				boolean found = false;
				
				Enumeration<Cell> bloomsCells = bloomsAlignment.getElements();
				while (bloomsCells.hasMoreElements()) {
					
					Cell blooms = bloomsCells.nextElement();
					
					String uri3 = blooms.getObject1AsURI().toString();
					String uri4 = blooms.getObject2AsURI().toString();
					
					if ((uri1.equals(uri3) && uri2.equals(uri4)) || 
							(uri1.equals(uri4) && uri2.equals(uri3))) {
						found = true;
						break;
					}
				}
				
				if (!found) {
//					System.out.println("\t" + uri1 + " == " + uri2);
					falseNegatives++;
				}
			}	
			
			// false positives: for each cell in the *blooms* alignment
			// if it is NOT in the corresponding *reference* alignment, it is a false positive
//			System.out.println("\nFalse positives");
			
			Enumeration<Cell> bloomsCells = bloomsAlignment.getElements();
			while (bloomsCells.hasMoreElements()) {
				
				Cell blooms = bloomsCells.nextElement();
				
				String uri1 = blooms.getObject1AsURI().toString();
				String uri2 = blooms.getObject2AsURI().toString();
				
				boolean found = false;
				
				refCells = refAlignment.getElements();
				while (refCells.hasMoreElements()) {
					
					Cell ref = refCells.nextElement();
					
					String uri3 = ref.getObject1AsURI().toString();
					String uri4 = ref.getObject2AsURI().toString();
					
					if ((uri1.equals(uri3) && uri2.equals(uri4)) || 
							(uri1.equals(uri4) && uri2.equals(uri3))) {
						found = true;
						break;
					}
				}
				
				if (!found) {
//					System.out.println("\t" + uri1 + " == " + uri2);
					falsePositives++;
				}
			}	
		}
		
		double precision = truePositives / ((float) truePositives + falsePositives);
		double recall = truePositives / ((float) truePositives + falseNegatives);
		double fMeasure = 2 * precision * recall / (precision + recall);
		
		System.out.println();
		System.out.println("Precision = " + precision);
		System.out.println("Recall = " + recall);
		System.out.println("F-measure = " + fMeasure);
	}
	
	
	public static void checkTypes(File dir) throws Exception {
		
		int all = 0;
		int properties = 0;
		
		for (File f: dir.listFiles()) {

			String filename = f.getName();
			
			// skip all files that aren't reference alignments
			if (!(filename.contains("-") && 
					filename.substring(filename.lastIndexOf("-"), filename.indexOf(".rdf")).length() > 2)) 
				continue; 

			// get the filenames for the ontologies involved in this reference alignment
			String s1 = filename.substring(0, filename.indexOf("-")).trim();
			s1 += ".rdf";
			s1 = dir.getAbsolutePath() + "/" + s1;

			String s2 = filename.substring(filename.indexOf("-")+1).trim();
			s2 = dir.getAbsolutePath() + "/" + s2;

			System.out.println(filename + "   " + s1 + "   " + s2);
			
			// read in the reference alignment
			AlignmentParser parser = new AlignmentParser();
			Alignment refAlignment = parser.parse(new FileReader(f));
			
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			IRI iriA = IRI.create(new File(s1));
			OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
			
			IRI iriB = IRI.create(new File(s2));
			OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
			
			Enumeration<Cell> cells = refAlignment.getElements();
			while (cells.hasMoreElements()) {
				
				all++;
				
				Cell ans = cells.nextElement();
				
				URI uri1 = ans.getObject1AsURI();
				URI uri2 = ans.getObject2AsURI();
				
				System.out.println("\t" + uri1 + " == " + uri2);
				
				Set<OWLEntity> set1 = ontA.getEntitiesInSignature(IRI.create(uri1));
				Iterator<OWLEntity> results = set1.iterator();
				
				while (results.hasNext()) {
					if (!results.next().isOWLClass()) {
						System.out.println("\t\t" + uri1 + " is not a class");
						properties++;
					}
				}
				
				Set<OWLEntity> set2 = ontB.getEntitiesInSignature(IRI.create(uri2));
				results = set2.iterator();
				
				while (results.hasNext()) {
					if (!results.next().isOWLClass()) {
						System.out.println("\t\t" + uri2 + " is not a class");
					}
				} 
			}
			
			System.out.println(all + " matches; " + properties + " involve properties");
		}
	}
	
	
	public static void checkNamespace(File dir) throws Exception {
		
		for (File f: dir.listFiles()) {

			String filename = f.getName();
			
			// skip all files that aren't reference alignments
			if (!(filename.contains("-") && 
					filename.substring(filename.lastIndexOf("-"), filename.indexOf(".rdf")).length() > 2)) 
				continue; 

			// read in the reference alignment
			AlignmentParser parser = new AlignmentParser();
			Alignment refAlignment = parser.parse(new FileReader(f));
			
			Enumeration<Cell> cells = refAlignment.getElements();
			while (cells.hasMoreElements()) {
				
				Cell ans = cells.nextElement();
				
				URI uri1 = ans.getObject1AsURI();
				URI uri2 = ans.getObject2AsURI();
				double strength = ans.getStrength();
				
				System.out.println("\t" + uri1 + " == " + uri2);
				
				if (!uri1.toString().contains("oaei")) {
					System.out.println("\t\t" + uri1 + " is not in the OAEI namespace");
					System.exit(0);
				}
				
				if (strength != 1.0) {
					System.out.println("\t\t the strength of the match is not 1.0");
					System.exit(0);
				}
			}
		}
	}
	
	
	public static void testWikipediaSearchStability() throws Exception {

		StringUtils stringUtils = new StringUtils(false);
		WikipediaBLOOMS blooms = WikipediaBLOOMS.getInstance();	

		String s1 = "./data/benchmark/101.rdf";

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI iriA = IRI.create(new File(s1));
		OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
		String ns1 = StringUtils.getNamespace(iriA.toURI());
		
		double minSim = 1.1;
		double totalSim = 0.0;
		int count = 0;
		
		for (OWLEntity entityA: ontA.getSignature(false)) {

			// skip entities not in the namespace
			if (!entityA.toString().contains(ns1)) continue;

			// always skip instances
			if (entityA.isOWLNamedIndividual()) continue;

			String labelA = stringUtils.getString(entityA, ontA);
			System.out.println(labelA);
			
			ArrayList<HashSet<String>> sets = new ArrayList<HashSet<String>>();
			
			for (int i=0; i<10; i++) {
				sets.add(blooms.getArticles(labelA, 10));
			}
			
			// compute the set sim, find the max and average
			for (int i=0; i<10; i++) {
				for (int j=i+1; j<10; j++) {

					double sim = setSim(sets.get(i), sets.get(j));
					
					if (sim < minSim) minSim = sim;
					totalSim += sim;
					count++;
				}
			}
		}
		
		System.out.println("min sim: " + minSim);
		System.out.println("avg sim: " + (totalSim / count));
	}
	
	
	private static double setSim(HashSet<String> set1, HashSet<String> set2) {
		
		double intersection = 0;
		
		for (String s1: set1) {
			if (set2.contains(s1)) intersection++;
		}
		
		return intersection / (set1.size() + set2.size() - intersection);
	}
	
	
	public static void testWikipediaCreationDates() throws Exception {

		int count = 0;
		
		StringUtils stringUtils = new StringUtils(false);
		WikipediaBLOOMS blooms = WikipediaBLOOMS.getInstance();	

		String s1 = "./data/benchmark/101.rdf";

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI iriA = IRI.create(new File(s1));
		OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
		String ns1 = StringUtils.getNamespace(iriA.toURI());
		
		for (OWLEntity entityA: ontA.getSignature(false)) {

			// skip entities not in the namespace
			if (!entityA.toString().contains(ns1)) continue;

			// always skip instances
			if (entityA.isOWLNamedIndividual()) continue;

			String labelA = stringUtils.getString(entityA, ontA);
			System.out.println(labelA);
			
			HashSet<String> articles = blooms.getArticles(labelA, 10);
			
			for (String article: articles) {
				
				System.out.println(article);
				
				article = URLEncoder.encode(article, "UTF-8");
				
				String query = "https://en.wikipedia.org/w/index.php?title=" + 
						article.replaceAll(" ", "_") + "&action=info";
				
				InvokeWikipediaWebService invokeWS = new InvokeWikipediaWebService(query);
				String content = null;
				
				try {
					content = invokeWS.invokeWebService();
				} catch (Exception e) {
					System.out.println(e);
					continue;
				}
				
				int creationIndex = content.indexOf("Date of page creation");
				if (creationIndex < 0) continue;
				
				content = content.substring(creationIndex);
				
				int startIndex = content.indexOf("<a");
				if (startIndex < 0) continue;
				content = content.substring(startIndex);
				
				startIndex = content.indexOf(">");
				if (startIndex < 0) continue;
				content = content.substring(startIndex);
				
				int endIndex = content.indexOf("</a>");
				if (endIndex < 0) continue;
				
				content = content.substring(1, endIndex);
				
				String[] tokens = content.split("[ ]");
				System.out.println(tokens[tokens.length-1]);
				
				try {
					int year = Integer.parseInt(tokens[tokens.length-1]);
					if (year > 2010) {
						count++;
					}
				} catch (Exception e) {
					System.out.println(tokens[tokens.length-1]);
				}
			}
		}
		
		System.out.println(count + " after 2010");
	}
}
