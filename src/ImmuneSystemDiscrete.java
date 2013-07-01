
import java.util.BitSet;

public class ImmuneSystemDiscrete implements ImmuneSystem{

	@Setting (description ="risk=specific_risk x exp(-xi_generalized x number_of_previous_infections)"	)  
		static double xi_generalized = 0.6;												
	@Setting (description ="risk=generalized_risk x exp(-xi_specific x number_of_segments_encountered_before)"	) 
		static double xi_specific = 0.6;
	
	int numPreviousInfections = 0;
	BitSet exposedToSegments = new BitSet();	
	
	public ImmuneSystemDiscrete() {			
	}

	public void reset() {
		numPreviousInfections=0;
		exposedToSegments.clear();	
	}
	
	@Override
	public double riskOfInfection(Virus v) {
		
		// Generalized immunity = Exp[-alpha x num_previous_infections]
		double generalizedimmunityExp = -xi_generalized*numPreviousInfections;

		// Specific immunity = Exp[-beta x num_seen_segments]
		BitSet seenSegments = (BitSet) exposedToSegments.clone();
		seenSegments.and(v.getSegmentIndices());
		double specificImmunityExp = -xi_specific*seenSegments.cardinality();
		
		// MAYBEDO: Drift		
		// double driftImmuntiy = -xi_drift*getDriftDistance(v,previousInfections);
		
		// Immunity = GeneralizedImmunity x Specific Immunity = Exp[-alpha*num_previous_infections-beta*num_seen_segments]
		return Math.exp(generalizedimmunityExp+specificImmunityExp);
	}

	@Override
	public void add(Virus v) {
		exposedToSegments.or(v.getSegmentIndices());
		numPreviousInfections+=1;
	}

	@Override
	public String print() {
		String returnValue=","+Integer.toString(numPreviousInfections); 
		for (int i = exposedToSegments.nextSetBit(0); i >= 0; i = exposedToSegments.nextSetBit(i+1)) {
		    returnValue=returnValue+","+Integer.toString(i);
		}

		return returnValue;		
	}
	
}
