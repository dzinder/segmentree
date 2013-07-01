/* A population of host individuals */

import java.util.*;
import java.io.*;

import org.omg.Dynamic.Parameter;
import org.omg.PortableInterceptor.SUCCESSFUL;

public class HostPopulation {

	// fields
	private int cases;	// number of cases from last count, doesn't effect dynamics 
	private List<Host> susceptibles = new ArrayList<Host>(); 
	private List<Host> infecteds = new ArrayList<Host>(); // including superinfecteds
	
	// host samples
	private InfectedHostSamples infectedHostSamples = new InfectedHostSamples();
	private HostsForImmunitySamples hostsForImmunitySamples = new HostsForImmunitySamples(); 

	// CONSTRUCTORS & INITIALIZERS
	public HostPopulation() {			
	}
	
	// reset population to factory condition
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

		// fill population with susceptibles
		int initialS = Parameters.N;
		initialS -= Parameters.initialI; // minus initial number of infected

		for (int i = 0; i < initialS; i++) {			
			if (i%5000000 == 0 ) System.out.println("adding hosts: " + i + " out of " + initialS); 	// display											
			Host h = new Host();			
			susceptibles.add(h);											
		}
		System.out.println("finished constructing " + initialS + " initial susceptible hosts\n"); // display

		// infect some individuals
		for (int i = 0; i < Parameters.initialI; i++) {		
			Host h = new Host();
			h.infect(Parameters.initialViruses.get(Random.nextInt(0, Parameters.initialViruses.size()-1)));
			infecteds.add(h);
		}		
		System.out.println("finished constructing " + Parameters.initialI + " infected hosts\n"); // display

		// add initial immune history to some individuals
		for (int i = 0; i < Math.round(Parameters.initialPrR*Parameters.N); i++) {
			if (i%5000000 == 0 ) System.out.println("adding immune history: " + i + " out of " + Math.round(Parameters.initialPrR*Parameters.N)); 	// display		
			getRandomHost().addToImmuneHistory(Parameters.initialViruses.get(Random.nextInt(0, Parameters.initialViruses.size()-1)));
		}		
		System.out.println("finished immunizing " +Math.round(Parameters.initialPrR*Parameters.N) + " hosts\n"); // display
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

		if (Parameters.swapDemography) {
			swap();
		} else {
			grow();
			decline();
		}

		contact(); // reassortants are sampled on contact
		recover();			
		mutate(); // introduce new segments without recycling

		sample(); // doesn't effect dynamics

	}

	private void mutate() {		

		double totalMutationRate = getI() * Parameters.mu;
		int mutations = Random.nextPoisson(totalMutationRate);
		for (int i = 0; i < mutations; i++) {
			getRandomHostI().mutate();
		}
	}

	// draw a Poisson distributed number of births and add these hosts to the end of the population list
	// new hosts are always born naive
	public void grow() {
		double totalBirthRate = getN() * Parameters.birthRate;
		int births = Random.nextPoisson(totalBirthRate);
		for (int i = 0; i < births; i++) {
			Host h = new Host();
			susceptibles.add(h);
		}
	}

	// draw a Poisson distributed number of deaths and remove random hosts from the population list
	public void decline() {
		// deaths in susceptible class
		double totalDeathRate = getS() * Parameters.deathRate;
		int deaths = Random.nextPoisson(totalDeathRate);
		for (int i = 0; i < deaths; i++) {
			if (getS()>0) {
				int sndex = getRandomS();
				removeSusceptible(sndex);
			}
		}		
		// deaths in infectious class		
		totalDeathRate = getI() * Parameters.deathRate;
		deaths = Random.nextPoisson(totalDeathRate);
		for (int i = 0; i < deaths; i++) {
			if (getI()>0) {
				int index = getRandomI();
				removeInfected(index);
			}
		}		

	}

	// draw a Poisson distributed number of births and reset these individuals
	public void swap() {
		// draw random individuals from susceptible class
		double totalBirthRate = getS() * Parameters.birthRate;
		int births = Random.nextPoisson(totalBirthRate);
		for (int i = 0; i < births; i++) {
			if (getS()>0) {
				getRandomHostS().reset();
			}
		}		

		// draw random individuals from infected class
		totalBirthRate = getI() * Parameters.birthRate;
		births = Random.nextPoisson(totalBirthRate);

		// remove infected and add to susceptible births
		for (int i = 0; i < births; i++) {			
			if (((Parameters.day>Parameters.burnin) || (!Parameters.keepAliveDuringBurnin)) && (!Parameters.keepAlive)) {
				if (getI()>0) {
					int index = getRandomI();
					Host h = infecteds.get(index);		
					removeInfected(index);
					h.reset();					
					susceptibles.add(h);
				}
			}
			else if ((Parameters.day<Parameters.burnin || Parameters.keepAlive) && getI()>1) {
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
		double susceptibleContactRate = getI()* getPrS()*Parameters.beta;
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
					for (int j=0; j<Parameters.n_bottleNeck; j++) {
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

		// each infected (or superinfected) makes contact on a per-day rate of beta*I*I/N
		double infectedContactRate = getI()* getPrI()*Parameters.beta;
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

		double totalRecoveryRate = getI() * Parameters.nu;
		int recoveries = Random.nextPoisson(totalRecoveryRate);

		for (int i = 0; i < recoveries; i++) {
			if (((Parameters.day>Parameters.burnin) || (!Parameters.keepAliveDuringBurnin)) && (!Parameters.keepAlive)) {
				if (getI()>0) {
					int index = getRandomI();
					Host h = infecteds.get(index);
					removeInfected(index);
					h.clearInfections();
					susceptibles.add(h);
				}
			}
			else if ((Parameters.day<Parameters.burnin || Parameters.keepAlive) && getI()>1) {
				int index = getRandomI();
				Host h = infecteds.get(index);
				removeInfected(index);
				h.clearInfections();					
				susceptibles.add(h);
			}


		}

	}			

	public void sample() {
		if (getI()>0 && Parameters.day >= Parameters.burnin) {
			// Sample infected hosts for out.infected
			int numInfectedHostSamples = Random.nextPoisson(Parameters.infectedHostSamplingRate*getI());
			
			for (int i=0; i<numInfectedHostSamples; i++) {
				Host h = getRandomHostI();					
				for (Virus v : h.getInfections()) {
					for (Segment s : v.getSegments()) {												
						infectedHostSamples.add(h,v,s);
					}
				}				
			}
			
			// Sample all hosts for out.immunity
			int numImmunityHostSamples = Random.nextPoisson(Parameters.immunityHostSamplingRate*getN());
			
			for (int i=0; i<numImmunityHostSamples; i++) {
				hostsForImmunitySamples.add(getRandomHost());
			}
			
			// Sample tree tips
			double totalSamplingRate = Parameters.tipSamplingRate;
			if (Parameters.tipSamplingProportional) 
				totalSamplingRate *= getI();
			else 
				totalSamplingRate *= getN();			

			int samples = Random.nextPoisson(totalSamplingRate);

			if (Parameters.sampleWholeGenomes) { // sample whole genomes of viruses
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
		if (Parameters.day > Parameters.burnin) {
			stream.printf("\t%d\t%d\t%d\t%d\t%d", getN(), getS(), getI(),0 /* getR()*/, getCases());
		}
	}	

	public List<Host> getIs() {	
		return infecteds;
	}

}