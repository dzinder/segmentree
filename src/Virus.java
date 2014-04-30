import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/* Virus infection that has a phenotype */

public class Virus {

	private float birth;		// measured in years relative to burn-in
	private float hostAge;		// age of host in years at time of infection
	BitSet segmentIndices = new BitSet(); // Virus segment indices in bit form 	
	List<Segment> segments = new ArrayList<Segment>(); // List of virus segments
	//private long virusNumber = 0;
	//static long lastVirusNumber = -1;

	// CONSTRUCTORS & INITIALIZERS
	// generate virus copy from parent virus pV 
	public Virus(Virus pV, float hostAge_) {
		hostAge=hostAge_;
		birth = (float) Parameters.getDate();
		segmentIndices=(BitSet) pV.segmentIndices.clone();
		for (Segment s : pV.segments) {
			segments.add(new Segment(s,hostAge_,this.hashCode()));
		}
		//virusNumber=lastVirusNumber+1;
		//lastVirusNumber+=1;
	}
	
	// generate new virus from parent viral segments (for reassortment or initial virus construction)
	public Virus(List<Segment> pSegments, float hostAge_) {
		hostAge=hostAge_;
		birth = (float) Parameters.getDate();
		for (Segment s : pSegments) {
			segmentIndices.set(s.getSegmentNumber());
			segments.add(new Segment(s,hostAge_,s.getSegmentNumber(),this.hashCode()));
		}	
	}
	
	// METHODS
	public float getBirth() {
		return birth;
	}

	public double getHostAge() {
		return hostAge;
	}

	// returns allele version at each loci "3,2,....,allele_version_at_loci_n"
	public String toString(String seperator) {

		String returnValue = "";

		for (int i=0; i<segments.size()-1;i++) {
			returnValue+=(segments.get(i).toString()+seperator);
		}
		returnValue+=segments.get(segments.size()-1).toString();
		
		return returnValue;
	}

	public Segment getRandomSegment() {		
		return segments.get(Random.nextInt(0, segments.size()-1));
	}

	public List<Segment> getSegments() {		
		return segments;
	}
	
	public BitSet getSegmentIndices() {
		return segmentIndices;
	}

	public Virus reassort(List<Virus> coinfectingViruses) { 
		List<Segment> reassortedSegments = new ArrayList<Segment>();
		for (int i=0; i<segments.size();i++) {
			if (Random.nextBoolean(Parameters.rho))  
				reassortedSegments.add(coinfectingViruses.get(Random.nextInt(0, coinfectingViruses.size()-1)).getSegments().get(i));				
			else
				reassortedSegments.add(segments.get(i));
		}
		return new Virus(reassortedSegments,hostAge);
	}

	public Virus mutate() {			
		List<Segment> mutatedSegments = new ArrayList<Segment>();
		for (Segment s : segments) {
			mutatedSegments.add(new Segment(s,hostAge, this.hashCode() ));
		}	
		int randomSite = Random.nextInt(0, segments.size()-1);
		mutatedSegments.set(randomSite, segments.get(randomSite).mutate());
		return new Virus(mutatedSegments,hostAge);		
	}
	
}