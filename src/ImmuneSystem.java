
public interface ImmuneSystem {

	double riskOfInfection(Virus v);

	void reset();

	void add(Virus v);

	String print();

}
