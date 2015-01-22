
import java.util.BitSet;
import java.util.List;

public class ImmuneSystemDiscrete implements ImmuneSystem{

	public static class ImmunityParametres {
		@Setting (description ="risk=specific_risk x exp(-xi_generalized x number_of_previous_infections)"	)  
		static double xi_generalized = 0.3;												
		@Setting (description ="risk=generalized_risk x exp(-xi_specific x number_of_segments_encountered_before / numImmunogenicSegments)"	) 
		static double xi_specific = 0.1;
	}

	int numPreviousInfections = 0;
	BitSet exposedToImmunogenicSegments = new BitSet();

	public static void updateImmunogenicSegmentMask(int nImmunogenicSegmets) {
	}

	public ImmuneSystemDiscrete() {

	}

	public void reset() {
		numPreviousInfections=0;
		exposedToImmunogenicSegments.clear();	
	}

	public double riskOfInfection(Virus v) {

		// Generalized immunity = Exp[-alpha x num_previous_infections]
		double generalizedimmunityExp = - ImmunityParametres.xi_generalized*numPreviousInfections;

		// Specific immunity = Exp[-beta x num_seen_segments / nImmunogenicSegments]
		BitSet seenViralSegments = (BitSet) exposedToImmunogenicSegments.clone();
		seenViralSegments.and(v.getImmunogenicSegmentIndices());
		double specificImmunityExp = - ImmunityParametres.xi_specific*(double)seenViralSegments.cardinality()/(double)Parameters.SegmentParameters.nImmunogenicSegments;

		// MAYBEDO: Drift		
		// double driftImmuntiy = -xi_drift*getDriftDistance(v,previousInfections);

		// Immunity = GeneralizedImmunity x Specific Immunity = Exp[-alpha*num_previous_infections-beta*num_seen_segments]
		return Math.exp(generalizedimmunityExp+specificImmunityExp);
	}

	public void add(Virus v) {
		exposedToImmunogenicSegments.or(v.getImmunogenicSegmentIndices());
		numPreviousInfections+=1;
	}

	public String print() {
		String returnValue=","+Integer.toString(numPreviousInfections); 
		for (int i = exposedToImmunogenicSegments.nextSetBit(0); i >= 0; i = exposedToImmunogenicSegments.nextSetBit(i+1)) {
			returnValue=returnValue+","+Integer.toString(i);
		}

		return returnValue;		
	}

	public void vaccinate(List<Virus> virusList) {
		for (Virus v : virusList) {
			exposedToImmunogenicSegments.or(v.getImmunogenicSegmentIndices());
		}
		numPreviousInfections+=1;		
	}

}
