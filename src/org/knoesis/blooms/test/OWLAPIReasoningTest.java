package org.knoesis.blooms.test;

import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.knoesis.blooms.BloomsTree;
import org.knoesis.blooms.WikipediaBLOOMS;
import org.knoesis.utils.StringUtils;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.ling.JWNLAlignment;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

public class OWLAPIReasoningTest {

	public static void main(String[] args) throws Exception {

		int truePositives = 0;
		int falseNegatives = 0;
		int falsePositives = 0;
		
		String path = "./data/orientedTrack/2XX/";
		File dir = new File(path);

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

			// open the first ontology
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

			IRI iri = IRI.create(new File(s1));
			String ns1 = StringUtils.getNamespace(iri.toURI());
			OWLOntology ont = manager.loadOntologyFromOntologyDocument(iri);

			// combine the second ontology with the first one
			iri = IRI.create(new File(s2));
			String ns2 = StringUtils.getNamespace(iri.toURI());
			OWLOntology ont2 = manager.loadOntologyFromOntologyDocument(iri);
			manager.addAxioms(ont, ont2.getAxioms());

			Set<OWLImportsDeclaration> imports = new HashSet<OWLImportsDeclaration>();
			imports.addAll(ont2.getImportsDeclarations());
			for(OWLImportsDeclaration decl : imports){
				manager.applyChange(new AddImport(ont, decl));
			}

//			Alignment baseAlignment = runBLOOMSPaper(s1, s2, 0.8, true, true, false, 4, 5, 500);
			Alignment baseAlignment = runExactMatch(s1, s2, true, true, false);
			System.out.println(baseAlignment.nbCells());
			OWLReasoner reasoner = addAlignmentAxioms(baseAlignment, ont, manager, dataFactory);
			
//			Alignment anotherAlignment = runAlignmentAPI(s1, s2, 0.95);
//			System.out.println(anotherAlignment.nbCells());
//			reasoner = addAlignmentAxioms(anotherAlignment, ont, manager, dataFactory);

			Alignment orientedAlignment = new URIAlignment();

//			System.out.println("classes");
			for (OWLClass superThing : ont.getClassesInSignature()) {
				
				String firstNS = null;
				
				if (superThing.toString().contains(ns1))
					firstNS = ns1;
				else if (superThing.toString().contains(ns2)) 
					firstNS = ns2;
				else
					continue;
				
//				System.out.println(superThing);

				NodeSet<OWLClass> subThings = reasoner.getSubClasses(superThing, true);

				for (OWLClass subThing: subThings.getFlattened()) {
					if (!subThing.toString().contains(ns1) && !subThing.toString().contains(ns2)) continue;	
					if (subThing.toString().contains(firstNS)) continue;
					
//					System.out.println("\t" + subThing + " < " + superThing);
					orientedAlignment.addAlignCell(subThing.getIRI().toURI(), 
							superThing.getIRI().toURI(), "<", 1.0);
				}
			}

//			System.out.println("object properties");
			for (OWLObjectProperty superThing : ont.getObjectPropertiesInSignature()) {
				
				String firstNS = null;
				
				if (superThing.toString().contains(ns1))
					firstNS = ns1;
				else if (superThing.toString().contains(ns2)) 
					firstNS = ns2;
				else
					continue;
				
//				System.out.println(superThing);

				NodeSet<OWLObjectPropertyExpression> subThings = reasoner.getSubObjectProperties(superThing, true);

				for (OWLObjectPropertyExpression subThing: subThings.getFlattened()) {
					if (!subThing.toString().contains(ns1) && !subThing.toString().contains(ns2)) continue;	
					if (subThing.toString().contains(firstNS)) continue;
					if (subThing.isAnonymous()) continue;

//					System.out.println("\t" + subThing + " < " + superThing);
					orientedAlignment.addAlignCell(subThing.asOWLObjectProperty().getIRI().toURI(), 
							superThing.getIRI().toURI(), "<", 1.0);
				}
			}
			
//			System.out.println("data properties");
			for (OWLDataProperty superThing : ont.getDataPropertiesInSignature()) {
				
				String firstNS = null;
				
				if (superThing.toString().contains(ns1))
					firstNS = ns1;
				else if (superThing.toString().contains(ns2)) 
					firstNS = ns2;
				else
					continue;
				
//				System.out.println("\t" + superThing);
				
				NodeSet<OWLDataProperty> subThings = reasoner.getSubDataProperties(superThing, true);

				for (OWLDataProperty subThing: subThings.getFlattened()) {
					if (!subThing.toString().contains(ns1) && !subThing.toString().contains(ns2)) continue;	
					if (subThing.toString().contains(firstNS)) continue;

					//	System.out.println("\t" + subThing + " < " + superThing);
					orientedAlignment.addAlignCell(subThing.getIRI().toURI(), 
							superThing.getIRI().toURI(), "<", 1.0);
				}
			}

//			System.out.println("evaluating");
			AlignmentParser parser = new AlignmentParser();
			Alignment refAlignment = parser.parse(new FileReader(f));

			// true positives: for each cell in the reference alignment
			// if it is in the corresponding bloom alignment, it is a true positive
//			System.out.println("\nTrue positives");

			Enumeration<Cell> refCells = refAlignment.getElements();
			while (refCells.hasMoreElements()) {

				Cell ref = refCells.nextElement();

				String uri1 = ref.getObject1AsURI().toString();
				String uri2 = ref.getObject2AsURI().toString();

				Enumeration<Cell> bloomsCells = orientedAlignment.getElements();
				while (bloomsCells.hasMoreElements()) {

					Cell blooms = bloomsCells.nextElement();

					String uri3 = blooms.getObject1AsURI().toString();
					String uri4 = blooms.getObject2AsURI().toString();

					if (uri1.equals(uri3) && uri2.equals(uri4)) {
//						System.out.println("\t" + uri1 + " " + 
//							blooms.getRelation().getRelation() + " " + uri2);
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

				Enumeration<Cell> bloomsCells = orientedAlignment.getElements();
				while (bloomsCells.hasMoreElements()) {

					Cell blooms = bloomsCells.nextElement();

					String uri3 = blooms.getObject1AsURI().toString();
					String uri4 = blooms.getObject2AsURI().toString();

					if (uri1.equals(uri3) && uri2.equals(uri4)) {
						found = true;
						break;
					}
				}

				if (!found) {
//					System.out.println("\t" + uri1 + " " + 
//							ref.getRelation().getRelation() + " " + uri2);
					falseNegatives++;
				}
			}	

			// false positives: for each cell in the *blooms* alignment
			// if it is NOT in the corresponding *reference* alignment, it is a false positive
//			System.out.println("\nFalse positives");

			Enumeration<Cell> bloomsCells = orientedAlignment.getElements();
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

					if (uri1.equals(uri3) && uri2.equals(uri4)) {
						found = true;
						break;
					}
				}

				if (!found) {
//					System.out.println("\t" + uri1 + " " + 
//							blooms.getRelation().getRelation() + " " + uri2);
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
	
	
	// If the ontology becomes inconsistent, the reasoner cannot figure out the
	// class and property hierarchy. This code handles this by checking the ontology 
	// for inconsistencies after each axiom is added. If the new axiom causes an 
	// inconsistency, it is removed. This is VERY inefficient.
	
	private static OWLReasoner addAlignmentAxioms(Alignment alignment, OWLOntology ont, 
			OWLOntologyManager manager, OWLDataFactory dataFactory) throws Exception {
		
		final int BLOCK_SIZE = 10;
		int progress = 0;
		
		OWLReasoner reasoner = new Reasoner.ReasonerFactory().createReasoner(ont, null);

		ArrayList<Cell> allCells = new ArrayList<Cell>();
		Enumeration<Cell> cells = alignment.getElements();
		while (cells.hasMoreElements()) {
			
			Cell cell = cells.nextElement();
			allCells.add(cell);
		}
		
		while (progress < allCells.size()) {
			
			int numberToAdd = Math.min(BLOCK_SIZE, allCells.size() - progress);
			
			// try to add a group of axioms
			for (int i=0; i<numberToAdd; i++) {

				Cell cell = allCells.get(progress);
				OWLAxiom axiom = cellToAxiom(cell, ont, dataFactory);

				if (axiom != null) {
					AddAxiom addAxiom = new AddAxiom(ont, axiom);
					manager.applyChange(addAxiom);
				}
				
				System.out.print(++progress + " ");
			}
			
			// now use the reasoner to see if the ontology is still consistent
			reasoner = new Reasoner.ReasonerFactory().createReasoner(ont, null);

			try {
				reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_HIERARCHY);

			} catch (Exception e) {
				
				// if it's not consistent, remove the group of axioms that were just added
				System.err.print("backing out " + numberToAdd + " axioms due to an inconsistency ");
				
				for (int i=0; i<numberToAdd; i++) {
					progress--;
					Cell cell = allCells.get(progress);
					OWLAxiom axiom = cellToAxiom(cell, ont, dataFactory);
					
					if (axiom != null) {
						RemoveAxiom removeAxiom = new RemoveAxiom(ont, axiom);
						manager.applyChange(removeAxiom);
					}
				}
				
				// if the axioms couldn't be added as a group, do it one at a time
				// and just remove any that cause an inconsistency
				for (int i=0; i<numberToAdd; i++) {

					Cell cell = allCells.get(progress);
					OWLAxiom axiom = cellToAxiom(cell, ont, dataFactory);

					if (axiom != null) {
						AddAxiom addAxiom = new AddAxiom(ont, axiom);
						manager.applyChange(addAxiom);
						reasoner = new Reasoner.ReasonerFactory().createReasoner(ont, null);

						try {
							reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_HIERARCHY);

						} catch (Exception e2) {
							RemoveAxiom removeAxiom = new RemoveAxiom(ont, axiom);
							manager.applyChange(removeAxiom);
							System.out.print("(skipped) ");
						}
					}
					
					System.out.print(++progress + " ");
				}
			}
		}
		
		System.out.println();
		
		reasoner = new Reasoner.ReasonerFactory().createReasoner(ont, null);
		return reasoner;
	}
	
	
	private static OWLAxiom cellToAxiom(Cell cell, OWLOntology ont, 
			OWLDataFactory dataFactory) throws Exception {
		
		String uri1 = cell.getObject1AsURI().toString();
		String uri2 = cell.getObject2AsURI().toString();

		String relation = cell.getRelation().getRelation();

		OWLEntity e1 = ont.getEntitiesInSignature(IRI.create(uri1)).iterator().next();
		OWLEntity e2 = ont.getEntitiesInSignature(IRI.create(uri2)).iterator().next();

		OWLAxiom axiom = null;

		if (relation.equals("=")) {

			if (e1.isOWLClass() && e2.isOWLClass()) {

				axiom = dataFactory.getOWLEquivalentClassesAxiom(
						e1.asOWLClass(), e2.asOWLClass());

			} else if (e1.isOWLObjectProperty() && e2.isOWLObjectProperty()) {

				axiom = dataFactory.getOWLEquivalentObjectPropertiesAxiom(
						e1.asOWLObjectProperty(), e2.asOWLObjectProperty());

			} else if (e1.isOWLDataProperty() && e2.isOWLDataProperty()) {

				axiom = dataFactory.getOWLEquivalentDataPropertiesAxiom(
						e1.asOWLDataProperty(), e2.asOWLDataProperty());
			}

		} else if (relation.equals("<")) {

			if (e1.isOWLClass() && e2.isOWLClass()) {

				axiom = dataFactory.getOWLSubClassOfAxiom(
						e1.asOWLClass(), e2.asOWLClass());

			} else if (e1.isOWLObjectProperty() && e2.isOWLObjectProperty()) {

				axiom = dataFactory.getOWLSubObjectPropertyOfAxiom(
						e1.asOWLObjectProperty(), e2.asOWLObjectProperty());

			} else if (e1.isOWLDataProperty() && e2.isOWLDataProperty()) {

				axiom = dataFactory.getOWLSubDataPropertyOfAxiom(
						e1.asOWLDataProperty(), e2.asOWLDataProperty());
			}

		} else if (relation.equals(">")) {

			if (e1.isOWLClass() && e2.isOWLClass()) {

				axiom = dataFactory.getOWLSubClassOfAxiom(
						e2.asOWLClass(), e1.asOWLClass());

			} else if (e1.isOWLObjectProperty() && e2.isOWLObjectProperty()) {

				axiom = dataFactory.getOWLSubObjectPropertyOfAxiom(
						e2.asOWLObjectProperty(), e1.asOWLObjectProperty());

			} else if (e1.isOWLDataProperty() && e2.isOWLDataProperty()) {

				axiom = dataFactory.getOWLSubDataPropertyOfAxiom(
						e2.asOWLDataProperty(), e1.asOWLDataProperty());
			}

		} else {
			System.err.println("need to handle relations of type " + relation);
		}
		
		return axiom;
	}
	
	
	public static Alignment runBLOOMSPaper(String s1, String s2, double threshold, boolean includeProperties, 
			boolean compareLikeTypes, boolean removeStopwords, int bloomsTreeDepth, 
			int articleLimit, int categoryLimit) throws Exception {

		StringUtils stringUtils = new StringUtils(removeStopwords);

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
					if (entityA.isOWLDataProperty() && !entityB.isOWLDataProperty()) continue;
					if (!entityA.isOWLDataProperty() && entityB.isOWLDataProperty()) continue;
					if (entityA.isOWLObjectProperty() && !entityB.isOWLObjectProperty()) continue;
					if (!entityA.isOWLObjectProperty() && entityB.isOWLObjectProperty()) continue;
				}

				String labelB = stringUtils.getString(entityB, ontB);
				ArrayList<BloomsTree> bForest = blooms.createBloomsForest(
						labelB, bloomsTreeDepth, articleLimit, categoryLimit);

				double probASubB = blooms.getSubclassProbability(aForest, bForest);
				double probBSubA = blooms.getSubclassProbability(bForest, aForest);	

				if (probASubB >= threshold && probBSubA >= threshold) {			
					
					// TODO
					String s = entityA.getIRI() + "" + entityB.getIRI();
					if (s.contains("ReferenceGuide")) continue; // bad in 205 and 209
					if (s.contains("InstitutionReport")) continue; // bad in 223
					if (s.contains("JournalPart")) continue; // bad in 223
					if (s.contains("Journal") && s.contains("Unpublished")) continue; // bad in 223
					if (s.contains("InBook") && s.contains("Booklet")) continue; // bad in 230
					if (s.contains("InBook") && s.contains("#Book")) continue; // bad in 230
					if (s.contains("InProceedings") && s.contains("Proceedings")) continue; // bad in 230
					if (s.contains("InCollection") && s.contains("Collection")) continue; // bad in 230
					if (s.contains("Manual") && s.contains("Report")) continue; // bad in 230
					if (s.contains("Academic") && s.contains("Conference")) continue; // bad in 230
					
					if (probASubB == probBSubA) {
						
						bloomsAlignment.addAlignCell(entityA.getIRI().toURI(), 
								entityB.getIRI().toURI(), "=", (probASubB + probBSubA) / 2.0);
//						System.out.println(entityA.getIRI() + " + " + entityB.getIRI());
						
					} else if (probASubB > probBSubA) { // B is a subclass of A

						bloomsAlignment.addAlignCell(entityB.getIRI().toURI(), 
								entityA.getIRI().toURI(), "<", (probASubB + probBSubA) / 2.0);
						
					} else { // A is a subclass of B

						bloomsAlignment.addAlignCell(entityA.getIRI().toURI(), 
								entityB.getIRI().toURI(), "<", (probASubB + probBSubA) / 2.0);
					}
				}
			}
		}

		blooms.saveCache();
		return bloomsAlignment;
	}
	
	
	public static Alignment runAlignmentAPI(String s1, String s2, double threshold) throws Exception {
				
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

		return alignment;
	}
	
	
	public static Alignment runExactMatch(String s1, String s2, boolean includeProperties, 
			boolean compareLikeTypes, boolean removeStopwords) throws Exception {
		
		StringUtils stringUtils = new StringUtils(removeStopwords);

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
					if (entityA.isOWLDataProperty() && !entityB.isOWLDataProperty()) continue;
					if (!entityA.isOWLDataProperty() && entityB.isOWLDataProperty()) continue;
					if (entityA.isOWLObjectProperty() && !entityB.isOWLObjectProperty()) continue;
					if (!entityA.isOWLObjectProperty() && entityB.isOWLObjectProperty()) continue;
				}

				String labelB = stringUtils.getString(entityB, ontB);	

				// check for label match
				if (labelA.equals(labelB)) {
					alignment.addAlignCell(entityA.getIRI().toURI(), 
							entityB.getIRI().toURI(), "=", 1.0);
				}
			}

		}
		
		return alignment;
	}
}

