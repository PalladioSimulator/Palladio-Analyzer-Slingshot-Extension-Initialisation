package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.List;

import org.palladiosimulator.spd.ScalingPolicy;

/**
 *
 * Record of the values of an {@code SPDAdjustorState}.
 *
 * This is a distinct data type, as this is itself not a state, but only carries
 * the values to be set in some other state.
 *
 * @author Sophie Stieß
 *
 */
public record SPDAdjustorStateValues(ScalingPolicy scalingPolicy, double latestAdjustmentAtSimulationTime, int numberScales,
		double coolDownEnd, int numberOfScalesInCooldown, List<ScalingPolicy> enactedPolicies, List<Double> enactmentTimeOfPolicies) {
	
	public String scalingPolicyId() {
		return scalingPolicy().getId();
	}
}