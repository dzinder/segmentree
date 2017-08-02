/* Interface for Phenotype objects */

public interface Phenotype {
		
	// return mutated Phenotype object
	// returned Phenotype is a newly constructed copy of original
	Phenotype mutate();
	
	// return mutated Phenotype object
	// returned Phenotype is a newly constructed copy of original
	Phenotype reassort(Phenotype g);
	
	// this is used in output, should be a short string form
	// 2D: 0.5,0.6
	// sequence: "ATGCGCC"
	String toString(String seperator);
	
	String toString();
	
}