import java.io.File;
import java.io.IOException;
import java.io.PrintStream;


public class InfectedHostSamples {
	
	PrintStream infectedStream = null;
	File infectedFile = null;
	long numHostsSampled = 0;
	Host lastHostSampled = null; 
	
	InfectedHostSamples() {
		reset();
	}
	
	void close() {
		infectedStream.close();
	}
	
	public void add(Host h_, Virus v_, Segment s) {
		
		// For print purposes only ...
		Host h=h_; 
		Virus v=v_;
		
		infectedStream.printf("%f,\"%s\",\"%s\",%s,%f,%d\n",Parameters.getDate(),h,v,s.getSegmentName(), Parameters.getDate()-h.getBirthInYears(),h.getInfections().size());
		if (h!=lastHostSampled) {
			lastHostSampled=h;
			numHostsSampled+=1;
			
		}
			
		lastHostSampled=h;

	}

	public void reset() {
		numHostsSampled=0;
		try {
			infectedFile = new File("out.infected");
			infectedFile.delete();
			infectedFile.createNewFile();
			infectedStream = new PrintStream(infectedFile);	
			infectedStream.printf("year,hostID,genomeID,segmentID,hostAge,numInfections\n",5);
		} catch(IOException ex) {
			System.out.println("Could not write to file out.infected!"); 
			System.exit(0);
		}		
	}

	

}