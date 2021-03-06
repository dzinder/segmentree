# Segmented Viral Evolution and Epidemiology Simulation
### github.com/dzinder/segmentree

-----------------------------------------------------------------------------------------------------------
### For details and citation please refer to:
* The roles of competition and mutation in shaping antigenic and genetic diversity in influenza D Zinder, T Bedford, S Gupta, M Pascual 
 PLoS Pathog 9 (1), e1003104

* Role of competition in the strain structure of rotavirus under invasion and reassortment D Zinder, M Riolo, R Woods, M Pascual (in prep)

**Requires:**
Java JRE 1.6 and newer

**For help type:**
java -jar segmentree.jar --help

**Example usage:**
java -Xms2G -Xmx2G -jar segmentree.jar viralFitnessType=EQUAL_FITNESS beta=0.65 nu=0.20

where -Xmx2G specifies maximum allowable memory usage limit, viralFitnessType, beta and nu are simulation parameters 

### General Parameters:
-----------------------
* seed - simulation random seed

### Simulation Parameters:
-----------------------
* burnin - Burn In time in days. Initial run time without recording output.
* endDay - Simulation end time in days.
* repeatSim - Repeat simulation following a stochastic extinction until endDay is reached.
* keepAliveDuringBurnin - Prevent stochastic extinction during burn-in period by maintaining at least one infected individual
* keepAlive - Prevent stochastic extinction by maintaining at least one infected individual

### Sampling Parameters:
-----------------------
* printStepTimeseries - print to out.timeseries every X days.
* printStepImmunity - print to out.immunity every X days
* tipSamplingRate - in proportion samples per day
* tipSamplingProportional - true to set sampling proportional to prevalence (vs. population size)
* treeProportion - proportion of tips to use in tree reconstruction
* intervalForMarkTips - interval used for sampling subset of tips to be marked
* diversitySamplingCount - how many tips to sample when estimating diversity
* yearsToTrunk - subtract this many years off the end of the tree when designating trunk
* sampleWholeGenomes - Sample whole genomes for tips rather than random samples.... 
* infectedHostSamplingRate - Infected host sampling rate for out.infected
* immunityHostSamplingRate - Host sampling rate for out.immunity

### Demographic Parameters:
-------------------------
* N - Number of hosts in population
* birthRate - in births per individual per day, i.e. 1/(30*365)
* deathRate - in deaths per individual per day, i.e. 1/(30*365)
* swapDemography - whether to keep overall population size constant

### Disruption Parameters:
------------------------
* disruptionTime1 - distruptive interruption 1 time
* disruptionType1 - disruption 1 type NONE/MASS_EXTINCTION/CHANGE_MUTATION/CHANGE_INTRO/CHANGE_REASSORTMENT
* disruptionParameter1 - disruption 1 parameter (fraction extinction for mass extinciton, new mutation/introduction/reassortment_rate for change mutation/intro/reassortment)
* disruptionTime2 - distruptive interruption 2 time
* disruptionType2 - disruption 2 type NONE/MASS_EXTINCTION/CHANGE_MUTATION/CHANGE_INTRO/CHANGE_REASSORTMENT
* disruptionParameter2 - disruption 2 parameter (fraction extinction for mass extinciton, new mutation/introduction/reassortment_rate for change mutation/intro/reassortment)
* disruptionTime3 - distruptive interruption 3 time
* disruptionType3 - disruption 3 type NONE/MASS_EXTINCTION/CHANGE_MUTATION/CHANGE_INTRO/CHANGE_REASSORTMENT
* disruptionParameter3 - disruption 3 parameter (fraction extinction for mass extinciton, new mutation/introduction/reassortment_rate for change mutation/intro/reassortment)
* disruptionTime4 - distruptive interruption 4 time
* disruptionType4 - disruption 4 type NONE/MASS_EXTINCTION/CHANGE_MUTATION/CHANGE_INTRO/CHANGE_REASSORTMENT
* disruptionParameter4 - disruption 4 parameter (fraction extinction for mass extinciton, new mutation/introduction/reassortment_rate for change mutation/intro/reassortment)

### Epidemiological Parameters:
-----------------------------
* initialI - initial number of infected individuals
* initialPrR - proportion recovered to intial virus/es (multiple recoveries for value greater than 1)
* beta - in contacts per individual per day
* nu - in recoveries per individual per day
* omega - in waning immunity per individual per day (Double.POSITIVE_INFINTY for no waning immunity)

### Mutation and Reassortment Parameters:
---------------------------------------
* intro - introduction rate - in segment introductions per day
* mu - mutation rate - in mutations per infected host per day
* rho - reassortment probability - the probability of a segment to be randomly chosen from all possible\n segments during transmission from a superinfection
* n_bottleNeck - infection bottle neck size - at most number of segment combinations to be transmitted from a superinfected host

### Reservoir Parameters:
-----------------------
* proportionContactWithReservoir - contact with initial strain reservoir as proporiton of beta
* reintro - introduction rate - in segment introductions per day

### Virus Parameters:
-------------------
* virusFitnessType - virus fitness EQUAL_FITNESS/SEGMENT_FITNESS/INC_SINCE_CREATION (1-p1*exp(-t*p2))
* viralFitnessParam1 - viral fitness parameter 1
 viralFitnessParam2 - viral fitness parameter 2

### Segment Parameters:
----------------------
* nSegments - number of viral segments
* nImmunogenicSegments - number of immunogenic segments (only these first n segments will generate effective immunity)
* nInitialSegmentAllels - number of inital segment allels
* nInitialStrains - number of inital random viral segement combinations
* segmentFitnessType - segment fitness EQUAL_FITNESS/RANDOM_EXPONENTIAL/RANDOM_TRUNCATED_NORMAL
* segmentFitnessParam1 - segment fitness parameter 1
* segmentFitnessParam2 - segment fitness parameter 2

### Immunity Parameters:
----------------------
* sigma_spec - the part of immunity which is reduction in suscptibility based on the number of segments seen before
* infection risk=gen_risk x specific_risk = exp(-sigma_het x #previous_infections) x exp(-sigma_spec x #previous_segments / nSegments)
* sigma_gen - the part of immunity which is reduction in suscptibility based on the number of previous infections
* infection risk=gen_risk x specific_risk = exp(-sigma_het x #previous_infections) x exp(-sigma_ho x #previous_segments / nSegments)
* xi_reduced_infectivity - reduction in infectivity following previous infections
* transmission risk=infectivity_at_first_infection x exp(-xi_reduced_infection x #previous_infections) x fitness
* infectivity_at_first_infection - infectivity at first infectiontransmission risk=infectivity_at_first_infection x exp(-xi_reduced_infection x #previous_infections) x fitness


## Output Files:
-------------
* out.params - parameters used
* out.timeseries - simulation status
* out.tips - sampled viral segments
* out.branches - tree information
* out.immunity - sampled immune histories from random hosts
* out.infected - sampled infected hosts
* out.mk - selection status, in development
* out.results - prevalence summary
