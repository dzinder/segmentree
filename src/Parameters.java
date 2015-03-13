/* Stores parameters for use across simulation */
/* Start with parameters in source, implement input file later */
/* A completely static class.  */

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

enum PhenotypeType {SEGMENTED};
enum VaccineMakeup {NONE, PREVALENT_SEGMENTS, PREVALENT_STRAINS, MAXIMUM_COVERAGE};
enum DisruptionType {NONE, MASS_EXTINCTION, CHANGE_MUTATION, CHANGE_INTRO, CHANGE_REASSORTMENT};

public class Parameters {

	public static class SimulationParameters {
		// Simulation Parameterss
		@Setting (description ="Burn In time in days. Initial run time without recording output."	) 
		static int burnin = 365*50; 	
		@Setting (description ="Simulation end time in days."	) 
		static int endDay = 365*150; 	
		@Setting (description ="Repeat simulation following a stochastic extinction until endDay is reached."	) 
		static boolean repeatSim = true;		
		@Setting (description ="Prevent stochastic extinction during burn-in period by maintaining at least one infected individual...." ) 
		static boolean keepAliveDuringBurnin = true; 	
		@Setting (description = "Prevent stochastic extinction by maintaining at least one infected individual.... " )
		static boolean keepAlive = true;
	}

	public static class SamplingParameters {
		// Sampling and Tree Sampling Parameters	
		@Setting (description ="print to out.timeseries every X days.")
		static int printStepTimeseries = 7;	
		@Setting (description ="print to out.immunity every X days")
		static int printStepImmunity = 7;						
		@Setting (description ="in proportion samples per day")
		static double tipSamplingRate = 2.0E-4;				    
		@Setting (description ="true to set sampling proportional to prevalence (vs. population size)" )
		static boolean tipSamplingProportional = true;		
		@Setting (description ="proportion of tips to use in tree reconstruction" )
		static double treeProportion = 1.0E-6;	
		@Setting (description ="interval used for sampling subset of tips to be marked" ) 
		static double intervalForMarkTips = 2.0;
		@Setting (description ="how many tips to sample when estimating diversity" )
		static int	diversitySamplingCount = 50;
		@Setting (description ="subtract this many years off the end of the tree when designating trunk" )
		static double yearsToTrunk = 5;			
		@Setting (description = "Sample whole genomes for tips rather than random samples.... " )
		static boolean sampleWholeGenomes = true; 
		@Setting (description = "Infected host sampling rate for out.infected" )
		static double infectedHostSamplingRate = 1E-3; 
		@Setting (description = "Host sampling rate for out.immunity" )
		static double immunityHostSamplingRate = 1E-5;
		
		// Memory Optimization Parameters
//		@Setting (description ="interval at which to streamline tree (optimize memory usage)" )
//		static int treeStreamlineInterval = 5000;
	}

	public static class DemographicParameters {
		// Host & Host Population Parameters & Settings
		@Setting (description ="Number of hosts in population" )
		static int N = 10000000;								
		@Setting (description ="in births per individual per day, i.e. 1/(30*365)" )
		static double birthRate = 1.0/(30.0*365.0);	
		@Setting (description ="in deaths per individual per day, i.e. 1/(30*365)" )
		static double deathRate = 1.0/(30.0*365.0);	
		@Setting (description ="whether to keep overall population size constant" )
		static boolean swapDemography = true;	
	}


	public static class EpidemiologicalParameters {
		// Infection & Epidemiology Parameters
		@Setting (description ="initial number of infected individuals" )
		static int initialI = 1;
		@Setting (description ="proportion recovered to intial virus/es (multiple recoveries for value greater than 1)" )
		static double initialPrR = 8.0; 
		@Setting (description ="in contacts per individual per day" )
		static double beta = 24.6/7.0; //3.72/7.0 
		@Setting (description ="in recoveries per individual per day" )
		static double nu = 1.0/7.0;
		@Setting (description ="in waning immunity per individual per day (Double.POSITIVE_INFINTY for no waning immunity)" )
		static double omega = Double.POSITIVE_INFINITY; 
	}

	public static class SegmentParameters {
		// Virus Segment Parameters
		@Setting (description ="number of viral segments" ) 
		static int nSegments = 3;
		@Setting (description ="number of immunogenic segments (only these first n segments will generate effective immunity)" ) 
		static int nImmunogenicSegments = 3;
		@Setting (description ="number of inital segment allels" ) 
		static int[] nInitialSegmentAllels = {1,1,1};
		@Setting (description ="number of inital random viral segement combinations" ) 
		static int nInitialStrains = 1;
	}

	public static class MutationAndReassortmentParameters {
		@Setting (description ="introduction rate - in segment introductions per day" )
		static double intro = 8.0/365.0;
		// Mutation & Reassortment Parameters
		@Setting (description ="mutation rate - in mutations per infected host per day" )
		static double mu = 0;//1E-4;
		@Setting (description ="reassortment probability - the probability of a segment to be randomly chosen from all possible\n"
							 + "                           segments during transmission from a superinfection" )
		static double rho = 1.0;			    
		@Setting (description ="infection bottle neck size - at most number of segment combinations to be transmitted from a superinfected host" )
		static double n_bottleNeck = 1;
		//TODO: @Setting (description ="post hoc neutral segment mutation rate" )
		//static double postHocMutateNeutral = 0.1;
	}

	public static class ReservoirParameters {
		// Strain Reservoir Parameters 
		@Setting (description ="contact with initial strain reservoir as proporiton of beta" )
		static double proportionContactWithReservoir = 0.000;
		@Setting (description ="introduction rate - in segment introductions per day" )
		static double reintro = 0.0/365.0;
	}


//	public static class VaccineParameters {
//		// Vaccine Parameters
//		@Setting (description ="vaccination ages in days (must be inputed in increasing order)" )
//		static int[] vaccinationAges = {2*30, 4*30,6*30}; 
//		@Setting (description ="vaccine proportion" )
//		static double vaccineP = 0.9;
//		@Setting (description ="vaccine makeup - PREVALENT_SEGMENTS or PREVALENT_STRAINS or MAXIMUM_COVERGE" )
//		static VaccineMakeup vaccineMakeup = VaccineMakeup.NONE;
//		@Setting (description ="vaccine valancy - number of strains or segments in vaccine" )
//		static int vaccineValancy = 2;
//		@Setting (description ="time of vaccination program start in days" )
//		static int vaccinationProgramStartTime = 365*7000; // days
//	}

	public static class DisruptionParameters {
		// Population Disruption Parameters
		@Setting (description ="distruptive interruption time" )
		static int disruptionTime = 365*100;
		@Setting (description ="disruption type" )
		static DisruptionType disruptionType = DisruptionType.CHANGE_INTRO;
		@Setting (description ="disruption parameter (fraction extinction for mass extinciton, new mutation/introduction/reassortment_rate for change mutation/intro/reassortment)" )
		static double disruptionParameter = 0.00; 
	}

	// "Global" fields 	
	private static int day = 0;
	private static List<Virus> initialViruses = null;
	private static Segment urSegment;
	
	// Settings
	static Settings s;

	// measured in years, starting at Parameters.SimulationParameters.burnin
	public static float getDate() {
		return (float) (((float) getDay() - (float) Parameters.SimulationParameters.burnin ) / 365.0 );
	}

	public static void init() {

		day = 0;

		s.apply(SimulationParameters.class);
		s.apply(SamplingParameters.class);
		s.apply(EpidemiologicalParameters.class);
		s.apply(DemographicParameters.class);
		s.apply(SegmentParameters.class);
		s.apply(MutationAndReassortmentParameters.class);
		s.apply(ReservoirParameters.class);
//		s.apply(VaccineParameters.class);
		s.apply(DisruptionParameters.class);
		s.apply(Random.class);

		Random.init();

		s.apply(ImmuneSystemDiscrete.ImmunityParameters.class);		
		ImmuneSystemDiscrete.updateImmunogenicSegmentMask(Parameters.SegmentParameters.nImmunogenicSegments);

		setUrSegment(new Segment()); // root to all segments		
		setInitialViruses(new ArrayList<Virus>());

		// Construct initial segment types
		List<Segment> initialSegmentRoots=new ArrayList<Segment>();
		List<List<Segment>> initialSegments = new ArrayList<List<Segment>>(); 

		for (short i=0;i<Parameters.SegmentParameters.nSegments;i++) {
			initialSegmentRoots.add(getUrSegment().mutate((short)i));
			initialSegments.add(new ArrayList<Segment>());
			for (short j=0;j<Parameters.SegmentParameters.nInitialSegmentAllels[i];j++) {
				initialSegments.get(i).add(initialSegmentRoots.get(i).mutate());
			}
		}

		// Construct initial random strains
		for (short i=0;i<Parameters.SegmentParameters.nInitialStrains;i++) {
			Segment[] viralSegments = new Segment[Parameters.SegmentParameters.nSegments];
			for (short j=0;j<Parameters.SegmentParameters.nSegments;j++) {
				viralSegments[j]=initialSegments.get(j).get(Random.nextInt(0, Parameters.SegmentParameters.nInitialSegmentAllels[j]-1));
			}
			Parameters.getInitialViruses().add(new Virus(viralSegments,0));
		}

	}

	public static void applyArgs(String[] args) {
		s = new Settings(args);
		s.apply(Parameters.class);
	}

	public static void printParams() {

		try {
			File paramFile = new File("out.params");
			paramFile.delete();
			paramFile.createNewFile();
			PrintStream paramStream = new PrintStream(paramFile);
			Settings.printSettings(Parameters.class, paramStream);
			Settings.printSettings(SimulationParameters.class, paramStream);
			Settings.printSettings(Parameters.SamplingParameters.class, paramStream);
			Settings.printSettings(Parameters.DemographicParameters.class, paramStream);
			Settings.printSettings(Parameters.EpidemiologicalParameters.class, paramStream);
			Settings.printSettings(Parameters.SegmentParameters.class, paramStream);
			Settings.printSettings(Parameters.MutationAndReassortmentParameters.class, paramStream);
			Settings.printSettings(Parameters.ReservoirParameters.class, paramStream);
//			Settings.printSettings(Parameters.VaccineParameters.class, paramStream);
			Settings.printSettings(Parameters.DisruptionParameters.class, paramStream);
			Settings.printSettings(Random.class, paramStream); 	
			Settings.printSettings(ImmuneSystemDiscrete.ImmunityParameters.class, paramStream); 	
			paramStream.print((new GregorianCalendar()).getTime());												
			paramStream.close();
		} catch(IOException ex) {
			System.out.println("Could not write to file out.params!"); 
			System.exit(0);
		}
	}

	public static int getDay() {
		return day;
	}

	public static void setDay(int newDay) {
		day = newDay;		
	}

	public static List<Virus> getInitialViruses() {
		return initialViruses;
	}

	public static void setInitialViruses(List<Virus> initialViruses) {
		Parameters.initialViruses = initialViruses;
	}

	public static Segment getUrSegment() {
		return urSegment;
	}

	public static void setUrSegment(Segment urSegment) {
		Parameters.urSegment = urSegment;
	}


}