
import java.util.BitSet;
import java.util.List;

public class ImmuneSystemDiscrete implements ImmuneSystem{

	public static class ImmunityParameters {
		@Setting (description ="risk=infection_risk x gen_risk x specific_risk = exp(-rho_reduced_infection*#previous_infections) x exp(-sigma*(sigma_het x #previous_infections) x exp (-sigma*(1-sigma_het) x #previous_segments / nSegments)"	)  
		static double sigma = 0.41;
		@Setting (description ="the part of immunity which is reduction in suscptibility based on the number of previous infections\n"
				+ "risk=infection_risk x gen_risk x specific_risk = exp(-rho_reduced_infection) x exp(-sigma*(sigma_het x #previous_infections) x exp (-sigma*(1-sigma_het) x #previous_segments / nSegments)"	)  
		static double sigma_het = 0.3;	
		@Setting (description ="reduction in infectivity following previous infections\n"
				+ "risk=risk=infection_risk x gen_risk x specific_risk = exp(-rho_reduced_infection*#previous_infections) x exp(-sigma*(sigma_het x #previous_infections) x exp (-sigma*(1-sigma_het) x #previous_segments / nSegments)"	)
		static double rho_reduced_infection = 0.39;
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

		// reduced infectivity
		double reducedInfectivityExp = - ImmunityParameters.rho_reduced_infection*numPreviousInfections;
		
		// Generalized immunity = Exp[-alpha x num_previous_infections]
		double generalizedimmunityExp = - ImmunityParameters.sigma*ImmunityParameters.sigma_het*numPreviousInfections;

		// Specific immunity = Exp[-beta x num_seen_segments / nImmunogenicSegments]
		BitSet seenViralSegments = (BitSet) exposedToImmunogenicSegments.clone();
		seenViralSegments.and(v.getImmunogenicSegmentIndices());
		double specificImmunityExp = - ImmunityParameters.sigma*(1.0-ImmunityParameters.sigma_het)*(double)seenViralSegments.cardinality()/(double)Parameters.SegmentParameters.nImmunogenicSegments;

		// MAYBEDO: Drift		
		// double driftImmuntiy = -xi_drift*getDriftDistance(v,previousInfections);

		// Immunity = GeneralizedImmunity x Specific Immunity = Exp[-alpha*num_previous_infections-beta*num_seen_segments]
		return Math.exp(generalizedimmunityExp+specificImmunityExp+reducedInfectivityExp);
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
