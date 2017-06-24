package org.knoesis.blooms;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.knoesis.utils.WordNet;


public class BloomsTree extends DefaultTreeModel {

	private static final long serialVersionUID = -7861651506694916481L;
	
	
	public BloomsTree(DefaultMutableTreeNode root) {
		super(root);
	}


	public List<DefaultMutableTreeNode> getLeaves() {
		
		List<DefaultMutableTreeNode> leaves = new ArrayList<DefaultMutableTreeNode>();
		
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> nodes = 
			((DefaultMutableTreeNode) root).breadthFirstEnumeration();
		
		while (nodes.hasMoreElements()) {
			TreeNode node = nodes.nextElement();
			if (node.isLeaf()) {
				leaves.add((DefaultMutableTreeNode) node);
			}
		}
		
		return leaves;
	}
	
	
	// Here I'm going to try what Prateek did in his code, which is different from  
	// what the paper describes
	public boolean isEquivalent(BloomsTree other) {
		
		// extra stuff begin
		if (this.getRoot().toString().equals(other.getRoot().toString())) return true;
		// extra stuff end
		
		for(int i = 0; i < ((DefaultMutableTreeNode) this.getRoot()).getChildCount(); i++) {

			String s1 = ((DefaultMutableTreeNode) this.getRoot()).getChildAt(i).toString();

			for(int j = 0; j < ((DefaultMutableTreeNode) other.getRoot()).getChildCount(); j++) {
				
				String s2 = ((DefaultMutableTreeNode) other.getRoot()).getChildAt(j).toString();
				
				if (s1.toLowerCase().equals(s2.toLowerCase())) {
					return true;
					
				} else {
					
					// check wordnet to see if these two labels are synonyms
					String temp1 = s1.replaceAll("Category:", "");
					String temp2 = s2.replaceAll("Category:", "");
					
					if (temp1.contains("ambiguation") || temp2.contains("ambiguation")) continue;
					
					HashSet<String> aSynonyms = WordNet.getInstance().getSynonyms(temp1);
					HashSet<String> bSynonyms = WordNet.getInstance().getSynonyms(temp2);
					for (String a: aSynonyms) {
						if (bSynonyms.contains(a)) {
							System.out.println("wordnet match on " + temp1 + " and "
									+ temp2 + " ("+ a + ")");
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}
	
	
	public double subclassProbability(BloomsTree other) {
		
		int common = 0;
		
		DefaultMutableTreeNode thisTreeCopy = copyTree((DefaultMutableTreeNode) this.getRoot());
		DefaultMutableTreeNode otherTreeCopy = copyTree((DefaultMutableTreeNode) other.getRoot());
		
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> sourceNodes = 
				((DefaultMutableTreeNode) thisTreeCopy.getRoot()).breadthFirstEnumeration();
		
		while (sourceNodes.hasMoreElements()) {
			DefaultMutableTreeNode sourceNode = sourceNodes.nextElement();
			
			@SuppressWarnings("unchecked")
			Enumeration<DefaultMutableTreeNode> targetNodes = 
					((DefaultMutableTreeNode) otherTreeCopy.getRoot()).breadthFirstEnumeration();
			
			while (targetNodes.hasMoreElements()) {
				DefaultMutableTreeNode targetNode = targetNodes.nextElement();
				
				if (sourceNode.toString().equals(targetNode.toString())) {
					sourceNode.removeAllChildren();
					targetNode.removeAllChildren();
					common++;
				}
			}
		}
		
		int count = 0;
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> temp = 
				((DefaultMutableTreeNode) thisTreeCopy.getRoot()).breadthFirstEnumeration();
		
		while (temp.hasMoreElements()) {
			count++;
			temp.nextElement();
		}
		
		if (common == 0) {
			return 0.0;
		}
		
		// the only common node was the root of both trees -- they are the same tree
		if (!(thisTreeCopy.getRoot().children().hasMoreElements() || 
				otherTreeCopy.getRoot().children().hasMoreElements())) {
			return 1.0;
		}
		
		if (count-1 <= 0) {
			return 0.0;
		}
		
		double sim = (double) (common / (float) (count - 1));
				
		assert sim > 1: "Bad tree similarity: " + sim;
		
		return sim;
	}


	public String toString() {

		String result = "";
		
		@SuppressWarnings("unchecked")
		Enumeration<DefaultMutableTreeNode> nodes = 
			((DefaultMutableTreeNode) root).breadthFirstEnumeration();

		while (nodes.hasMoreElements()) {
			TreeNode node = nodes.nextElement();
			result += node.toString() + ", ";
		}
		
		// remove last comma
		result = result.substring(0, result.length()-2);

		return result;
	}
	
	
	@SuppressWarnings("unchecked")
	public static DefaultMutableTreeNode copyTree(DefaultMutableTreeNode originalTreeNode)
	{
	    if (originalTreeNode == null)
	    {
	        return null;
	    }

	    // copy current node's data
	    DefaultMutableTreeNode copiedNode = new DefaultMutableTreeNode(originalTreeNode.toString());

	    // copy current node's children
	    Enumeration<DefaultMutableTreeNode> kids = originalTreeNode.children();
	    while (kids.hasMoreElements()) {
	    	DefaultMutableTreeNode kid = kids.nextElement();
	    	DefaultMutableTreeNode kidCopy = copyTree(kid);
	    	copiedNode.add(kidCopy);
	    	kidCopy.setParent(copiedNode);
	    }

	    return copiedNode;
	}

}