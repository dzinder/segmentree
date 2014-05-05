import java.util.ArrayList;
import java.util.List;

public class Host {

	// fields
	private List<Virus> infectingViruses = new ArrayList<Virus>();												
	private ImmuneSystem immuneSystem = new ImmuneSystemDiscrete();
	private int birth;	// measured in years relative to burnin	


	// CONSTRUCTORS & INITIALIZERS

	// generate initial naive host
	public Host(boolean bornOld) {
		if (bornOld) {
			float lifespan = (float) (1 / (365.0 * Parameters.birthRate));
			float age = (float) Random.nextExponential(lifespan);
			birth = (int) (Parameters.day - age*365);
		}
		else {
			birth = Parameters.day;
		}
	}

	// recycle host 
	public void reset() {
		birth = Parameters.day;
		infectingViruses.clear();
		immuneSystem.reset();
	}

	// METHODS
	public float getBirthInYears() {
		return ((float)birth-Parameters.burnin)/(float)365.0;
	}

	public float getAgeInDays() {
		return Parameters.day-birth;
	}

	public boolean isInfected() {		
		return infectingViruses.size()>0;
	}

	public List<Virus> getInfections() {
		return infectingViruses;
	}

	public void infect(Virus infectingVirus_) {
		float hostAge = Parameters.getDate() - (((float)birth-Parameters.burnin)/(float)365.0);
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
		// TODO: What to do with mutation under coinfection? assume the same viral load? double the viral load?
		int infectingVirusToMutate = Random.nextInt(0, infectingViruses.size()-1);
		infectingViruses.set(infectingVirusToMutate, infectingViruses.get(infectingVirusToMutate).mutate());

	}

	public ImmuneSystem getImmuneSystem() {
		return immuneSystem;
	}

	public void immunize(List<Virus> vaccineComposition) {
		immuneSystem.vaccinate(vaccineComposition);		
	}



}