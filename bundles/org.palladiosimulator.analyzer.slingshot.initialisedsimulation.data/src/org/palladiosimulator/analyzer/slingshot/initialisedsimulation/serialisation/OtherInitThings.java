package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialisation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotBehaviourConfigurationParameters;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 * Class to deserialize information from the given json to.
 *
 * @author Sophie Stieß
 *
 */
public class OtherInitThings {
	
	private final List<ScalingPolicy> incomingPolicies; 
	private final Map<String, SnapshotBehaviourConfigurationParameters> parameters;


	/**
	 * 
	 * @param sensibility for configuring the snapshot mechanism.
	 * @param incomingPolicies policy to be injected at the beginning of the simulation.
	 */
	public OtherInitThings(final List<ScalingPolicy> incomingPolicies, final Map<String, SnapshotBehaviourConfigurationParameters> parameters) {
		super();
		this.incomingPolicies = incomingPolicies;
		this.parameters = parameters;
	}
	
	/**
	 * 
	 * @return
	 */
	public Map<String, SnapshotBehaviourConfigurationParameters> getConfigurationParameters() {
		return this.parameters != null ? this.parameters : new HashMap<>();
	}

	/**
	 * 
	 * @return policy to be injected at the beginning of the simulation.
	 */
	public List<ScalingPolicy> getIncomingPolicies() {
		return this.incomingPolicies != null ? this.incomingPolicies : List.of();
	}
}
