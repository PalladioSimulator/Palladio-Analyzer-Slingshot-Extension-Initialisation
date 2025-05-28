package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.data;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.converter.data.SLO;
import org.palladiosimulator.analyzer.slingshot.converter.data.StateGraphNode;
import org.palladiosimulator.analyzer.slingshot.converter.data.Utility;
import org.palladiosimulator.analyzer.slingshot.stateexploration.data.ReasonToLeave;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.spd.ScalingPolicy;

/**
 * For Serialization only 
 *
 * @author Sophie Stieß
 *
 */
public class ResultState {


	/* Additional fields*/
	private final String parentId;
	private final String id;
	
	/* known at start */
	private final double startTime;

	/* known at the end */
	private final double duration;
	private final Set<ReasonToLeave> reasonsToLeave;

	
	private final List<SLO> slos;
	private final Utility utility;
	private final List<String> outgoingPolicyIds;
	

	private final List<MeasurementSet> measurementSets;

	/**
	 * 
	 * @param pointInTime
	 * @param measurementSets
	 * @param snapshot
	 * @param duration
	 * @param reasonsToLeave
	 * @param parentId
	 */
	public ResultState(final PCMResourceSetPartition initModels, final double pointInTime, final List<MeasurementSet> measurementSets, final double duration, final Set<ReasonToLeave> reasonsToLeave, final String parentId, final List<ScalingPolicy> outgoingPolicies, final Utility utility) {
		this.parentId = parentId;
		this.startTime = pointInTime;
		this.reasonsToLeave = reasonsToLeave;
		
		this.measurementSets = measurementSets;
		this.duration = duration;
		
		this.id = UUID.randomUUID().toString();
		this.slos = PCMResourcePartitionHelper.getSLORepository(initModels).getServicelevelobjectives().stream().map(x -> StateGraphNode.visitServiceLevelObjective(x)).toList();
		
		this.utility = utility;
		this.outgoingPolicyIds = outgoingPolicies.stream().map(ScalingPolicy::getId).toList();
	}
}
