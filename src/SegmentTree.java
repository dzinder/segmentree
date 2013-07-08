/* Stores a list of Segmentes that have sampled during the course of the simulation */

import java.util.*;
import java.io.*;

public class SegmentTree {

	// fields
	private static Segment root = Parameters.urSegment;	
	private static List<Segment> tips = new ArrayList<Segment>();

	static final Comparator<Segment> descendantOrder = new Comparator<Segment>() {
		public int compare(Segment v1, Segment v2) {
			Integer descendantsV1 = new Integer(getNumberOfDescendants(v1));
			Integer descendantsV2 = new Integer(getNumberOfDescendants(v2));
			return descendantsV1.compareTo(descendantsV2);
		}
	};	

	// static methods
	public static void add(Segment s) {		
		tips.add(s);
	}
	public static void clear() {
		tips.clear();
	}
	public static List<Segment> getTips() {
		return tips;
	}
	public static Segment getRoot() {
		return root;
	}

	// return a random tip that lies between year from and year to
	public static List<Segment> getRandomTipsFromTo(float from, float to) {

		if (Parameters.sampleWholeGenomes) {
			// fill temporary list with first loci segments
			List<Integer> select = new ArrayList<Integer>();
			for (int i=0; i<tips.size();i+=Parameters.nSegments) {
				float x = tips.get(i).getBirth();
				assert (tips.get(i).getLoci()==1);
				if (x >= from && x < to) {
					select.add(i);					
				}
			}

			// pull random whole genome from this list			
			ArrayList<Segment> singleGenomeSegmentList = new ArrayList<Segment>();			
			if (select.size() > 0) {	
				int selectIndex =select.get(Random.nextInt(0,select.size()-1));
				for (int i=0;i<Parameters.nSegments;i++) {
					singleGenomeSegmentList.add(tips.get(selectIndex+i));
				}
			}

			return singleGenomeSegmentList;
		}
		else {
			// fill temporary list
			List<Segment> select = new ArrayList<Segment>();
			for (Segment s : tips) {
				float x = s.getBirth();
				if (x >= from && x < to && (s.getLoci()==1)) {
					select.add(s);
				}
			}

			// pull random segment from this list
			Segment rS = null;
			if (select.size() > 0) {	
				rS = select.get(Random.nextInt(0,select.size()-1));
			}
			ArrayList<Segment> singleSegmentList = new ArrayList<Segment>();
			singleSegmentList.add(rS);
			return singleSegmentList;
		}
	}

	// work backwards for each sample filling the children lists
	public static void fillBackward() {

		for (Segment child : tips) {
			Segment parent = child.getParent();
			while (parent != null) {
				parent.addChild(child);
				parent.incrementCoverage();
				child = parent;
				parent = child.getParent();
			}
		}

	}

	// work backwards for each sample filling the children lists
	public static void fillBackwardHostViruses(HostPopulation hp) {

		for (Host h : hp.getIs()) {
			for (Virus v : h.getInfections()) {
				for (Segment child : v.getSegments()) {
					child.markActive();
					Segment parent = child.getParent();
					while (parent != null) {
						parent.addChild(child);						
						child = parent;
						parent = child.getParent();
					}
				}

			}
		}
	}


	// work backwards for each sample filling the children lists
	public static void removeBackward() {

		for (Segment child : tips) {
			Segment parent = child.getParent();
			while (parent != null) {
				parent.removeChild(child);		
				parent.decrementCoverage();
				child = parent;
				parent = child.getParent();
			}
		}

	}

	// work backwards for each sample filling the children lists
	public static void removeBackwardHostViruses(HostPopulation hp) {

		for (Host h : hp.getIs()) {
			for (Virus v : h.getInfections()) {
				for (Segment child : v.getSegments()) {
					child.unmarkActive();
					Segment parent = child.getParent();
					while (parent != null) {
						parent.removeChild(child);				
						child = parent;
						parent = child.getParent();
					}
				}

			}
		}
	}

	// marking to by time, not proportional to prevalence
	public static void markTips() {
		// mark tips
		for (float i = 0; i < Parameters.getDate(); i+=0.1) {
			List<Segment> segmentList = getRandomTipsFromTo(i,i+(float)Parameters.intervalForMarkTips);
			for (Segment s : segmentList) {
				if (s != null) {
					while (s.getParent() != null) {
						s.mark();
						s = s.getParent();
					}
				}
			}
		}
	}

	// prune tips
	public static void pruneTips() {
		List<Segment> reducedTips = new ArrayList<Segment>();
		double keepProportion = (double) Parameters.treeProportion;

		if (Parameters.sampleWholeGenomes) {
			// sample tips in whole-genome chuncks 
			for (int i=0;i<tips.size();i+=Parameters.nSegments) {				
				if (Random.nextBoolean(keepProportion)) {
					for (int j=0;j<Parameters.nSegments;j++) {
						reducedTips.add(tips.get(i+j));
					}
				}
			}
		}
		else {
			// sample tips according to keepProportion
			for (Segment s : tips) {
				if (Random.nextBoolean(keepProportion)) {
					reducedTips.add(s);
				}
			}
		}

		tips = reducedTips;

	}

	// returns virus s and all its descendents via a depth-first traversal
	public static List<Segment> postOrderNodes(Segment r) {

		List<Segment> descendantsAndRoot = new ArrayList<Segment>();
		descendantsAndRoot.add(r);

		Stack<Segment> S = new Stack<Segment>();	
		Segment u;
		S.push(r);
		while (!S.isEmpty()) {
			u = S.pop();
			for (Segment s : u.getChildren()) {
				descendantsAndRoot.add(s);
				S.push(s);                
			}
		}    
		return descendantsAndRoot;


	}

	// Count total descendents of a Segment, working through its children and its children's children
	public static int getNumberOfDescendants(Segment r) {

		int numberOfDescendants = 0;

		Stack<Segment> S = new Stack<Segment>();
		Segment u;

		S.push(r);
		while (!S.isEmpty()) {
			u = S.pop();
			for (Segment s : u.getChildren()) {
				numberOfDescendants+=1;
				S.push(s);                
			}
		}        

		return numberOfDescendants;
	}

	public static int getNumberOfDescendants() {
		return getNumberOfDescendants(root);
	}

	// sorts children lists so that first member is child with more descendents than second member
	public static void sortChildrenByDescendants(Segment r) {

		Collections.sort(r.getChildren(), descendantOrder);

		Stack<Segment> S = new Stack<Segment>();
		Segment u;

		S.push(r);
		while (!S.isEmpty()) {
			u = S.pop();
			for (Segment s : u.getChildren()) {
				Collections.sort(s.getChildren(), descendantOrder);
				S.push(s);                
			}
		}        
	}	

	public static void sortChildrenByDescendants() {
		sortChildrenByDescendants(root);
	}

	// sets Segment layout based on a postorder traversal
	public static void setLayoutByDescendants() {

		List<Segment> vNodes = postOrderNodes(root);

		// set layout of tips based on traversal
		float y = 0;
		for (Segment s : vNodes) {
			//			if (tips.contains(s)) {
			if (s.isTip()) {
				s.setLayout(y);
				y++;
			}
		}

		// update layout of internal nodes
		Collections.reverse(vNodes);
		for (Segment s : vNodes) {
			if (s.getNumberOfChildren() > 0) {
				float mean = 0;
				for (Segment child : s.getChildren()) {
					mean += child.getLayout();
				}
				mean /= s.getNumberOfChildren();
				s.setLayout(mean);
			}
		}

	}	

	// looks at a virus and its grandparent, if traits are identical and there is no branching
	// then make virus child rather than grandchild
	// returns s.parent after all is said and done
	public static Segment collapse(Segment s) {

		Segment sp = null;
		Segment vgp = null;
		if (s.getParent() != null) {
			sp = s.getParent();
			if (sp.getParent() != null) {
				vgp = sp.getParent();
			}
		}

		if (sp != null && vgp != null) {
			if (sp.getNumberOfChildren() == 1 && (s.getSegmentNumber()==sp.getSegmentNumber()) && (s.isTrunk() == sp.isTrunk()) && (!s.isActive())) {

				List<Segment> vgpChildren = vgp.getChildren();
				int vpIndex =  vgpChildren.indexOf(sp);

				if (vpIndex >= 0) {

					// replace virus as child of grandparent
					vgpChildren.set(vpIndex, s);

					// replace grandparent as parent of virus
					s.setParent(vgp);

					// erase parent
					sp = null;

				}

			}
		}

		return s.getParent();

	}

	// walks backward using the list of tips, collapsing where possible
	public static void streamline() {

		for (Segment s : tips) {
			Segment sp = s;
			while (sp != null) {
				sp = collapse(sp);
			}
		}

	}


	// walks backward using the list of segments in the infected population collapsing where possible
	public static void streamlineHostViruses(HostPopulation hp) {
		for (Host h : hp.getIs()) {
			for (Virus v : h.getInfections()) {
				for (Segment s : v.getSegments()) {
					Segment sp = s;
					while (sp != null) {
						sp = collapse(sp);
					}					
				}
			}		
		}

	}

	public static void printTips() {

		try {
			File tipFile = new File("out.tips");
			tipFile.delete();
			tipFile.createNewFile();
			PrintStream tipStream = new PrintStream(tipFile);
			tipStream.printf("{\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"}\n", "name", "wholegenome", "year", "trunk", "tip", "mark", "hostAge", "layout","segmentID","loci");							
			for (int i = 0; i < tips.size(); i++) {
				Segment s = tips.get(i);		
				tipStream.printf("{\"%s\",%d,%.4f,%d,%d,%d,%.4f,%.4f,%s,%d}\n", s, s.getWholeGenomeID(),s.getBirth(), s.isTrunk()?1:0, s.isTip()?1:0, s.isMarked()?1:0,s.getHostAge(), s.getLayout(), s.getSegmentName(),s.getLoci());
			}
			tipStream.close();
		} catch(IOException ex) {
			System.out.println("Could not write to file"); 
			System.exit(0);
		}

	}

	public static void printBranches() {

		try {
			File branchFile = new File("out.branches");
			branchFile.delete();
			branchFile.createNewFile();
			PrintStream branchStream = new PrintStream(branchFile);			
			for (Segment s : postOrderNodes(root)) {
				if (s.getParent() != null) {
					Segment sp = s.getParent();
					branchStream.printf("{\"%s\",%d,%.3f,%d,%d,%d,%.4f,%.3f,%s,%d}\t", s, s.getWholeGenomeID(), s.getBirth(), s.isTrunk()?1:0, s.isTip()?1:0, s.isMarked()?1:0,  s.getHostAge(), s.getLayout(), s.getSegmentName(), s.getLoci());
					branchStream.printf("{\"%s\",%d,%.3f,%d,%d,%d,%.4f,%.3f,%s,%d}\t", sp, sp.getWholeGenomeID(), sp.getBirth(), sp.isTrunk()?1:0, sp.isTip()?1:0, s.isMarked()?1:0,  sp.getHostAge(), sp.getLayout(), sp.getSegmentName(), s.getLoci());
					branchStream.printf("%d\n", sp.getCoverage());
				}
			}
			branchStream.close();					

		} catch(IOException ex) {
			System.out.println("Could not write to file"); 
			System.exit(0);
		}

	}



	public static int sideBranchMutations() {
		int count = 0;
		for (Segment s : postOrderNodes(root)) {
			if (s.getParent() != null && s.getBirth() < (Parameters.getDate() - Parameters.yearsToTrunk) && (s.getBirth()> 0) ) {
				Segment sp = s.getParent();
				if (!s.isTrunk() && !(s.getSegmentNumber()==sp.getSegmentNumber())) {
					count++;
				}
			}
		}
		return count;
	}	

	public static double sideBranchOpportunity() {
		double time = 0;
		for (Segment s : postOrderNodes(root)) {
			if (s.getParent() != null && s.getBirth() < (Parameters.getDate() - Parameters.yearsToTrunk)&&(s.getBirth()>0)) {
				Segment sp = s.getParent();
				if (!s.isTrunk()) {
					time += s.getBirth() - sp.getBirth();
				}
			}
		}
		return time;
	}	

	public static int trunkMutations() {
		int count = 0;
		for (Segment s : postOrderNodes(root)) {
			if (s.getParent() != null && s.getBirth() < (Parameters.getDate() - Parameters.yearsToTrunk)&& (s.getBirth()> 0) ) {
				Segment sp = s.getParent();
				if (s.isTrunk() && sp.isTrunk() && !(s.getSegmentNumber()==sp.getSegmentNumber())) {
					count++;
				}
			}
		}
		return count;
	}	

	public static double trunkOpportunity() {
		double time = 0;
		for (Segment s : postOrderNodes(root)) {
			if (s.getParent() != null && s.getBirth() < (Parameters.getDate() - Parameters.yearsToTrunk)&&(s.getBirth()>0) ) {
				Segment sp = s.getParent();
				if (s.isTrunk() && sp.isTrunk()) {
					time += (s.getBirth() - sp.getBirth());
				}
			}
		}
		return time;
	}		

	public static void printMK() {

		try {
			File mkFile = new File("out.mk");
			mkFile.delete();
			mkFile.createNewFile();
			PrintStream mkStream = new PrintStream(mkFile);
			mkStream.printf("sideBranchMut,sideBranchOpp ,sideBranchRate,trunkMut,trunkOpp,trunkRate,mk\n");
			int sideBranchMut = sideBranchMutations();
			double sideBranchOpp = sideBranchOpportunity();
			double sideBranchRate = sideBranchMut / sideBranchOpp;
			int trunkMut = trunkMutations();
			double trunkOpp = trunkOpportunity();	
			double trunkRate = trunkMut / trunkOpp;		
			double mk = trunkRate / sideBranchRate;
			mkStream.printf("%d,%.4f,%.4f,%d,%.4f,%.4f,%.4f\n", sideBranchMut, sideBranchOpp, sideBranchRate, trunkMut, trunkOpp, trunkRate, mk);
			mkStream.close();
		} catch(IOException ex) {
			System.out.println("Could not write to file"); 
			System.exit(0);
		}

	}
	public static void init() {
		root = Parameters.urSegment;	
		tips = new ArrayList<Segment>();		
	}	

}