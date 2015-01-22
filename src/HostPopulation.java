/* A population of host individuals */

import java.util.*;
import java.io.*;

import org.javatuples.Pair;

public class HostPopulation {

	// fields
	private int cases;	// number of cases from last count, doesn't effect dynamics 
	private List<Host> susceptibles = new ArrayList<Host>(); 
	private List<Host> infecteds = new ArrayList<Host>(); // including superinfecteds
	private List<Host> initialStrainReservoir = new ArrayList<Host>();

	// host samples
	private InfectedHostSamples infectedHostSamples = new InfectedHostSamples();
	private HostsForImmunitySamples hostsForImmunitySamples = new HostsForImmunitySamples();

	// Vaccine composition
	private HashMap<BitSet,Pair<Virus,Integer>> strainTallyForVaccineComposition = new HashMap<BitSet,Pair<Virus,Integer>>();	
	private HashMap<Integer,Pair<Segment,Integer>> segmentTallyForVaccineComposition = new HashMap<Integer,Pair<Segment,Integer>>(); // TODO: implement this	
	private List<Virus> vaccineComposition = new ArrayList<Virus>();
	// Vaccine queue
	private List<Host> vaccineQueue = new ArrayList<Host>();
	private int minTimeIntervalForVaccinationQueueUpdate;
	private int maxAge;

	// genome samples for vaccine makeup determination

	// CONSTRUCTORS & INITIALIZERS
	public HostPopulation() {		
		// For vaccination
		if (Parameters.VaccineParameters.vaccinationAges.length>0) {
			minTimeIntervalForVaccinationQueueUpdate  = Parameters.VaccineParameters.vaccinationAges[0];
			maxAge  = Parameters.VaccineParameters.vaccinationAges[Parameters.VaccineParameters.vaccinationAges.length-1];
		}
		else { 
			minTimeIntervalForVaccinationQueueUpdate = Parameters.SimulationParameters.endDay;
			maxAge = Parameters.SimulationParameters.endDay;
		}
	}

	// close sample lists
	public void close() {
		infectedHostSamples.close();
		hostsForImmunitySamples.close();				
	}

	// reset population to factory condition
	public void reset() {

		// samples
		infectedHostSamples.reset();
		hostsForImmunitySamples.reset();		

		// clearing lists
		susceptibles.clear();
		infecteds.clear();			
		strainTallyForVaccineComposition.clear();
		segmentTallyForVaccineComposition.clear();
		vaccineComposition.clear();

		// reservoir
		initialStrainReservoir.clear();
		if (Parameters.ReservoirParameters.proportionContactWithReservoir>0) {					
			for (Virus v : Parameters.initialViruses) {
				Host h = new Host(true);
				h.infect(v);
				initialStrainReservoir.add(h);
			}
		}

		// fill population with susceptibles
		int initialS = Parameters.DemographicParameters.N;
		initialS -= Parameters.EpidemiologicalParameters.initialI; // minus initial number of infected

		for (int i = 0; i < initialS; i++) {			
			if (i%5000000 == 0 ) System.out.println("adding hosts: " + i + " out of " + initialS); 	// display											
			Host h = new Host(true);			
			susceptibles.add(h);	
		}
		System.out.println("finished constructing " + initialS + " initial susceptible hosts\n"); // display

		// infect some individuals
		for (int i = 0; i < Parameters.EpidemiologicalParameters.initialI; i++) {		
			Host h = new Host(true);
			h.infect(Parameters.initialViruses.get(Random.nextInt(0, Parameters.initialViruses.size()-1)));
			infecteds.add(h);
		}		
		System.out.println("finished constructing " + Parameters.EpidemiologicalParameters.initialI + " infected hosts\n"); // display

		// add initial immune history to some individuals
		for (int i = 0; i < Math.round(Parameters.EpidemiologicalParameters.initialPrR*Parameters.DemographicParameters.N); i++) {
			if (i%5000000 == 0 ) System.out.println("adding immune history: " + i + " out of " + Math.round(Parameters.EpidemiologicalParameters.initialPrR*Parameters.DemographicParameters.N)); 	// display		
			getRandomHost().addToImmuneHistory(Parameters.initialViruses.get(Random.nextInt(0, Parameters.initialViruses.size()-1)));
		}		
		System.out.println("finished immunizing " +Math.round(Parameters.EpidemiologicalParameters.initialPrR*Parameters.DemographicParameters.N) + " hosts\n"); // display
	}

	// METHODS
	public int getN() {
		return susceptibles.size() + infecteds.size();
	}

	public int getS() {
		return susceptibles.size();
	}

	public int getI() {
		return infecteds.size();
	}

	public double getPrS() {
		return (double) getS() / (double) getN();
	}

	public double getPrI() {
		return (double) getI() / (double) getN();
	}

	public int getRandomN() {
		return Random.nextInt(0,getN()-1);
	}
	public int getRandomS() {
		return Random.nextInt(0,getS()-1);
	}
	public int getRandomI() {
		return Random.nextInt(0,getI()-1);
	}	

	public Host getRandomHost() {
		double n = Random.nextInt(0,getN()-1);
		if (n <= (getS()-1)) 
			return getRandomHostS();
		return getRandomHostI();
	}

	public Host getRandomHostS() {
		return susceptibles.get(Random.nextInt(0,susceptibles.size()-1));
	}

	public Host getRandomHostI() {
		return infecteds.get(Random.nextInt(0,infecteds.size()-1));
	}


	public void resetCases() {
		cases = 0;
	}

	public int getCases() {
		return cases;
	}	

	public void removeSusceptible(int i) {
		// remove by moving last susceptible to location i, and than shortening ArrayList		
		susceptibles.set(i, susceptibles.get(susceptibles.size()-1));
		susceptibles.remove(susceptibles.size()-1);
	}

	public void removeInfected(int i) {	
		// remove by moving last infected to location i, and than shortening ArrayList
		infecteds.set(i, infecteds.get(infecteds.size()-1));
		infecteds.remove(infecteds.size()-1);
	}


	public void stepForward() {

		// Potential Future Hypothesis 
		// 0. How tress of segments relate for different paramters
		// 1. Epistasis between segments - some segment combinations have better fitness 
		// 2. Fitness heterogeneity among hosts - n differential fitness per segment depending on hosts
		// 3. Immunization thersholds per segment 
		// 4. Immunizing population - generalized immunization, segment specific immunization
		// Fitness could be either in recovery or contact                                         

		if (Parameters.DemographicParameters.swapDemography) {
			swap();
		} else {
			grow();
			decline();
		}

		contact(); // reassortants are sampled on contact
		recover();			
		mutate(); // introduce new segments without recycling

		vaccinate(); // vaccinate individuals based on policy
		
		disruption(); // 
		
		contactReservoir();

		sample(); // doesn't effect dynamics

	}

	private void vaccinate() {
		if (Parameters.day > Parameters.VaccineParameters.vaccinationProgramStartTime) {

			// TODO: switch to a non-naive algorithm for this
			//			for (Host h : susceptibles ) {
			//				for (double age : Parameters.vaccinationAges) {
			//					if (h.getAgeInDays()==age) {
			//						if (Random.nextBoolean(Parameters.vaccineP)) {
			//							h.immunize(vaccineComposition); 
			//						}
			//					}
			//				}
			//			}
			//
			//			for (Host h : infecteds ) {
			//				for (double age : Parameters.vaccinationAges) {
			//					if (h.getAgeInDays()==age) {
			//						if (Random.nextBoolean(Parameters.vaccineP)) {
			//							h.immunize(vaccineComposition); 
			//						}
			//					}
			//				}
			//			}

			if ((Parameters.day-Parameters.VaccineParameters.vaccinationProgramStartTime)%minTimeIntervalForVaccinationQueueUpdate==0) {
				vaccineQueue.clear();
				for (Host h : susceptibles) {
					if (h.getAgeInDays()<=maxAge) {
						vaccineQueue.add(h);
					}
				}
				for (Host h : infecteds) {
					if (h.getAgeInDays()<=maxAge) {
						vaccineQueue.add(h);
					}
				}
			}

			for (Host h : vaccineQueue ) {
				for (double age : Parameters.VaccineParameters.vaccinationAges) {
					if (h.getAgeInDays()==age) {
						if (Random.nextBoolean(Parameters.VaccineParameters.vaccineP)) {
							h.immunize(vaccineComposition); 
						}
					}
				}
			}

		}
	}
	
	private void disruption() {
		if (Parameters.day == Parameters.DisruptionParameters.disruptionTime) {
			switch (Parameters.DisruptionParameters.disruptionType) {
			case MASS_EXTINCTION :							
				int recoveries = Random.nextPoisson(getI()*Parameters.DisruptionParameters.disruptionParameter);

				for (int i = 0; i < recoveries; i++) {
					if (getI()>0) {
						int index = getRandomI();
						Host h = infecteds.get(index);
						removeInfected(index);
						h.clearInfections();
						susceptibles.add(h);
					}					
				}
				break;
			case CHANGE_MUTATION :							
				Parameters.MutationAndReassortmentParameters.mu=Parameters.DisruptionParameters.disruptionParameter;
				break;
			case CHANGE_INTRO :							
				Parameters.MutationAndReassortmentParameters.intro=Parameters.DisruptionParameters.disruptionParameter;
				break;
			case CHANGE_REASSORTMENT :							
				Parameters.MutationAndReassortmentParameters.rho=Parameters.DisruptionParameters.disruptionParameter;
				break;
			case NONE:
				break;
			default:
				break;
			}
		}
		
	}

	private void mutate() {		

		double totalMutationRate = getI() * Parameters.MutationAndReassortmentParameters.mu+Parameters.MutationAndReassortmentParameters.intro;
		int mutations = Random.nextPoisson(totalMutationRate);
		for (int i = 0; i < mutations; i++) {
			getRandomHostI().mutate();
		}
	}

	// draw a Poisson distributed number of births and add these hosts to the end of the population list
	// new hosts are always born naive
	public void grow() {
		double totalBirthRate = getN() * Parameters.DemographicParameters.birthRate;
		int births = Random.nextPoisson(totalBirthRate);
		for (int i = 0; i < births; i++) {
			Host h = new Host(false);
			susceptibles.add(h);
		}
	}

	// draw a Poisson distributed number of deaths and remove random hosts from the population list
	public void decline() {
		// deaths in susceptible class
		double totalDeathRate = getS() * Parameters.DemographicParameters.deathRate;
		int deaths = Random.nextPoisson(totalDeathRate);
		for (int i = 0; i < deaths; i++) {
			if (getS()>0) {
				int sndex = getRandomS();
				removeSusceptible(sndex);
			}
		}		
		// deaths in infectious class		
		totalDeathRate = getI() * Parameters.DemographicParameters.deathRate;
		deaths = Random.nextPoisson(totalDeathRate);
		for (int i = 0; i < deaths; i++) {
			if (getI()>0) {
				int index = getRandomI();
				removeInfected(index);
			}
		}		

	}
	
	private void contactReservoir() {
		// each infected (or superinfected) makes contacts on a per-day rate of propContactWithReservoir*beta*reservoirSize*S/N
		double susceptibleContactRate = initialStrainReservoir.size()* getPrS()*Parameters.EpidemiologicalParameters.beta*Parameters.ReservoirParameters.proportionContactWithReservoir;
		int contacts = Random.nextPoisson(susceptibleContactRate);
		for (int i = 0; i < contacts; i++) {
			if (getS()>0) {
				// get indices and objects
				Host iH = initialStrainReservoir.get(Random.nextInt(0, initialStrainReservoir.size()-1));
				int sndex = getRandomS();
				Host sH = susceptibles.get(sndex);			

				if (!iH.isSuperinfected()) {
					// attempt infection
					Virus v = iH.getRandomInfection();
					double chanceOfSuccess = sH.riskOfInfection(v); 
					if (Random.nextBoolean(chanceOfSuccess)) {
						sH.infect(v);
						removeSusceptible(sndex);
						infecteds.add(sH);
						cases++; // doesn't effect dynamics
					}
				} else {
					// for superinfected host:
					// Pick n_bottleNeck random viruses, for each segment with probability rho replace it with segment from all infecting viruses
					// viruses transmits based on individual probability
					boolean infected = false; 
					for (int j=0; j<Parameters.MutationAndReassortmentParameters.n_bottleNeck; j++) {
						Virus v = iH.getRandomInfection();
						double chanceOfSuccess = sH.riskOfInfection(v); 
						if (Random.nextBoolean(chanceOfSuccess)) {
							infected = true;
							sH.infect(v);
						}
					}
					if (infected) {
						removeSusceptible(sndex);
						infecteds.add(sH);
						cases++; // doesn't effect dynamics
					}
				}			
			}
		}

	}

	// draw a Poisson distributed number of births and reset these individuals
	public void swap() {
		// draw random individuals from susceptible class
		double totalBirthRate = getS() * Parameters.DemographicParameters.birthRate;
		int births = Random.nextPoisson(totalBirthRate);
		for (int i = 0; i < births; i++) {
			if (getS()>0) {
				getRandomHostS().reset();
			}
		}		

		// draw random individuals from infected class
		totalBirthRate = getI() * Parameters.DemographicParameters.birthRate;
		births = Random.nextPoisson(totalBirthRate);

		// remove infected and add to susceptible births
		for (int i = 0; i < births; i++) {			
			if (((Parameters.day>Parameters.SimulationParameters.burnin) || (!Parameters.SimulationParameters.keepAliveDuringBurnin)) && (!Parameters.SimulationParameters.keepAlive)) {
				if (getI()>0) {
					int index = getRandomI();
					Host h = infecteds.get(index);		
					removeInfected(index);
					h.reset();					
					susceptibles.add(h);
				}
			}
			else if ((Parameters.day<Parameters.SimulationParameters.burnin || Parameters.SimulationParameters.keepAlive) && getI()>1) {
				int index = getRandomI();
				Host h = infecteds.get(index);
				removeInfected(index);
				h.reset();
				susceptibles.add(h);
			}
		}	
	}

	// draw a Poisson distributed number of contacts and move from S->I based upon this
	public void contact() {

		// each infected (or superinfected) makes contacts on a per-day rate of beta*I*S/N
		double susceptibleContactRate = getI()* getPrS()*Parameters.EpidemiologicalParameters.beta;
		int contacts = Random.nextPoisson(susceptibleContactRate);
		for (int i = 0; i < contacts; i++) {
			if (getS()>0) {
				// get indices and objects
				Host iH = getRandomHostI();
				int sndex = getRandomS();
				Host sH = susceptibles.get(sndex);			

				if (!iH.isSuperinfected()) {
					// attempt infection
					Virus v = iH.getRandomInfection();
					double chanceOfSuccess = sH.riskOfInfection(v)*iH.getRiskOfTransmission(); 
					if (Random.nextBoolean(chanceOfSuccess)) {
						sH.infect(v);
						removeSusceptible(sndex);
						infecteds.add(sH);
						cases++; // doesn't effect dynamics
					}
				} else {
					// for superinfected host:
					// Pick n_bottleNeck random viruses, for each segment with probability rho replace it with segment from all infecting viruses
					// viruses transmits based on individual probability
					boolean infected = false; 
					for (int j=0; j<Parameters.MutationAndReassortmentParameters.n_bottleNeck; j++) {
						Virus v = iH.getRandomInfection();
						double chanceOfSuccess = sH.riskOfInfection(v)*iH.getRiskOfTransmission(); 
						if (Random.nextBoolean(chanceOfSuccess)) {
							infected = true;
							sH.infect(v);
						}
					}
					if (infected) {
						removeSusceptible(sndex);
						infecteds.add(sH);
						cases++; // doesn't effect dynamics
					}
				}			
			}
		}

		// each infected (or superinfected) makes contact on a per-day rate of beta*I*I/N
		double infectedContactRate = getI()* getPrI()*Parameters.EpidemiologicalParameters.beta;
		contacts = Random.nextPoisson(infectedContactRate);
		for (int i = 0; i < contacts; i++) {
			// get indices and objects
			Host fromH = getRandomHostI();
			Host toH = getRandomHostI();

			// attempt infection
			Virus v = fromH.getRandomInfection();
			double chanceOfSuccess = toH.riskOfInfection(v); 
			if (Random.nextBoolean(chanceOfSuccess)) {
				toH.infect(v);
			}
		}

	}


	// draw a Poisson distributed number of recoveries and move from I->S based upon this
	public void recover() {
		// each infected recovers at a per-day rate of nu
		// infected clear from multiple infections simultaneously

		double totalRecoveryRate = getI() * Parameters.EpidemiologicalParameters.nu;
		int recoveries = Random.nextPoisson(totalRecoveryRate);

		for (int i = 0; i < recoveries; i++) {
			if (((Parameters.day>Parameters.SimulationParameters.burnin) || (!Parameters.SimulationParameters.keepAliveDuringBurnin)) && (!Parameters.SimulationParameters.keepAlive)) {
				if (getI()>0) {
					int index = getRandomI();
					Host h = infecteds.get(index);
					removeInfected(index);
					h.clearInfections();
					susceptibles.add(h);
				}
			}
			else if ((Parameters.day<Parameters.SimulationParameters.burnin || Parameters.SimulationParameters.keepAlive) && getI()>1) {
				int index = getRandomI();
				Host h = infecteds.get(index);
				removeInfected(index);
				h.clearInfections();					
				susceptibles.add(h);
			}


		}

	}			

	public void sample() {
		if (getI()>0 && Parameters.day >= Parameters.SimulationParameters.burnin) {
			// Sample infected hosts for out.infected
			int numInfectedHostSamples = Random.nextPoisson(Parameters.SamplingParameters.infectedHostSamplingRate*getI());

			for (int i=0; i<numInfectedHostSamples; i++) {
				Host h = getRandomHostI();					
				for (Virus v : h.getInfections()) {
					for (Segment s : v.getSegments()) {												
						infectedHostSamples.add(h,v,s);
					}
				}				
			}

			// Sample all hosts for out.immunity
			int numImmunityHostSamples = Random.nextPoisson(Parameters.SamplingParameters.immunityHostSamplingRate*getN());

			for (int i=0; i<numImmunityHostSamples; i++) {
				hostsForImmunitySamples.add(getRandomHost());
			}

			// Sample tree tips
			double totalSamplingRate = Parameters.SamplingParameters.tipSamplingRate;
			if (Parameters.SamplingParameters.tipSamplingProportional) 
				totalSamplingRate *= getI();
			else 
				totalSamplingRate *= getN();			

			int samples = Random.nextPoisson(totalSamplingRate);

			if (Parameters.SamplingParameters.sampleWholeGenomes) { // sample whole genomes of viruses
				for (int i = 0; i < samples; i++) {									
					for (Segment s : getRandomHostI().getRandomInfection().getSegments()) {
						SegmentTree.add(s);
					}
				}				
			}
			else { // sample segments independently
				for (int i = 0; i < samples; i++) {									
					SegmentTree.add(getRandomHostI().getRandomInfection().getRandomSegment());
				}
			}
		}
	}

	// through current infected population assigning ancestry as trunk
	public void makeTrunk() {
		for (Host h : infecteds) {			
			for (Virus v : h.getInfections()) {
				for (Segment s : v.getSegments()) {
					s.makeTrunk();
					while (s.getParent() != null) {
						s = s.getParent();
						if (s.isTrunk()) {
							break;
						} else {
							s.makeTrunk();
						}
					}
				}
			}
		}				
	}	

	public void printState(PrintStream stream) {
		if (Parameters.day > Parameters.SimulationParameters.burnin) {
			stream.printf("\t%d\t%d\t%d\t%d\t%d", getN(), getS(), getI(),0 /* getR()*/, getCases());
		}
	}	

	public List<Host> getIs() {	
		return infecteds;
	}

	public void determineVaccineComposition() {
		switch (Parameters.VaccineParameters.vaccineMakeup) {
		case MAXIMUM_COVERAGE :	
			// TODO: this
			System.err.println("MAXIMUM_COVERAGE - NOT IMPLEMENTED!");
			System.exit(0);
			break;
		case PREVALENT_SEGMENTS :
			// TODO: check this
			// Tally up segment counts
			for (Host h : infecteds) {
				for (Virus v : h.getInfections()) {
					for (Segment s : v.segments) {						
						if (segmentTallyForVaccineComposition.containsKey(s.getSegmentNumber())) {
							Pair<Segment,Integer> newCount = new Pair<Segment, Integer>(s,segmentTallyForVaccineComposition.get(s.getSegmentNumber()).getValue1()+1);
							segmentTallyForVaccineComposition.put(s.getSegmentNumber(),newCount);
						}
						else { 
							Pair<Segment,Integer> newCount = new Pair<Segment, Integer>(s,1);
							segmentTallyForVaccineComposition.put(s.getSegmentNumber(),newCount);
						}
					}
				}		
			}

			// Get Most Prevalent Segments
			Segment[] vaccineSegments = new Segment[Parameters.SegmentParameters.nSegments];
			for (int i = 0; i<Parameters.VaccineParameters.vaccineValancy; i++) {		
				Pair<Segment,Integer> prevalentSegmentTally = segmentTallyForVaccineComposition.values().iterator().next();

				for (Integer segmentIndex : segmentTallyForVaccineComposition.keySet()) {
					Integer currentSegmentTally = segmentTallyForVaccineComposition.get(segmentIndex).getValue1();				
					if (currentSegmentTally>prevalentSegmentTally.getValue1()) {
						prevalentSegmentTally =segmentTallyForVaccineComposition.get(segmentIndex);					
					}
				}
				vaccineSegments[i]=prevalentSegmentTally.getValue0();
				strainTallyForVaccineComposition.remove(prevalentSegmentTally.getValue0().getSegmentNumber());
			}
			Virus vaccineVirus = new Virus(vaccineSegments,(float)0.0);
			vaccineComposition.add(vaccineVirus);	
			break;
		case PREVALENT_STRAINS :
			// TODO: check this
			// Tally up strain compositions
			for (Host h : infecteds) {
				for (Virus v : h.getInfections()) {
					BitSet segmentIndices = v.getImmunogenicSegmentIndices();
					if (strainTallyForVaccineComposition.containsKey(segmentIndices)) {
						Pair<Virus,Integer> newCount = new Pair<Virus, Integer>(v,strainTallyForVaccineComposition.get((segmentIndices)).getValue1()+1);
						strainTallyForVaccineComposition.put(segmentIndices,newCount);
					}
					else {
						Pair<Virus,Integer> newCount = new Pair<Virus, Integer>(v,1);
						strainTallyForVaccineComposition.put(segmentIndices,newCount);
					}
				}		
			}

			// Get Most Prevalent Strains
			for (int i = 0; i<Math.min(Parameters.VaccineParameters.vaccineValancy,strainTallyForVaccineComposition.size()); i++) {		
				Pair<Virus,Integer> prevalentStrainTally = strainTallyForVaccineComposition.values().iterator().next();

				for (BitSet segmentIndices : strainTallyForVaccineComposition.keySet()) {
					Integer currentStrainTally = strainTallyForVaccineComposition.get(segmentIndices).getValue1();				
					if (currentStrainTally>prevalentStrainTally.getValue1()) {
						prevalentStrainTally =strainTallyForVaccineComposition.get(segmentIndices);					
					}
				}

				vaccineComposition.add(prevalentStrainTally.getValue0());
				strainTallyForVaccineComposition.remove(prevalentStrainTally.getValue0().getImmunogenicSegmentIndices());
			}
			strainTallyForVaccineComposition.clear();
			break;
		case NONE:
			break; // TODO: check if  this works
		default:
			break;
		}
	}

	public void printVaccine() {
		try {
			File vacFile = new File("out.vaccine");
			vacFile.delete();
			vacFile.createNewFile();
			PrintStream vacStream = new PrintStream(vacFile);
			for (int i=0;i<Parameters.SegmentParameters.nSegments;i++) {
				vacStream.printf("segment%d",i);
				if (i!=(Parameters.SegmentParameters.nSegments-1)) {
					vacStream.printf(",");
				}					
			}
			vacStream.printf("\n");		

			for (Virus vaccineStrain : vaccineComposition) {
				Segment[] vaccineStrainSegments = vaccineStrain.getSegments();
				for (int i=0; i<vaccineStrainSegments.length;i++) {
					vacStream.printf("%d",vaccineStrainSegments[i].getSegmentNumber());
					if (i!=(vaccineStrainSegments.length-1)) {
						vacStream.printf(",");
					}
				}
				vacStream.printf("\n");
			}
			vacStream.close();
		} catch(IOException ex) {
			System.out.println("Could not write to file out.vaccine"); 
			System.exit(0);
		}

	}	

}