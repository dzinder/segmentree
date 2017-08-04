/* Simulation functions, holds the host population */

import java.io.*;

public class Simulation {

	// fields
	private HostPopulation hp = new HostPopulation();
	private double diversity;
	private int totalCases=0;


	// constructor
	public Simulation() {

	}

	// methods

	public int getN() {
		return hp.getN();
	}

	public int getS() {
		return hp.getS();
	}	

	public int getI() {
		return hp.getI();
	}	
	
	public int getR() {
		return hp.getR();
	}	

	public int getCases() {
		return hp.getCases();
	}	

	public double getDiversity() {
		return diversity;
	}	


	public Virus getRandomInfection() {
		return hp.getRandomHostI().getRandomInfection(); 
	}

	// return random host from random deme
	public Host getRandomHost() {
		return hp.getRandomHost();
	}


	public void makeTrunk() {
		hp.makeTrunk();
	}	

	public void printState() {
		System.out.printf("%d\t%.3f\t%d\t%d\t%d\t%d\t%d\n", Parameters.getDay(), getDiversity(), getN(), getS(), getI(), getR() , getCases());
	}

	public void printHeader(PrintStream stream) {
		stream.print("date\tdiversity\ttotalN\ttotalS\ttotalI\ttotalR\ttotalCases");
		stream.println();
	}

	public void printState(PrintStream stream) {
		if (Parameters.getDay() > Parameters.SimulationParameters.burnin) {
			stream.printf("%.4f\t%.4f\t%d\t%d\t%d\t%d\t%d", Parameters.getDate(), getDiversity(), getN(), getS(), getI(), getR(), getCases());
			totalCases+=getCases();
			stream.println();
		}
	}	


	public void updateDiversity() {
		// Diversity
		diversity = 0.0;
		int sampleCount = Parameters.SamplingParameters.diversitySamplingCount;
		for (int i = 0; i < sampleCount; i++) {
			Segment vA = getRandomInfection().getSegments()[0];
			Segment vB = getRandomInfection().getSegments()[0];;
			if (vA != null && vB != null) {
				diversity += vA.distance(vB);
			}
		}	
		diversity /= (double) sampleCount;
	}	

	public void resetCases() {
		hp.resetCases();
	}

	public void stepForward() {
		hp.stepForward();
		Parameters.setDay(Parameters.getDay() + 1);
	}

	public void run() {

		try {

			File seriesFile = new File("out.timeseries");		
			seriesFile.delete();
			seriesFile.createNewFile();
			PrintStream seriesStream = new PrintStream(seriesFile);	
			System.out.println("day\t\tdiversity\tN\tS\tI\tR\tcases");
			printHeader(seriesStream);

			for (int i = 0; i < Parameters.SimulationParameters.endDay; i++) {

				stepForward(); // population dynamics

				if (Parameters.getDay() % Parameters.SamplingParameters.printStepTimeseries == 0) { // output
					updateDiversity();
					printState();
					printState(seriesStream);
					resetCases();
				}
				
				if (Parameters.getDay() == (Parameters.VaccineParameters.vaccinationProgramStartDay + Parameters.SimulationParameters.burnin) ) {
					determineVaccineComposition();					
				}

				if (getI()==0) {
					if (Parameters.SimulationParameters.repeatSim) {
						reset();
						i = 0; 
						seriesFile.delete();
						seriesFile.createNewFile();						
						seriesStream = new PrintStream(seriesFile);
						printHeader(seriesStream);
					} else {
						break;
					}
				}
				

			}

			seriesStream.close();
		} catch(IOException ex) {
			System.out.println("Could not write to file"); 
			System.exit(0);
		}	
		
		// tree reduction
		SegmentTree.pruneTips(); 
		SegmentTree.markTips();		

		// tree prep
		makeTrunk();
		SegmentTree.fillBackward();			
		SegmentTree.sortChildrenByDescendants();
		SegmentTree.setLayoutByDescendants();
		SegmentTree.streamline();
	
		// tip and tree output
		SegmentTree.printTips();
		SegmentTree.printBranches();	

		// mk output
		SegmentTree.printMK();
		
		// vaccine output
		hp.printVaccine();
		
		// close streams in host population
		hp.close();

	}	

	private void determineVaccineComposition() {	
		hp.determineVaccineComposition();				
	}

	public void reset() {
		Parameters.init();
		SegmentTree.init();	
		hp.reset();		
		diversity =0;
		totalCases=0;
	}


	public double getYearlyIncidancePercent() {	
		return ((double)totalCases)*365.0/(Parameters.SimulationParameters.endDay-Parameters.SimulationParameters.burnin)/getN()*100;
	}

}