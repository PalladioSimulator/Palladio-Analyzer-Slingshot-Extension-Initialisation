package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialisation;

import java.util.List;

import org.palladiosimulator.spd.ScalingPolicy;

/**
 * Class to deserialize information from the given json to.
 *
 * @author Sophie Stieß
 *
 */
public class OtherInitThings {
	
	private final double sensibility; 
	private final List<ScalingPolicy> incomingPolicies;

	/**
	 * 
	 * @param sensibility for configuring the snapshot mechanism.
	 * @param incomingPolicies policy to be injected at the beginning of the simulation.
	 */
	public OtherInitThings(final double sensibility, final List<ScalingPolicy> incomingPolicies) {
		super();
		this.sensibility = sensibility;
		this.incomingPolicies = incomingPolicies;
	}

	/**
	 * 
	 * @return sensibility for configuring the snapshot mechanism.
	 */
	public double getSensibility() {
		return sensibility;
	}

	/**
	 * 
	 * @return policy to be injected at the beginning of the simulation.
	 */
	public List<ScalingPolicy> getIncomingPolicies() {
		return incomingPolicies;
	}
}
