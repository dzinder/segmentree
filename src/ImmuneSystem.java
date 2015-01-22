import java.util.List;


public interface ImmuneSystem {

	double riskOfInfection(Virus v);

	void reset();

	void add(Virus v);

	String print();
	
	void vaccinate(List<Virus> v);

	double getRiskOfTransmission();

}
