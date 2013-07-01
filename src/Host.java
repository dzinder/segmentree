import java.util.ArrayList;
import java.util.List;

public class Host {

	// fields
	private List<Virus> infectingViruses = new ArrayList<Virus>();												
	private ImmuneSystem immuneSystem = new ImmuneSystemDiscrete();
	private float birth;	// measured in years relative to burnin	


	// CONSTRUCTORS & INITIALIZERS

	// generate initial naive host
	public Host() {		
		float lifespan = (float) (1 / (365.0 * Parameters.birthRate));
		float age = (float) Random.nextExponential(lifespan);
		birth = (float) (Parameters.getDate() - age);	
	}

	// recycle host 
	public void reset() {
		birth = (float) Parameters.getDate();
		infectingViruses.clear();
		immuneSystem.reset();
	}

	// METHODS
	public float getBirth() {
		return birth;
	}

	public boolean isInfected() {		
		return infectingViruses.size()>0;
	}

	public List<Virus> getInfections() {
		return infectingViruses;
	}

	public void infect(Virus infectingVirus_) {
		float hostAge = Parameters.getDate() - birth;
		infectingViruses.add(new Virus(infectingVirus_,hostAge));			
	}
	
	public void addToImmuneHistory(Virus immunizingVirus_) {
		immuneSystem.add(immunizingVirus_);			
	}

	public void clearInfections() {
		for (Virus v : infectingViruses) {
			immuneSystem.add(v);
		}
		infectingViruses.clear();		
	}

	public double riskOfInfection( Virus v) {
		return immuneSystem.riskOfInfection(v);
	}

	public Virus getRandomInfection() {
		if (!isSuperinfected()) {
			return infectingViruses.get(Random.nextInt(0, infectingViruses.size()-1));
		} 
		else {
			return infectingViruses.get(Random.nextInt(0, infectingViruses.size()-1)).reassort(infectingViruses);
		}
	}

	public boolean isSuperinfected() {		
		return infectingViruses.size()>0;
	}

	public void mutate() {
		int infectingVirusToMutate = Random.nextInt(0, infectingViruses.size()-1);
		infectingViruses.set(infectingVirusToMutate, infectingViruses.get(infectingVirusToMutate).mutate());
		
	}

	public ImmuneSystem getImmuneSystem() {
		return immuneSystem;
	}

	
	
}