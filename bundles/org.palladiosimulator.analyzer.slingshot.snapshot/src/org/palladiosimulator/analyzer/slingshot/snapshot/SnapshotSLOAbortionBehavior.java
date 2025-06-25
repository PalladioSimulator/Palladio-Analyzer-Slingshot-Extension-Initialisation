package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.measure.Measure;
import javax.measure.quantity.Quantity;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.StateBuilder;
import org.palladiosimulator.analyzer.slingshot.monitor.data.events.MeasurementUpdated;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.ConfigurableSnapshotExtension;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.servicelevelobjective.SoftThreshold;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 *
 * Abort simulation runs, if any measurement value violates a SLO hard threshold. 
 *
 * This class does <strong>not</strong> use the raw measurements provided by
 * {@code MeasurementMade} events. Instead it uses the aggregated values
 * provided by {@code MeasurementUpdated} events.
 * 
 * Beware: The aggregated values provided by {@code MeasurementUpdated} events
 * are aggregated according to the {@code ProcessingType} elements defined in
 * the {@code MonitorRepository}. This class does not aggregate on its own.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = MeasurementUpdated.class, then = SnapshotInitiated.class)
public class SnapshotSLOAbortionBehavior extends ConfigurableSnapshotExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotSLOAbortionBehavior.class);

	private final StateBuilder state;
	private final ServiceLevelObjectiveRepository sloRepo;

	private final boolean activated;
	
	private final Map<MeasurementSpecification, Predicate<Double>> map = new HashMap<>();


	/**
	 * Create new behaviour for aborting simulation runs based on the SLOs.
	 * 
	 * @param stateBuilder for propagating the reason to leave
	 * @param blackboard
	 * @param config
	 */
	@Inject
	public SnapshotSLOAbortionBehavior(final @Nullable StateBuilder stateBuilder,
			final @Nullable MDSDBlackboard blackboard, final @Nullable SnapshotConfiguration config) {
		
		super(config);
		
		if (PCMResourcePartitionHelper.hasSLORepository((PCMResourceSetPartition)
				blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID))) {
			this.sloRepo = PCMResourcePartitionHelper.getSLORepository((PCMResourceSetPartition)
					blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID));
		} else {
			this.sloRepo = null;
		}
		
		this.activated = stateBuilder != null && sloRepo != null && config != null
				&& !sloRepo.getServicelevelobjectives().isEmpty();

		this.state = stateBuilder;
		
		if (activated) {
			this.parseSLOS();
		}
	}

	/**
	 * 
	 * @param sloRepo
	 */
	private void parseSLOS() {
		for (final ServiceLevelObjective slo : sloRepo.getServicelevelobjectives()) {
			final MeasurementSpecification spec = slo.getMeasurementSpecification();
			Predicate<Double> check = null;
			
			if (slo.getLowerThreshold() instanceof final SoftThreshold th) {
				final Measure<Number, ?> hardlimit =  (Measure<Number, ?>) (th.getThresholdLimit());
				check = value -> value < hardlimit.getValue().doubleValue();
			}
			if (slo.getUpperThreshold() instanceof final SoftThreshold th) {
				final Measure<Number, ?> hardlimit =  (Measure<Number, ?>) (th.getThresholdLimit());
				final Predicate<Double> uppercheck = value -> value > hardlimit.getValue().doubleValue();
				
				check = check == null ? uppercheck : check.or(uppercheck);
			}
			
			map.put(spec, check);
		}	
	}
	
	@Override
	public boolean getActivated() {
		return this.activated && !map.isEmpty();
	}

	@Subscribe
	public Result<SnapshotInitiated> onMeasurementUpdated(final MeasurementUpdated event) {

		final MetricDescription base = event.getEntity().getProcessingType().getMeasurementSpecification()
				.getMetricDescription();

		final Measure<Double, Quantity> value = event.getEntity().getMeasuringValue().getMeasureForMetric(base);

		final double calculationValue = value.doubleValue(value.getUnit());

		final MeasurementSpecification spec = event.getEntity().getProcessingType().getMeasurementSpecification();

		
		if(map.containsKey(spec)) {
			if (map.get(spec).test(calculationValue)) {
				state.addReasonToLeave(ReasonToLeave.aborted);
				return Result.of(new SnapshotInitiated(0.0));
			}
		}
		
		return Result.empty();
	}
}
