import java.util.BitSet;
import java.util.List;

/* Virus infection that has a phenotype */

public class Virus {

	private float birth;		// measured in years relative to burn-in
	private float hostAge;		// age of host in years at time of infection
	//BitSet segmentIndices = new BitSet(); // Virus segment indices in bit form 	
	BitSet immunogenicSegmentIndices =null; // Virus segments which are immunogenic in bit form
	Segment[] segments = null; // List of virus segments
	//private long virusNumber = 0;
	//static long lastVirusNumber = -1;

	// CONSTRUCTORS & INITIALIZERS
	// generate virus copy from parent virus pV 
	public Virus(Virus pV, float hostAge_) {
		hostAge=hostAge_;
		birth = Parameters.getDate();
		immunogenicSegmentIndices=(BitSet) pV.immunogenicSegmentIndices;
		segments = new Segment[Parameters.SegmentParameters.nSegments];
		for (int i=0;i<segments.length;i++) {
			segments[i]=new Segment(pV.segments[i],hostAge_,this.hashCode());
		}
		//virusNumber=lastVirusNumber+1;
		//lastVirusNumber+=1;
	}
	
	// generate new virus from parent viral segments (for reassortment or initial virus construction)
	public Virus(Segment[] pSegments, float hostAge_) {
		hostAge=hostAge_;
		birth = Parameters.getDate();
		immunogenicSegmentIndices = new BitSet();
		segments = new Segment[Parameters.SegmentParameters.nSegments];
		for (int i=0;i<pSegments.length;i++) {
			if (pSegments[i].getLoci()<Parameters.SegmentParameters.nImmunogenicSegments)
				immunogenicSegmentIndices.set(pSegments[i].getSegmentNumber());
			segments[i]=new Segment(pSegments[i],hostAge_,pSegments[i].getSegmentNumber(),this.hashCode());
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

		for (int i=0; i<segments.length-1;i++) {
			returnValue+=(segments[i].toString()+seperator);
		}
		returnValue+=segments[segments.length-1].toString();
		
		return returnValue;
	}

	public Segment getRandomSegment() {		
		return segments[Random.nextInt(0, segments.length-1)];
	}

	public Segment[] getSegments() {		
		return segments;
	}
	
	public BitSet getImmunogenicSegmentIndices() {
		return immunogenicSegmentIndices;
	}

	public Virus reassort(List<Virus> coinfectingViruses) { 
		Segment[] reassortedSegments = new Segment[Parameters.SegmentParameters.nSegments];
		for (int i=0; i<segments.length;i++) {
			if (Random.nextBoolean(Parameters.MutationAndReassortmentParameters.rho))  
				reassortedSegments[i]=coinfectingViruses.get(Random.nextInt(0, coinfectingViruses.size()-1)).getSegments()[i];				
			else
				reassortedSegments[i]=segments[i];
		}
		return new Virus(reassortedSegments,hostAge);
	}

	public Virus mutate() {			
		Segment[] mutatedSegments = new Segment[Parameters.SegmentParameters.nSegments];
		for (int i=0;i<Parameters.SegmentParameters.nSegments;i++) {
			mutatedSegments[i]=new Segment(segments[i],hostAge, this.hashCode());
		}	
		int randomSite = Random.nextInt(0, segments.length-1);
		mutatedSegments[randomSite]=segments[randomSite].mutate();
		return new Virus(mutatedSegments,hostAge);		
	}
	
}