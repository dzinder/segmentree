/* A population of host individuals */

import java.util.*;

import java.io.*;

//import org.javatuples.Pair;

public class HostPopulation {


	private int cases;	// number  of cases from last count, doesn't effect dynamics 

	// major classes
	private List<Host> susceptibles = new ArrayList<Host>(); 
	private List<Host> infecteds = new ArrayList<Host>(); // including  superinfecteds
	private List<Host> recoverds = new ArrayList<Host>(); // fully protected  

	// strain reservoir
	private List<Host> initialStrainReservoir = new ArrayList<Host>();

	// host samples
	private InfectedHostSamples infectedHostSamples = new InfectedHostSamples();
	private HostsForImmunitySamples hostsForImmunitySamples = new HostsForImmunitySamples();

	// Vaccine composition
	private HashMap<BitSet,Pair<Virus,Integer>> strainTallyForVaccineComposition = new HashMap<BitSet,Pair<Virus,Integer>>();	
	private List<Virus> vaccineComposition = new ArrayList<Virus>();
	
	// Vaccine queue
	private List<PriorityQueue<Host>> vaccineQueues = new ArrayList<PriorityQueue<Host>>(); // newborns to vaccinate ordered by birthday
	
	// CONSTRUCTORS & INITIALIZERS
	public HostPopulation() {		
		// Priority queue of newborn for vaccination, queues sorted by age 
		for (int i=0;i<Parameters.VaccineParameters.vaccinationAges.length;i++) {
			vaccineQueues.add(new PriorityQueue<Host>(100000, (h1,h2) -> h1.getBirth() - h2.getBirth()));
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
		recoverds.clear();
		strainTallyForVaccineComposition.clear();
		vaccineComposition.clear();
		
		// vaccine queues
		for (int i=0;i<vaccineQueues.size();i++) {
			vaccineQueues.get(i).clear();
		}

		// reservoir
		initialStrainReservoir.clear();
		if (Parameters.ReservoirParameters.proportionContactWithReservoir>0) {					
			for (Virus v : Parameters.getInitialViruses()) {
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
		    addHostToVaccineQueues(h);
			susceptibles.add(h);
		}
		System.out.println("finished constructing " + initialS + " initial susceptible hosts\n"); // display

		// infect some individuals
		for (int i = 0; i < Parameters.EpidemiologicalParameters.initialI; i++) {		
			Host h = new Host(true);
			addHostToVaccineQueues(h);
			h.infect(Parameters.getInitialViruses().get(Random.nextInt(0, Parameters.getInitialViruses().size()-1)));
			infecteds.add(h);
		}		
		System.out.println("finished constructing " + Parameters.EpidemiologicalParameters.initialI + " infected hosts\n"); // display

		// add initial immune history to some individuals
		for (int i = 0; i < Math.round(Parameters.EpidemiologicalParameters.initialPrR*Parameters.DemographicParameters.N); i++) {
			if (i%5000000 == 0 ) System.out.println("adding immune history: " + i + " out of " + Math.round(Parameters.EpidemiologicalParameters.initialPrR*Parameters.DemographicParameters.N)); 	// display		
			getRandomHost().addToImmuneHistory(Parameters.getInitialViruses().get(Random.nextInt(0, Parameters.getInitialViruses().size()-1)));
		}		
		System.out.println("finished setting up initial conditions for " +Math.round(Parameters.EpidemiologicalParameters.initialPrR*Parameters.DemographicParameters.N) + " hosts\n"); // display
	}

	private void addHostToVaccineQueues(Host h) {
		
		// if nearing vaccination program start day 
		if (Parameters.getDay()>=(Parameters.VaccineParameters.vaccinationProgramStartTime-Parameters.VaccineParameters.vaccinationAges[Parameters.VaccineParameters.vaccinationAges.length-1])) {
			for (int i=0;i<vaccineQueues.size();i++) {
				if (h.getAgeInDays()<=Parameters.VaccineParameters.vaccinationAges[i]) {
					vaccineQueues.get(i).add(h);	
				}
			}
		}
	}
	
	private void removeHostFromVaccineQueues(Host h) {		
		
		for (int i=0;i<vaccineQueues.size();i++) {
			vaccineQueues.get(i).remove(h);	
		}
	}

	// METHODS
	public int getN() {
		return susceptibles.size() + infecteds.size() + recoverds.size();
	}

	public int getS() {
		return susceptibles.size();
	}

	public int getR() {
		return recoverds.size();
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
	public int getRandomR() {
		return Random.nextInt(0,getR()-1);
	}
	public int getRandomI() {
		return Random.nextInt(0,getI()-1);
	}	

	public Host getRandomHost() {
		double n = Random.nextInt(0,getN()-1);
		if (n <= (getS()-1))
			return getRandomHostS();
		if (n <= (getS() + getI() - 1))
			return getRandomHostI();
		else
			return getRandomHostR();
	}

	public Host getRandomHostS() {
		return susceptibles.get(Random.nextInt(0,susceptibles.size()-1));
	}
	public Host getRandomHostI() {
		return infecteds.get(Random.nextInt(0,infecteds.size()-1));
	}
	public Host getRandomHostR() {
		return recoverds.get(Random.nextInt(0,recoverds.size()-1));
	}



	public void resetCases() {
		cases = 0;
	}

	public int getCases() {
		return cases;
	}	

	public void removeSusceptible(int i) {
		
		// remove from vaccine queue
		removeHostFromVaccineQueues(susceptibles.get(i));
				
		// remove by moving last susceptible to location i, and than shortening ArrayList		
		susceptibles.set(i, susceptibles.get(susceptibles.size()-1));
		susceptibles.remove(susceptibles.size()-1);

	}

	public void removeInfected(int i) {	
		// remove from vaccine queue
		removeHostFromVaccineQueues(infecteds.get(i));
				
		// remove by moving last infected to location i, and than shortening ArrayList
		infecteds.set(i, infecteds.get(infecteds.size()-1));
		infecteds.remove(infecteds.size()-1);
	}

	public void removeRecoverd(int i) {	
		// remove from vaccine queue
		removeHostFromVaccineQueues(recoverds.get(i));
				
		// remove by moving last infected to location i, and than shortening ArrayList
		recoverds.set(i, recoverds.get(recoverds.size()-1));
		recoverds.remove(recoverds.size()-1);
	}



	public void stepForward() {               

		if (Parameters.DemographicParameters.swapDemography) {
			swap(); // implement birth death by 'swapping' deaths with births
		} else {
			grow(); // birth
			decline(); // death
		}

		contact(); // contact between infected and the general population 
		
		recover();	// recover at a given rate
		
		loseImmunity(); // waning immunity at at a given rate
		
		mutate(); // introduce new segments without recycling with present day parents

		introduce(); // introduce new segments without recycling from initial reservoir strains

		reintroduce(); // reintroduce segments from initial reservoir strains

		vaccinate(); // vaccinate individuals based on policy

		disruption(); // apply abrupt change based on parameter specification

		contactReservoir(); // contact external strain reservoir

		sample(); // take samples for output files
	}

	private void vaccinate() {
		// N queues, one for each vaccine age, for unvaccinated newborns
		
		if (Parameters.day > Parameters.VaccineParameters.vaccinationProgramStartTime) {
			for (int i=0;i<vaccineQueues.size();i++) {
				
				// vaccinate individuals at the right age at top of queue
				Queue<Host> currentQueue = vaccineQueues.get(i);
				int currentVaccineAge = Parameters.VaccineParameters.vaccinationAges[i];

				boolean remainingToVaccinate = false;
				if (currentQueue.size()>=1) {
					if (currentQueue.peek().getAgeInDays()>=currentVaccineAge) {
						remainingToVaccinate = true;
					}
				}

				while (remainingToVaccinate) {
					Host vaccinatedHost = currentQueue.remove();
					vaccinatedHost.immunize(vaccineComposition);
				
					remainingToVaccinate = false;
					if (currentQueue.size()>=1) {
						if (currentQueue.peek().getAgeInDays()>=currentVaccineAge) {
							remainingToVaccinate = true;
						}
					}
				}
			}
		}

	}

	private void disruption() {
		if (Parameters.getDay() == Parameters.DisruptionParameters.disruptionTime1) {
			switch (Parameters.DisruptionParameters.disruptionType1) {
			case MASS_EXTINCTION :							
				int recoveries = Random.nextPoisson(getI()*Parameters.DisruptionParameters.disruptionParameter1);

				for (int i = 0; i < recoveries; i++) {
					if (getI()>0) {
						int index = getRandomI();
						Host h = infecteds.get(index);
						removeInfected(index);
						h.clearInfections();
						recoverds.add(h); // two options recover or move to suscptibles
					}					
				}
				break;
			case CHANGE_MUTATION :							
				Parameters.MutationAndReassortmentParameters.mu=Parameters.DisruptionParameters.disruptionParameter1;
				break;
			case CHANGE_INTRO :							
				Parameters.MutationAndReassortmentParameters.intro=Parameters.DisruptionParameters.disruptionParameter1;
				break;
			case CHANGE_REASSORTMENT :							
				Parameters.MutationAndReassortmentParameters.rho=Parameters.DisruptionParameters.disruptionParameter1;
				break;
			case NONE:
				break;
			default:
				break;
			}
		}

		if (Parameters.getDay() == Parameters.DisruptionParameters.disruptionTime2) {
			switch (Parameters.DisruptionParameters.disruptionType2) {
			case MASS_EXTINCTION :							
				int recoveries = Random.nextPoisson(getI()*Parameters.DisruptionParameters.disruptionParameter2);

				for (int i = 0; i < recoveries; i++) {
					if (getI()>0) {
						int index = getRandomI();
						Host h = infecteds.get(index);
						removeInfected(index);
						h.clearInfections();
						recoverds.add(h); // two options recover or move to suscptibles
					}					
				}
				break;
			case CHANGE_MUTATION :							
				Parameters.MutationAndReassortmentParameters.mu=Parameters.DisruptionParameters.disruptionParameter2;
				break;
			case CHANGE_INTRO :							
				Parameters.MutationAndReassortmentParameters.intro=Parameters.DisruptionParameters.disruptionParameter2;
				break;
			case CHANGE_REASSORTMENT :							
				Parameters.MutationAndReassortmentParameters.rho=Parameters.DisruptionParameters.disruptionParameter2;
				break;
			case NONE:
				break;
			default:
				break;
			}
		}

		if (Parameters.getDay() == Parameters.DisruptionParameters.disruptionTime3) {
			switch (Parameters.DisruptionParameters.disruptionType3) {
			case MASS_EXTINCTION :							
				int recoveries = Random.nextPoisson(getI()*Parameters.DisruptionParameters.disruptionParameter3);

				for (int i = 0; i < recoveries; i++) {
					if (getI()>0) {
						int index = getRandomI();
						Host h = infecteds.get(index);
						removeInfected(index);
						h.clearInfections();
						recoverds.add(h); // two options recover or move to suscptibles
					}					
				}
				break;
			case CHANGE_MUTATION :							
				Parameters.MutationAndReassortmentParameters.mu=Parameters.DisruptionParameters.disruptionParameter3;
				break;
			case CHANGE_INTRO :							
				Parameters.MutationAndReassortmentParameters.intro=Parameters.DisruptionParameters.disruptionParameter3;
				break;
			case CHANGE_REASSORTMENT :							
				Parameters.MutationAndReassortmentParameters.rho=Parameters.DisruptionParameters.disruptionParameter3;
				break;
			case NONE:
				break;
			default:
				break;
			}
		}

		if (Parameters.getDay() == Parameters.DisruptionParameters.disruptionTime4) {
			switch (Parameters.DisruptionParameters.disruptionType4) {
			case MASS_EXTINCTION :							
				int recoveries = Random.nextPoisson(getI()*Parameters.DisruptionParameters.disruptionParameter4);

				for (int i = 0; i < recoveries; i++) {
					if (getI()>0) {
						int index = getRandomI();
						Host h = infecteds.get(index);
						removeInfected(index);
						h.clearInfections();
						recoverds.add(h); // two options recover or move to suscptibles
					}					
				}
				break;
			case CHANGE_MUTATION :							
				Parameters.MutationAndReassortmentParameters.mu=Parameters.DisruptionParameters.disruptionParameter4;
				break;
			case CHANGE_INTRO :							
				Parameters.MutationAndReassortmentParameters.intro=Parameters.DisruptionParameters.disruptionParameter4;
				break;
			case CHANGE_REASSORTMENT :							
				Parameters.MutationAndReassortmentParameters.rho=Parameters.DisruptionParameters.disruptionParameter4;
				break;
			case NONE:
				break;
			default:
				break;
			}
		}


	}

	private void mutate() {		

		double totalMutationRate = getI() * Parameters.MutationAndReassortmentParameters.mu;
		int mutations = Random.nextPoisson(totalMutationRate);
		for (int i = 0; i < mutations; i++) {
			getRandomHostI().mutate();
		}
	}


	private void introduce() {		

		double totalIntroRate = Parameters.MutationAndReassortmentParameters.intro;
		int introductions = Random.nextPoisson(totalIntroRate);
		for (int i = 0; i < introductions; i++) {
			getRandomHostI().introduce();
		}
	}

	private void reintroduce() {		

		double totalReIntroRate = Parameters.ReservoirParameters.reintro;
		int reintroductions = Random.nextPoisson(totalReIntroRate);
		for (int i = 0; i < reintroductions; i++) {
			getRandomHostI().reintroduce();
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
			addHostToVaccineQueues(h);
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
		// deaths in recoverd's class		
		totalDeathRate = getR() * Parameters.DemographicParameters.deathRate;
		deaths = Random.nextPoisson(totalDeathRate);
		for (int i = 0; i < deaths; i++) {
			if (getR()>0) {
				int index = getRandomR();
				removeRecoverd(index);
			}
		}	
		
		
	}

	private void contactReservoir() {
		// each infected (or superinfected) makes contacts on a per-day rate of propContactWithReservoir*beta*reservoirSize*S/N
		double susceptibleContactRate = initialStrainReservoir.size()*getPrS()*Parameters.EpidemiologicalParameters.beta*Parameters.ReservoirParameters.proportionContactWithReservoir;
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
					double chanceOfSuccess = sH.riskOfInfection(v)*iH.getRiskOfTransmission(v); 
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
						double chanceOfSuccess = sH.riskOfInfection(v)*iH.getRiskOfTransmission(v); 
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

		// Contact with infecteds
		// each infected (or superinfected) makes contacts on a per-day rate of propContactWithReservoir*beta*reservoirSize*I/N
		double infectedContactRate = initialStrainReservoir.size()*getPrI()*Parameters.EpidemiologicalParameters.beta*Parameters.ReservoirParameters.proportionContactWithReservoir;
		contacts = Random.nextPoisson(infectedContactRate);
		for (int i = 0; i < contacts; i++) {
			if (getI()>0) {
				// get indices and objects
				Host fromH = initialStrainReservoir.get(Random.nextInt(0, initialStrainReservoir.size()-1));
				int index=getRandomI();
				Host toH = infecteds.get(index);			

				if (!fromH.isSuperinfected()) {
					// attempt infection
					Virus v = fromH.getRandomInfection();
					double chanceOfSuccess = toH.riskOfInfection(v);//*fromH.getRiskOfTransmission(); 
					if (Random.nextBoolean(chanceOfSuccess)) {
						toH.infect(v);
						cases++; // doesn't effect dynamics
					}
				} else {
					boolean infected = false; 
					// for superinfected host:
					// Pick n_bottleNeck random viruses, for each segment with probability rho replace it with segment from all infecting viruses
					// viruses transmits based on individual probability		
					for (int j=0; j<Parameters.MutationAndReassortmentParameters.n_bottleNeck; j++) {
						Virus v = fromH.getRandomInfection();
						double chanceOfSuccess = toH.riskOfInfection(v);//*fromH.getRiskOfTransmission(); 
						if (Random.nextBoolean(chanceOfSuccess)) {
							infected = true;
							toH.infect(v);
						}
					}
					if (infected) {				
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
				int index = getRandomS();
				// remove from vaccine queue
				Host h = susceptibles.get(index);
				removeSusceptible(index);
				h.reset();
				susceptibles.add(h);
				addHostToVaccineQueues(h);
				
			}
		}		

		// draw random individuals from infected class
		totalBirthRate = getI() * Parameters.DemographicParameters.birthRate;
		births = Random.nextPoisson(totalBirthRate);

		// remove infected and add to susceptible births
		for (int i = 0; i < births; i++) {			
			if (((Parameters.getDay()>Parameters.SimulationParameters.burnin) || (!Parameters.SimulationParameters.keepAliveDuringBurnin)) && (!Parameters.SimulationParameters.keepAlive)) {
				if (getI()>0) {
					int index = getRandomI();
					Host h = infecteds.get(index);		
					removeInfected(index);
					h.reset();					
					susceptibles.add(h);
					addHostToVaccineQueues(h);
				}
			}
			else if ((Parameters.getDay()<Parameters.SimulationParameters.burnin || Parameters.SimulationParameters.keepAlive) && getI()>1) {
				int index = getRandomI();
				Host h = infecteds.get(index);
				removeInfected(index);
				h.reset();
				susceptibles.add(h);
				addHostToVaccineQueues(h);
			}
		}

		// draw random individuals from recoverd's class
		totalBirthRate = getR() * Parameters.DemographicParameters.birthRate;
		births = Random.nextPoisson(totalBirthRate);
		for (int i = 0; i < births; i++) {
			if (getR()>0) {
				int index = getRandomR();
				Host h = recoverds.get(index);		
				removeRecoverd(index);
				h.reset();					
				susceptibles.add(h);
				addHostToVaccineQueues(h);
				
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
					double chanceOfSuccess = sH.riskOfInfection(v)*iH.getRiskOfTransmission(v); 
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
						double chanceOfSuccess = sH.riskOfInfection(v)*iH.getRiskOfTransmission(v); 
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

			if (!fromH.isSuperinfected()) {
				// attempt infection
				Virus v = fromH.getRandomInfection();
				double chanceOfSuccess = toH.riskOfInfection(v)*fromH.getRiskOfTransmission(v); 
				if (Random.nextBoolean(chanceOfSuccess)) {
					toH.infect(v);
					cases++; // doesn't effect dynamics
				}
			} else {
				// for superinfected host:
				// Pick n_bottleNeck random viruses, for each segment with probability rho replace it with segment from all infecting viruses
				// viruses transmits based on individual probability
				boolean infected = false; 
				for (int j=0; j<Parameters.MutationAndReassortmentParameters.n_bottleNeck; j++) {
					Virus v = fromH.getRandomInfection();
					double chanceOfSuccess = toH.riskOfInfection(v)*fromH.getRiskOfTransmission(v); 
					if (Random.nextBoolean(chanceOfSuccess)) {
						infected = true;
						toH.infect(v);
					}
				}
				if (infected) {				
					cases++; // doesn't effect dynamics
				}
			}			
		}

	}


	// draw a Poisson distributed number of recoveries and move from I->R based upon this
	public void recover() {
		// each infected recovers at a per-day rate of nu
		// infected clear from multiple infections simultaneously

		double totalRecoveryRate = getI() * Parameters.EpidemiologicalParameters.nu;
		int recoveries = Random.nextPoisson(totalRecoveryRate);

		for (int i = 0; i < recoveries; i++) {
			if (((Parameters.getDay()>Parameters.SimulationParameters.burnin) || (!Parameters.SimulationParameters.keepAliveDuringBurnin)) && (!Parameters.SimulationParameters.keepAlive)) {
				if (getI()>0) {
					int index = getRandomI();
					Host h = infecteds.get(index);
					removeInfected(index);
					h.clearInfections();
					recoverds.add(h);
				}
			}
			else if ((Parameters.getDay()<Parameters.SimulationParameters.burnin || Parameters.SimulationParameters.keepAlive) && getI()>1) {
				int index = getRandomI();
				Host h = infecteds.get(index);
				removeInfected(index);
				h.clearInfections();					
				recoverds.add(h);
			}
		}
	}

	// draw a Poisson distributed number of recoverds and move to susceptible class
	public void loseImmunity() {

		if (Double.isInfinite(Parameters.EpidemiologicalParameters.omega)) {
			for (int index=recoverds.size()-1;index>=0;index--) {		
				Host h = recoverds.get(index);
				removeRecoverd(index);
				susceptibles.add(h);				
			}
		} 
		else {
			// each recoverd loses immuntiy at a per-day rate of omega
			// recoverds are fully protected

			double totalRecoveryRate = getR() * Parameters.EpidemiologicalParameters.omega;
			int recoveries = Random.nextPoisson(totalRecoveryRate);

			for (int i = 0; i < recoveries; i++) {		
				if (getR()>0) {
					int index = getRandomR();
					Host h = recoverds.get(index);
					removeRecoverd(index);
					susceptibles.add(h);
				}
			}
		}
	}		


	public void sample() {
		if (getI()>0 && Parameters.getDay() >= Parameters.SimulationParameters.burnin) {
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
		if (Parameters.getDay() > Parameters.SimulationParameters.burnin) {
			stream.printf("\t%d\t%d\t%d\t%d\t%d", getN(), getS(), getI(), getR(), getCases());
		}
	}	

	public List<Host> getIs() {	
		return infecteds;
	}

	public void determineVaccineComposition() {
		switch (Parameters.VaccineParameters.vaccineMakeup) {
		
		case PREVALENT_STRAINS :
		
			// Tally up strain compositions
			for (Host h : infecteds) {
				for (Virus v : h.getInfections()) {
					BitSet segmentIndices = v.getImmunogenicSegmentIndices();
					if (strainTallyForVaccineComposition.containsKey(segmentIndices)) {
						Pair<Virus,Integer> newCount = new Pair<Virus, Integer>(v,strainTallyForVaccineComposition.get((segmentIndices)).getValue()+1);
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
					Integer currentStrainTally = strainTallyForVaccineComposition.get(segmentIndices).getValue();				
					if (currentStrainTally>prevalentStrainTally.getValue()) {
						prevalentStrainTally =strainTallyForVaccineComposition.get(segmentIndices);					
					}
				}

				vaccineComposition.add(prevalentStrainTally.getKey());
				strainTallyForVaccineComposition.remove(prevalentStrainTally.getKey().getImmunogenicSegmentIndices());
			}
			strainTallyForVaccineComposition.clear();
			break;
		case NONE:
			break; 
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