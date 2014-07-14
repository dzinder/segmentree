/* Holds random number genator necessities */
/* Trying to encapsulate this, so the RNG particulars can be changed if necessary */ 
/* Completely static class, allows no instances to be instantiated */

//import cern.jet.random.*;
import cern.jet.random.Exponential;
import cern.jet.random.Gamma;
import cern.jet.random.Poisson;
import cern.jet.random.Uniform;
import cern.jet.random.engine.*;


public class Random {
				
	// methods
	static RandomEngine rng = null;
	@Setting static Integer seed = null;
	
	static Uniform myUniform = null;
	static Exponential myExp = null;
	static Gamma myGamma = null;
	static Poisson myPoisson = null;
		
	public static void init() {
		if (seed!=null)
			rng = new MersenneTwister(seed);
		else 
			rng = new MersenneTwister();
		
		myUniform = new Uniform(rng);
		myExp = new Exponential(0.1,rng);
		myGamma = new Gamma(0.1, 0.1, rng);
		myPoisson = new Poisson(0.1, rng);
		
	}

	public static int nextInt(int from, int to) { // closed interval [form,to] (including form and to)
		return myUniform.nextIntFromTo(from, to);
	}	
	
	public static byte nextByte(byte from, byte to) {
		return (byte) myUniform.nextIntFromTo(from, to);
	}	
	
	
	public static double nextDouble() {
		return myUniform.nextDouble();		
	}
	
	public static double nextDouble(double from, double to) {
		return myUniform.nextDoubleFromTo(from, to);		
	}	

	// tuned with mean
	public static double nextExponential(double lambda) {
		return myExp.nextDouble(1.0/lambda);
	}
	
	// tuned with alpha and beta, matching Mathematica's notation
	public static double nextGamma(double alpha, double beta) {
		return myGamma.nextDouble(alpha, 1/beta);
	}	
	
	public static int nextPoisson(double lambda) {
		return myPoisson.nextInt(lambda);
	}
	
	
	// return true with probability p
	public static boolean nextBoolean(double p) {
		boolean x = false;
		if (myUniform.nextDouble() < p) {
			x = true;
		}
		return x;
	}	
	
	
	

}