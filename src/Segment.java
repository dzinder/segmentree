/* Segment infection that has genotype, phenotype and ancestry */

import java.util.*;

public class Segment {

	// simulation fields
	private Segment parent;
	private long wholeGenomeID;	
	private int segmentNumber;
	private short loci;
	private double fitness;
		
	private float birth;		// measured in years relative to Parameters.SimulationParameters.burnin
	private float hostAge;		// age of host in years at time of infection

	// tree 	
	private boolean active = false; // if presently participating in an infection
	private boolean marked = false;
	private boolean trunk = false;	// fill this at the end of the simulation
	private List<Segment> children = null; //new ArrayList<Segment>(0);	// will be void until simulation ends	
	private float layout;
	private int coverage;		// how many times this Segment has been covered in tracing the tree backwards	
	
	// segment unique number generator
	static private int lastSegmentNumber = -2; // -1 is root, 0 is first etc...
		
	//	create new segment type for root 
	public Segment() {
		segmentNumber = (short) (lastSegmentNumber+1);
		lastSegmentNumber+=1;
		birth = (float) Parameters.getDate();
		wholeGenomeID=-1;
		loci=-1;
		switch (Parameters.SegmentParameters.segmentFitnessType) {
			case EQUAL_FITNESS : fitness=1; break;
			case RANDOM_EXPONENTIAL : fitness=Random.nextExponential(Parameters.SegmentParameters.segmentFitnessParam1); break;
			case RANDOM_TRUNCATED_NORMAL :fitness=Math.min(Parameters.SegmentParameters.segmentFitnessParam1*2,Math.max(0, Random.nextNormal(Parameters.SegmentParameters.segmentFitnessParam1,Parameters.SegmentParameters.segmentFitnessParam2))); break;
			default : fitness=1;
		}
		parent=null;		
	}
	
	//	create new segment type for mutations
	public Segment(Segment pS, float hostAge, long wholeGenomeID_, short loci_) {
		segmentNumber =  lastSegmentNumber+1;
		lastSegmentNumber+=1;
		birth = (float) Parameters.getDate();
		wholeGenomeID=wholeGenomeID_;
		loci=loci_;
		switch (Parameters.SegmentParameters.segmentFitnessType) {
			case EQUAL_FITNESS : fitness=1; break;
			case RANDOM_EXPONENTIAL : fitness=Random.nextExponential(Parameters.SegmentParameters.segmentFitnessParam1); break;
			case RANDOM_TRUNCATED_NORMAL : fitness=Math.min(Parameters.SegmentParameters.segmentFitnessParam1*2,Math.max(0, Random.nextNormal(Parameters.SegmentParameters.segmentFitnessParam1,Parameters.SegmentParameters.segmentFitnessParam2))); break;
			default : fitness=1;
		}
		parent=pS;
		
	}
	
	// replication, copies the segment, and adds ancestry
	public Segment(Segment pS, float hostAge_, long wholeGenomeID_) {
		parent = pS;		
		segmentNumber=pS.segmentNumber;
		birth = Parameters.getDate();
		hostAge = hostAge_;
		wholeGenomeID=wholeGenomeID_;
		loci=pS.getLoci();
		fitness=pS.getFitness();
	}
	
	double getFitness() {
		return fitness;
	}

	// replication, copies the segment, and adds ancestry
	public Segment(Segment pS, float hostAge_, int segmentNumber_, long wholeGenomeID_) {
		parent = pS;		
		segmentNumber=pS.segmentNumber;		
		birth = Parameters.getDate();
		hostAge = hostAge_;
		wholeGenomeID=wholeGenomeID_;
		loci=pS.getLoci();
		fitness=pS.getFitness();
		segmentNumber=segmentNumber_;
	}		
	
	public void setParent(Segment parent_) {
		parent=parent_;
	}
	
	public float getBirth() {
		return birth;
	}
	
	public Segment getParent() {
		return parent;
	}
	
	public double getHostAge() {
		return hostAge;
	}
	
	public boolean isTrunk() {
		return trunk; 
	}
	
	public void makeTrunk() {
		trunk = true;
	}
	
	public void mark() {
		marked = true;
	}
	
	public boolean isMarked() {
		return marked;
	}
	
	public double getLayout() {
		return layout;
	}
	
	public void setLayout(float y) {
		layout = y;
	}
	
	public int getCoverage() {
		return coverage;
	}

	public void incrementCoverage() {
		coverage++;
	}
	
	// add virus node as child if does not already exist
	public void addChild(Segment v) {
		if (children==null) {
			children = new ArrayList<Segment>(0);
		}
		if (!children.contains(v)) {
			children.add(v);
		}
	}		
	
	public int getNumberOfChildren() {
		if (children==null) {
			return 0;
		}
		return children.size();
	}
	
	public List<Segment> getChildren() {
		if (children==null) {
			children=new ArrayList<Segment>(0);
		}
		return children;
	}	
	
	public boolean isTip() {
		return getNumberOfChildren() == 0 ? true : false;
	}
	
	public Segment commonAncestor(Segment virusB) {
				
		Segment lineageA = this;
		Segment lineageB = virusB;
		Segment commonAnc = null;
		Set<Segment> ancestry = new HashSet<Segment>();		
		while (true) {
			if (lineageA.getParent() != null) {		
				lineageA = lineageA.getParent();
				if (!ancestry.add(lineageA)) { 
					commonAnc = lineageA;
					break; 
				}
			}
			if (lineageB.getParent() != null) {
				lineageB = lineageB.getParent();
				if (!ancestry.add(lineageB)) { 
					commonAnc = lineageB;
					break; 
				}
			}
		}		
		return commonAnc;	
	}
	
	public double distance(Segment virusB) {
		Segment ancestor = commonAncestor(virusB);
		double distA = getBirth() - ancestor.getBirth();
		double distB = virusB.getBirth() - ancestor.getBirth();
		return distA + distB;
	}
	
	public String toString() {
		return Integer.toHexString(this.hashCode());
	}
	
	public String getSegmentName() {
		return Integer.toString(segmentNumber);
	}

	public int getSegmentNumber() {		
		return segmentNumber;
	}

	public Segment mutate() {				
		Segment s = new Segment(this,hostAge,wholeGenomeID,loci); 
		s.parent=this;				
		return s;			
	}
	
	public Segment mutate(short newloci) {				
		Segment s = new Segment(this,hostAge,wholeGenomeID,newloci); 
		s.parent=this;				
		return s;			
	}

	public long getWholeGenomeID() {
		return wholeGenomeID;
	}

	public short getLoci() {
		return loci;
	}


	public void markActive() {
		active=true;		
	}
	
	public void unmarkActive() {
		active=false;		
	}

	public boolean isActive() {
		return active;
	}

	public void removeChild(Segment child) {
		if (children!=null) {
			int childIndex =children.indexOf(child); 
			if (childIndex>=0) 
				children.remove(childIndex);
			if (children.size()==0) {
				children=null;
			}
		}
		
	}

	public void decrementCoverage() {
		coverage--;		
	}

}