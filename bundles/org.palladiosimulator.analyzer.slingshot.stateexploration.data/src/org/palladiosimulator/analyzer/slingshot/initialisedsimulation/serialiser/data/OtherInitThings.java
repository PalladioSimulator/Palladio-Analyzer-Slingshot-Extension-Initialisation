package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.data;

import java.util.List;

import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * @author Sophie Stieß
 *
 */
public class OtherInitThings {
	
	private final double sensibility; 
	private final List<ScalingPolicy> incomingPolicies;

	public OtherInitThings(final double sensibility, final List<ScalingPolicy> incomingPolicies) {
		super();
		this.sensibility = sensibility;
		this.incomingPolicies = incomingPolicies;
	}

	public double getSensibility() {
		return sensibility;
	}

	public List<ScalingPolicy> getIncomingPolicies() {
		return incomingPolicies;
	}
}
