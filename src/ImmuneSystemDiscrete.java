
import java.util.BitSet;
import java.util.List;

public class ImmuneSystemDiscrete implements ImmuneSystem{

	public static class ImmunityParameters {
		@Setting (description ="the part of immunity which is reduction in suscptibility based on the number of segments seen before\n"
				+ "risk=gen_risk x specific_risk = exp(-sigma_het x #previous_infections) x exp(-sigma_spec x #previous_segments / nSegments)"	)  
		static double sigma_spec = 0.42*0.5;	
		@Setting (description ="the part of immunity which is reduction in suscptibility based on the number of previous infections\n"
				+ "risk=gen_risk x specific_risk = exp(-sigma_het x #previous_infections) x exp(-sigma_ho x #previous_segments / nSegments)"	)  
		static double sigma_gen = 0.42;	
		@Setting (description ="reduction in infectivity following previous infections\n"
				+ "risk=exp(-rho_reduced_infection x #previous_infections)"	)
		static double rho_reduced_infectivity = 1.16;
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
		double generalizedimmunityExp = - ImmunityParameters.sigma_gen*numPreviousInfections;

		// Specific immunity = Exp[-beta x num_seen_segments / nImmunogenicSegments]
		BitSet seenViralSegments = (BitSet) exposedToImmunogenicSegments.clone();
		seenViralSegments.and(v.getImmunogenicSegmentIndices());
		double specificImmunityExp = - ImmunityParameters.sigma_spec*(double)seenViralSegments.cardinality()/(double)Parameters.SegmentParameters.nImmunogenicSegments;

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

	@Override
	public double getRiskOfTransmission() {		
		return Math.exp(-ImmunityParameters.rho_reduced_infectivity*numPreviousInfections);
	}

}
