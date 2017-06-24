package org.knoesis.blooms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;


/**
 * @author prateekjain
 * 
 */
public class WikipediaBLOOMS implements Serializable {

	private static final long serialVersionUID = -1341831556351810649L;

	private HashMap<String, ArrayList<BloomsTree>> cache = new HashMap<String, ArrayList<BloomsTree>>();
	private HashMap<String, HashSet<String>> articleCache = new HashMap<String, HashSet<String>>();
	private HashMap<String, HashSet<String>> categoryCache = new HashMap<String, HashSet<String>>();


	public static WikipediaBLOOMS getInstance() {
		WikipediaBLOOMS blooms = null;

		try {
			File cache = new File("wiki.out");
			if (!cache.exists()) {
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("wiki.out"));
				out.writeObject(blooms);
				out.close();
			} else {
				ObjectInputStream in = new ObjectInputStream(new FileInputStream("wiki.out"));
				blooms = (WikipediaBLOOMS) in.readObject();
				in.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (blooms == null) {
			blooms = new WikipediaBLOOMS();
		}

		return blooms;
	}


	public double getSubclassProbability(ArrayList<BloomsTree> sourceTrees, 
			ArrayList<BloomsTree> targetTrees) {

		double maxStrength = 0.0;

		for (BloomsTree treeA : sourceTrees) {
			for (BloomsTree treeB : targetTrees) {

				double prob = treeA.subclassProbability(treeB);
				
//				System.out.println("\t\t" + treeA.getRoot() + " and " + 
//					treeB.getRoot() + " = " + prob);
				
				if (prob > maxStrength) {
					maxStrength = prob;
				}
			}
		}

		return maxStrength;
	}
	
	
	public boolean areEquivalent(ArrayList<BloomsTree> sourceForest, 
			ArrayList<BloomsTree> targetForest) {

		for (BloomsTree treeA : sourceForest) {
			for (BloomsTree treeB : targetForest) {
				if (treeA.isEquivalent(treeB)) return true;
			}
		}

		return false;
	}

	
	public HashSet<String> getArticles(String phrase, int articleLimit) throws Exception {
		
		if (articleCache.containsKey(phrase)) {
			return articleCache.get(phrase);
		}
		
		System.out.println("cache miss on " + phrase);
		
		HashSet<String> articles = WikipediaOperations.getArticles(phrase, articleLimit);
		
		articleCache.put(phrase, articles);
		return articles;
	}
	
	
	public HashSet<String> getCategories(String phrase, int articleLimit, int catLimit) throws Exception {

		HashSet<String> categories = new HashSet<String>();

		if (categoryCache.containsKey(phrase)) {
			return categoryCache.get(phrase);
		}

		System.out.println("cache miss on " + phrase);

		// Get all articles related to the phrase
		HashSet<String> articles = getArticles(phrase, articleLimit);

		// For each article in the list,
		for (String article: articles) {
			
			int attempts = 0;
			boolean success = false;

			while (!success && attempts < 5) {
				
				try {
					// get all of the categories for that article
//					System.out.println("getting categories for " + article);
					HashSet<String> cats = WikipediaOperations.getCategories(article, catLimit);
					categories.addAll(cats);
//					System.out.println("\t" + cats);
					success = true;
					
				} catch (Exception e) {
					System.out.println(e.getMessage());
					System.out.println("Error querying wikipedia -- " + 
							(4 - attempts) + " more attempts");
				}
			}

			if (!success) {
				System.out.println("Failed to query wikipedia in 5 attempts, quitting");
				saveCache();
				System.exit(0);
			}

		}

		categoryCache.put(phrase, categories);
		return categories;
	}
	
	
	public ArrayList<BloomsTree> createBloomsForest(String phrase, int depth, 
			int articleLimit, int categoryLimit) {

		ArrayList<BloomsTree> forest = new ArrayList<BloomsTree>();

		if (cache.containsKey(phrase)) {
			return cache.get(phrase);
		}

		System.out.println("cache miss on " + phrase);
		
		// Get all articles related to the phrase
		HashSet<String> articleList = null;

		int attempts = 0;
		boolean success = false;

		while (!success && attempts < 5) {
			try {
				articleList = WikipediaOperations.getArticles(phrase, articleLimit);
				success = true;
			} catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println("Error querying wikipedia -- " + (4 - attempts) + " more attempts");
				attempts++;
			}
		}

		if (!success) {
			System.out.println("Failed to query wikipedia in 5 attempts, quitting");
			saveCache();
			System.exit(0);
		}

		// For each article in the list, create a tree
		for (String article: articleList) {
			forest.add(createBloomsTree(article, depth, categoryLimit));
		}

		cache.put(phrase, forest);
		return forest;
	}
	
	
	private boolean contains(DefaultMutableTreeNode r, DefaultMutableTreeNode n) {
		
		if (r == null) return false;
		
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> sourceNodes = 
				((DefaultMutableTreeNode) r).breadthFirstEnumeration();
		
		while (sourceNodes.hasMoreElements()) {
			DefaultMutableTreeNode c = sourceNodes.nextElement();
			if (c.toString().equals(n.toString())) 
				return true;
		}
		
		return false;
	}
	
	
	private BloomsTree createBloomsTree(String article, int depth, int catLimit) {
		
		DefaultMutableTreeNode articleNode = new DefaultMutableTreeNode(article);
		BloomsTree tree = new BloomsTree(articleNode);
		
		DefaultMutableTreeNode r = (DefaultMutableTreeNode) tree.getRoot();
		for (int i = 1; i < depth; i++) {
			
			// at first the root is the only leaf, but this should still work
			List<DefaultMutableTreeNode> bloomsNodeList = tree.getLeaves();
			
			for (DefaultMutableTreeNode n : bloomsNodeList) {
				// For each leaf node, get the categories and add them to the tree
				String phrase = (String) n.getUserObject();
				if (i > 1) phrase = "category:" + phrase;
				constructCategoryTree(n, phrase, r, catLimit);
			}
		}
		
		return tree;
	}


	private void constructCategoryTree(DefaultMutableTreeNode n, String phrase, 
			DefaultMutableTreeNode root, int catLimit) {

		int attempts = 0;
		boolean success = false;

		while (!success && attempts < 5) {
			
			try {
				
				// get all of the categories for this phrase
				HashSet<String> categoryList = WikipediaOperations.getCategories(phrase, catLimit);

				// add each category to the tree under node n (the root is used only
				// to check for duplicates)
				for (String category: categoryList)
					addChildrenToParent(category, n, root);
					
				success = true;
				
			} catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println("Error querying wikipedia -- " + (4 - attempts) + " more attempts");
				attempts++;
			}
		}

		if (!success) {
			System.out.println("Failed to query wikipedia in 5 attempts, quitting");
			saveCache();
			System.exit(0);
		}
	}

	
	// adds the category as a child node n iff the category is not already in the tree
	private void addChildrenToParent(String category, DefaultMutableTreeNode n, DefaultMutableTreeNode root) {
		
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(category);
		if (!contains(root, newNode)) {
			n.add(newNode);
			newNode.setParent(n);
		}	
	}

	
	public void saveCache() {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("wiki.out"));
			out.writeObject(this);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		
		WikipediaBLOOMS blooms = WikipediaBLOOMS.getInstance();
		
		ArrayList<BloomsTree> test = blooms.createBloomsForest("bicycle", 2, 5, 5);
		System.out.println(test);
		System.exit(0);
		
		ArrayList<BloomsTree> conference = blooms.createBloomsForest("Collection", 4, 5, 10);
		ArrayList<BloomsTree> unpublished = blooms.createBloomsForest("List", 4, 5, 10);

		System.out.println(conference.size() + " trees for Collection");
		System.out.println(unpublished.size() + " trees for List");
		
		for(BloomsTree tree: conference) {
			System.out.println(tree.getRoot().toString());
		}

		for(BloomsTree tree: unpublished) {
			System.out.println(tree.getRoot().toString());
		}
		
		double probAsubB = blooms.getSubclassProbability(conference, unpublished);
		double probBsubA = blooms.getSubclassProbability(unpublished, conference);

		System.out.println("sim(a, b) = " + probAsubB);
		System.out.println("sim(b, a) = " + probBsubA);
		
		if (probAsubB > probBsubA)
			System.out.println("Collection" + " > " + "List" + " : " + probBsubA);
		else if (probAsubB < probBsubA)
			System.out.println("List" + " > " + "Collection" + " : " + probAsubB);
		else if (probAsubB == probBsubA)
			System.out.println("Collection" + " = " + "List" + " : " + probBsubA);

		blooms.saveCache();
	}
}
