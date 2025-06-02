package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialisation;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.converter.data.Utility;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.ReasonToLeave;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 * Class for serializing the state.
 * 
 * @author Sophie Stieß
 *
 */
public class ResultState {

	private final String parentId;
	private final String id;
	
	private final double startTime;
	private final double duration;
	private final Set<ReasonToLeave> reasonsToLeave;

	
	private final Utility utility;
	private final List<String> outgoingPolicyIds;

	private final List<MeasurementSet> measurementSets;

	/**
	 * 
	 * @param pointInTime start time of this state
	 * @param measurementSets measurements taken during this state.
	 * @param duration duration of this state.
	 * @param reasonsToLeave reason for ending a simulation run and creating this state.
	 * @param parentId id of this state's parentstate.
	 * @param outgoingPolicies policies that happend at the end of this simulation run. empty, if none happend. 
	 * @param utility utility of this state.
	 */
	public ResultState(final double pointInTime, final List<MeasurementSet> measurementSets, final double duration, final Set<ReasonToLeave> reasonsToLeave, final String parentId, final List<ScalingPolicy> outgoingPolicies, final Utility utility) {
		this.parentId = parentId;
		this.startTime = pointInTime;
		this.reasonsToLeave = reasonsToLeave;
		
		this.measurementSets = measurementSets;
		this.duration = duration;
		
		this.id = UUID.randomUUID().toString();
		
		this.utility = utility;
		this.outgoingPolicyIds = outgoingPolicies.stream().map(ScalingPolicy::getId).toList();
	}
}
