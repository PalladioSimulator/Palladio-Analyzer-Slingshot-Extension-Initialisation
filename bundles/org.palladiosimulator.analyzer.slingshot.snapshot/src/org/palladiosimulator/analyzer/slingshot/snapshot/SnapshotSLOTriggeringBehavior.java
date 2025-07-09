package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Quantity;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.common.utils.SPDHelper;
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
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjective;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.servicelevelobjective.SoftThreshold;
import org.palladiosimulator.servicelevelobjective.Threshold;
import org.palladiosimulator.spd.SPD;

import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 *
 * This behaviour extension initiates snapshots based on a measurement's closeness to the defined SLOs.
 * 
 * The closeness is configurable. The corresponding parameter is called "sensitivity", the value must be a double in [0.0, 1.0]. 
 * For more details, c.f. {@link ValueRange}. <br>
 * <br>
 * Activation of this behaviour extension can be delayed. The corresponding parameter is called "activationDelay", the value must be a double.
 * If the value is less than or equal to 0.0, this behaviour extension is always active.<br>
 * <br>
 * This class does <strong>not</strong> use the raw measurements provided by
 * {@code MeasurementMade} events. Instead it uses the aggregated values
 * provided by {@code MeasurementUpdated} events.
 * <br>
 * Beware: The aggregated values provided by {@code MeasurementUpdated} events
 * are aggregated according to the {@code ProcessingType} elements defined in
 * the {@code MonitorRepository}. This class does not aggregate on its own.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = MeasurementUpdated.class, then = SnapshotInitiated.class)
public class SnapshotSLOTriggeringBehavior extends ConfigurableSnapshotExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotSLOTriggeringBehavior.class);
	
	private final static String SENSITIVITY = "sensitivity";

	private final StateBuilder state;
	private final Configuration semanticSpd;
	private final SPD spd;

	private final boolean activated;
	
	private final double delay;
	private final static String DELAY = "activationDelay";

	/* Calculate mapping to custom class to save time later on (probably) */
	private final Map<MeasurementSpecification, Set<ValueRange>> mp2range;

	@Inject
	public SnapshotSLOTriggeringBehavior(final @Nullable StateBuilder state,
			final @Nullable MDSDBlackboard blackboard,
			final @Nullable Configuration semanticSpd,
			final @Nullable SPD spd,
			final @Nullable SnapshotConfiguration config) {
		
		super(config);
		
		if (state != null && semanticSpd != null && spd != null && blackboard != null) {
			this.activated = true;
			this.state = state;
			this.semanticSpd = semanticSpd; 
			this.spd = spd;
			this.mp2range = this.initMeasuringPoint2RangeMap(blackboard);
			this.delay = this.configParameters.hasParameter(DELAY, Double.class) ? this.configParameters.getParameter(DELAY) : 0.0; 
			
		} else {
			this.activated = false;
			this.state = null;
			this.semanticSpd = null;
			this.spd = null;
			this.mp2range = null;
			this.delay = 0.0;
			
			LOGGER.warn(String.format("Extension %s is deactivated because a required parameter is null.", this.getClass().getSimpleName()));
		}
	}
	
	@Override
	public boolean getActivated() {
		return this.activated;
	}

	/**
	 * 
	 * Create a map with pairs of {@link MeasurementSpecification} and {@link ValueRange}.
	 * 
	 * @param blackboard blackboard to provide access to the SLOs.
	 * @return A map of ({@link MeasurementSpecification}, {@link ValueRange}) pairs, or an empty map, if the SLO repository is missing or empty.
	 */
	private Map<MeasurementSpecification, Set<ValueRange>> initMeasuringPoint2RangeMap(final MDSDBlackboard blackboard) {
		final Map<MeasurementSpecification, Set<ValueRange>> map = new HashMap<>();
		
		if (!PCMResourcePartitionHelper.hasSLORepository((PCMResourceSetPartition)
				blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID))) {
			return map;
		}
		
		final ServiceLevelObjectiveRepository sloRepo = PCMResourcePartitionHelper.getSLORepository((PCMResourceSetPartition)
				blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID));
		final double sensitivity = configParameters.hasParameter(SENSITIVITY, Double.class) ? configParameters.getParameter(SENSITIVITY) : 0.0;
		
		if (sensitivity < 0 || sensitivity > 1) {
			throw new IllegalArgumentException("Parameter \"sensitivity\" must be in [0.0, 1.0] but is " + sensitivity + ".");
		}
		
		for (final ServiceLevelObjective slo : sloRepo.getServicelevelobjectives()) {
			if (slo.getLowerThreshold() == null && slo.getUpperThreshold() == null) {
				LOGGER.debug(
						String.format("No thresholds for %s [%s], will be ignored", slo.getName(), slo.getId()));
				continue;
			}
			if (slo.getLowerThreshold() != null && slo.getUpperThreshold() == null) {
				LOGGER.debug(String.format("No upper threshold for %s [%s], will be ignored", slo.getName(),
						slo.getId()));
				continue;
			}

			final MeasurementSpecification mp = slo.getMeasurementSpecification();
			if (!map.containsKey(mp)) {
				map.put(mp, new HashSet<>());
			}
			if (slo.getLowerThreshold() == null) {
				map.get(mp).add(new SingleEndedRange(
						getLimit(slo.getUpperThreshold()), sensitivity));
			} else {
				map.get(mp).add(new DoubleEndedRange(
						getLimit(slo.getUpperThreshold()),
						getLimit(slo.getLowerThreshold()), sensitivity));
			}
		}
		return map;
	}

	/**
	 * Get the limit of the given threshold. In case the given threshold is a
	 * {@link SoftThreshold}, this operation gets the soft limit instead of the hard
	 * one.
	 * 
	 * @param threshold
	 * @return Hard- or soft limit of the given threshold.
	 */
	private Measure<Object, Quantity> getLimit(final Threshold threshold) {
		if (SoftThreshold.class.isInstance(threshold)) {
			return SoftThreshold.class.cast(threshold).getSoftLimit();
		} else {
			return (Measure<Object, Quantity>) threshold.getThresholdLimit();
		}
	}
	
	/**
	 * Check whether the measurement value insed the given event is too close to an
	 * SLO and initiates a snapshot, if the former is the case.
	 * 
	 * @param event
	 * @return {@link SnapshotInitiated} if the measurement value inside the given
	 *         event is too close to an SLO, an empty result.
	 */
	@Subscribe
	public Result<SnapshotInitiated> onMeasurementUpdated(final MeasurementUpdated event) {
		
		if (event.time() < this.delay) {
			return Result.empty();
		}

		if (!mp2range.containsKey(event.getEntity().getProcessingType().getMeasurementSpecification())) {
			return Result.empty();
		}

		final MetricDescription base = event.getEntity().getProcessingType().getMeasurementSpecification()
				.getMetricDescription();
		final Measure<Double, Quantity> value = event.getEntity().getMeasuringValue().getMeasureForMetric(base);
		final double calculationValue = value.doubleValue(value.getUnit());
		final MeasurementSpecification spec = event.getEntity().getProcessingType().getMeasurementSpecification();

		// precalculate, because digging into the semantic model is probably expensive
		final boolean isMinArch = SPDHelper.isMinArchitecture(semanticSpd, spd);
		final boolean isMaxArch = SPDHelper.isMaxArchitecture(semanticSpd, spd);
		
		for (final ValueRange range : mp2range.get(spec)) {
			if (range.isViolatedBy(calculationValue)) {
				if (range.isLowerViolatedBy(calculationValue) && isMinArch
						|| range.isUpperViolatedBy(calculationValue) && isMaxArch) {
					continue;
				}
				state.addReasonToLeave(ReasonToLeave.closenessToSLO);
				LOGGER.debug(String.format(
						"Triggering snapshot due to closeness to SLO for %s at measuring point %s. Value is %s",
						event.getEntity().getProcessingType().getMeasurementSpecification().getMetricDescription()
								.getName(),
						event.getEntity().getMeasuringPoint().getStringRepresentation(), value.toString()));
				this.mp2range.clear(); // reset to avoid additional Snapshot Initiations.
				return Result.of(new SnapshotInitiated(0.0));
			}
		}

		return Result.empty();
	}
	

	/**
	 * Value range with upper and lower bound. Bounds are calculated based on the
	 * upper and lower thresholds of the SLO and the provided {@code sensitivity}.
	 * <br>
	 * The {@code sensitivity} must be in [0.0, 1.0]. 
	 * A higher {@code sensitivity} implies a smaller acceptable value range.
	 * <br>
	 * Example: SLO wih upper threshold 5.0 and lower threshold 1.0
	 * <ul>
	 * <li>sensitivity = 0:</li>
	 * <ul>
	 * <li>lower = 1.0 + ((5.0 - 1.0) / 2) * 0 = 1.0</li>
	 * <li>upper = 5.0 - ((5.0 - 1.0) / 2) * 0 = 5.0</li>
	 * </ul>
	 * <li>sensitivity = 0.5:</li>
	 * <ul>
	 * <li>lower = 1.0 + ((5.0 - 1.0) / 2) * 0.5 = 2.0</li>
	 * <li>upper = 5.0 - ((5.0 - 1.0) / 2) * 0.5 = 4.0</li>
	 * </ul>
	 * <li>sensitivity = 1:</li>
	 * <ul>
	 * <li>lower = 1.0 + ((5.0 - 1.0) / 2) * 1 = 3.0</li>
	 * <li>upper = 5.0 - ((5.0 - 1.0) / 2) * 1 = 3.0</li>
	 * </ul>
	 * </ul>
	 *
	 * If a measured value is greater than the upper bound, or smaller than the
	 * lower bound, the range is considered violated and the simulation run should
	 * stop.
	 *
	 * @author Sarah Stieß
	 *
	 */
	private abstract class ValueRange {
		protected final double sensitivity;
		protected final double upper;
		protected final double lower;

		/**
		 * Creates a instance of {@link ValueRange}.
		 * 
		 * Upper and lower bound of the range are calculated based on the upper and lower bound of the corresponding SLO and the given sensitivity. 
		 * A higher {@code sensitivity} implies a smaller acceptable value range. 
		 *
		 *
		 * @param upper       Upper Threshold of SLO
		 * @param lower       Lower Threshold of SLO
		 * @param sensitivity Number in [0,1] where 0 is insensible, and 1 is very
		 *                    sensible.
		 */
		public ValueRange(final Measure<?, Quantity> upper, final Measure<?, Quantity> lower,
				final double sensitivity) {
			this.sensitivity = sensitivity;

			final double u = upper.doubleValue(upper.getUnit());
			final double l = lower.doubleValue(lower.getUnit());

			final double middle = (u - l) / 2.0;

			this.upper = u - this.sensitivity * middle;
			this.lower = l + this.sensitivity * middle;
		}

		/**
		 * Determines, whether {@code value} violates this value range.
		 *
		 * @param value
		 * @return true, if this range is violated, false otherwise.
		 */
		public abstract boolean isViolatedBy(final double value);

		/**
		 * Determines, whether {@code value} violates this value range's lower boundary.
		 *
		 * @param value
		 * @return true, if this range's lower boundary is violated, false otherwise.
		 */
		public boolean isLowerViolatedBy(final double value) {
			return value <= this.lower;
		}

		/**
		 * Determines, whether {@code value} violates this value range's upper boundary.
		 *
		 * @param value
		 * @return true, if this range's upper boundary is violated, false otherwise.
		 */
		public boolean isUpperViolatedBy(final double value) {
			return value >= this.upper;
		}
	}

	/**
	 * Value range with upper and lower bound.
	 *
	 * @author Sarah Stieß
	 *
	 */
	private class DoubleEndedRange extends ValueRange {

		/**
		 * @see {@link ValueRange}
		 */
		public DoubleEndedRange(final Measure<Object, Quantity> upper, final Measure<Object, Quantity> lower,
				final double significance) {
			super(upper, lower, significance);
		}

		@Override
		public boolean isViolatedBy(final double value) {
			return this.isUpperViolatedBy(value) || this.isLowerViolatedBy(value);
		}
	}

	/**
	 * Value range with only an upper bound. For calculation, the lower
	 * bound is treated as zero. For checks, only the upper bound is considered.
	 *
	 * @author Sarah Stieß
	 *
	 */
	private class SingleEndedRange extends ValueRange {

		/**
		 * Defaults to 0 as lower threshold.
		 *
		 * @see {@link ValueRange}
		 */
		public SingleEndedRange(final Measure<Object, Quantity> upper, final double significance) {
			super(upper, (Measure<Double, Quantity>) (Measure<Double, ?>) Measure.valueOf(0.0, Dimensionless.UNIT),
					significance);
		}

		@Override
		public boolean isViolatedBy(final double value) {
			return this.isUpperViolatedBy(value);
		}

		/**
		 * @return false, as there is no lower boundary to single ended range.
		 */
		@Override
		public boolean isLowerViolatedBy(final double value) {
			return false;
		}
	}
}
