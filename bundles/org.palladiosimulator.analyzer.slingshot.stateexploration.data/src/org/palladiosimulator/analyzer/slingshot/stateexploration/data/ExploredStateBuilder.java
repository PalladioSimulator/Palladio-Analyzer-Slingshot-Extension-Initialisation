package org.palladiosimulator.analyzer.slingshot.stateexploration.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.converter.MeasurementConverter;
import org.palladiosimulator.analyzer.slingshot.converter.data.MeasurementSet;
import org.palladiosimulator.analyzer.slingshot.converter.data.Utility;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.data.InitState;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.data.ResultState;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.ExperimentData.ExperimentSetting;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.common.base.Preconditions;

/**
 * Builder Class for {@link ExploredState}.
 * 
 * Cannot be used to build the root node.
 *
 * Some values from the builder are required for initialising a simulation run,
 * while others are required for post processing. Those are wrapped into record
 * types, as i do not want arbitrary getters in this class.
 *
 * @author Sophie Stieß
 *
 */
public class ExploredStateBuilder {

	/* known at start */
	private final double startTime;
	private final String parentId;

	private final String id = UUID.randomUUID().toString();

	/* must be filled at the end of a simulation run */
	private final Set<ReasonToLeave> reasonsToLeave = new HashSet<>();

	/* must be set at the end of a simulation run */
	private double duration = -1;
	private Snapshot snapshot = null;

	/* must be set after configuration of the simulation run */
	private ExperimentSetting experimentSetting = null;
	
	private final PCMResourceSetPartition partition;

	public ExploredStateBuilder(final String parentId, final double startTime, final PCMResourceSetPartition partition) {
		this.startTime = startTime;
		this.parentId = parentId;
		this.partition = partition;
	}

	public String getParentId() {
		return parentId;
	}

	public String getId() {
		return id;
	}

	public Set<ReasonToLeave> getReasonsToLeave() {
		return reasonsToLeave;
	}

	public double getDuration() {
		return duration;
	}

	public Snapshot getSnapshot() {
		return snapshot;
	}

	public ExperimentSetting getExperimentSetting() {
		return experimentSetting;
	}

	public PCMResourceSetPartition getPartition() {
		return partition;
	}

	public double getStartTime() {
		return startTime;
	}

	public void setExperimentSetting(final ExperimentSetting experimentSetting) {
		this.experimentSetting = experimentSetting;
	}

	public void setSnapshot(final Snapshot snapshot) {
		this.snapshot = snapshot;
	}

	public void setDuration(final double duration) {
		this.duration = duration;
	}

	public void addReasonToLeave(final ReasonToLeave reasonToLeave) {
		this.reasonsToLeave.add(reasonToLeave);
	}
	
	/**
	 * Build a new {@link ExploredState} based on this builder.
	 * 
	 * Requires all attributes to be set. If some attributes are missing or were not
	 * updated as intended, building the state fails.
	 * 
	 * @return a new {@link ExploredState}
	 * @throws IllegalStateException if this operation is called while the builder
	 *                               is still incomplete, or if one attempts to use this builder to create multiple states.
	 */
	public ResultState buildResultState() {
		Preconditions.checkState(!reasonsToLeave.isEmpty(), "Cannot build state, reasons to leave were not yet added.");
		Preconditions.checkState(duration >= 0, "Cannot build state, duration was not yet set.");
		Preconditions.checkState(snapshot != null, "Cannot build state, because snapshot was not yet set.");
		Preconditions.checkState(experimentSetting != null,
				"Cannot build state, because experiment settings were not yet set.");

		
		final List<ScalingPolicy> policies = snapshot.getModelAdjustmentRequestedEvent().stream().map(e -> e.getScalingPolicy()).toList();
		final List<MeasurementSet> measurements = new MeasurementConverter(0.0, duration).visitExperiementSetting(experimentSetting);

		final Utility utility = Utility.createUtility(startTime, startTime + duration, measurements, PCMResourcePartitionHelper.getSLORepository(partition).getServicelevelobjectives());
			
		return new ResultState(startTime + duration, measurements, duration, reasonsToLeave, parentId, policies, utility);
	}
	
	
	
	
	public InitState buildInitState() {
//		PreconditionsCheck...
		return new InitState(startTime + duration, snapshot, id);
	}
}
