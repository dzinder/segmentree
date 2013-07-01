import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.GregorianCalendar;


public class HostsForImmunitySamples {
	
	PrintStream immuneStream = null;
	File infectedFile = null;
	long numHostsSampled = 0;
	Host lastHostSampled = null; 
	
	HostsForImmunitySamples() {
		reset();
	}
	
	void close() {
		immuneStream.close();
	}
	
	public void add(Host h) {	
		// For print purposes only ...
		immuneStream.printf("%f,\"%s\",%f,%d%s\n",Parameters.getDate(),h, Parameters.getDate()-h.getBirth(),h.getInfections().size(),h.getImmuneSystem().print());	
	}

	public void reset() {
		numHostsSampled=0;
		try {
			infectedFile = new File("out.immunity");
			infectedFile.delete();
			infectedFile.createNewFile();
			immuneStream = new PrintStream(infectedFile);	
			immuneStream.printf("year,hostID,hostAge,numInfections,numPreviousInfections,segments....\n",5);
		} catch(IOException ex) {
			System.out.println("Could not write to file out.immunity!"); 
			System.exit(0);
		}		
	}

	

}