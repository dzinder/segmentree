import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.GregorianCalendar;


/* Implements an individual-based model in which the infection's genealogical history is tracked through time */

class Main {
	public static void main(String[] args) {
		
		if (args.length>0) {
			if (args[0].equalsIgnoreCase("?") || args[0].equalsIgnoreCase("--help") || args[0].equalsIgnoreCase("-h") || args[0].equalsIgnoreCase("-help")) {
				printHelp();
				return;
			}
		}

		// initialize random  number generator
		cern.jet.random.AbstractDistribution.makeDefaultGenerator();		
				
		Parameters.applyArgs(args);				
		
		Simulation sim = new Simulation();
		sim.reset();
		Parameters.printParams();
		sim.run();
		
		try {
			File resultFile = new File("out.results");
			resultFile.delete();
			resultFile.createNewFile();
			PrintStream resultStream = new PrintStream(resultFile);
			
			resultStream.printf("inc: %f\n",sim.getYearlyIncidancePercent());
			
			resultStream.print((new GregorianCalendar()).getTime());
												
			resultStream.close();
		} catch(IOException ex) {
			System.out.println("Could not write to file out.results!"); 
			System.exit(0);
		}
		
		System.out.printf("inc: %f",sim.getYearlyIncidancePercent());	
	}


	private static void printHelp() {	
		System.out.println("\n\nGeneral Parameters:");
		System.out.println("-------------------");
		Settings.printSettingsDescription(Parameters.class, System.out);
		Settings.printSettingsDescription(Random.class, System.out);
		System.out.println("\nSegmented Phenotype: (SEGMENT)");
		Settings.printSettingsDescription(ImmuneSystemDiscrete.class, System.out);		
		System.out.println("\n\nExamples:");
		System.out.println("--------");
		System.out.println("java -Xms2G -Xmx2G -jar simtree.jar phenotypeSpace=SEGMENTED beta=0.65 nu=0.20"); 	
	}

	


}

