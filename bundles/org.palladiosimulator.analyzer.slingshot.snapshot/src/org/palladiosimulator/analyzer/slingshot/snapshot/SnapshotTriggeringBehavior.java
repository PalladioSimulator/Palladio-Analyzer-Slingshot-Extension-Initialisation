package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.StateBuilder;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;
import org.palladiosimulator.semanticspd.ServiceGroupCfg;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.adjustments.StepAdjustment;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.ServiceGroup;
import org.palladiosimulator.spd.targets.TargetGroup;

/**
 *
 * Triggers the creation of a {@link Snapshot} if a reconfiguration was
 * triggered, but before it gets applied.
 *
 * Drops reconfigurations under certain circumstances. 
 *
 * @author Sophie Stieß
 *
 */
public class SnapshotTriggeringBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotTriggeringBehavior.class);

	private final List<ModelAdjustmentRequested> adjustmentEvents;

	private final StateBuilder state;
	private final SimulationScheduling scheduling;

	private final boolean activated;

	private final Configuration config;

	@Inject
	public SnapshotTriggeringBehavior(final @Nullable StateBuilder state,
			final @Nullable EventsToInitOnWrapper eventsWapper, final SimulationScheduling scheduling,
			final @Nullable Configuration config) {
		this.state = state;
		this.scheduling = scheduling;
		this.adjustmentEvents = eventsWapper == null ? null : eventsWapper.getAdjustmentEvents();
		this.config = config;

		this.activated = state != null && eventsWapper != null;
	}

	@Override
	public boolean isActive() {
		return this.activated;
	}

	/**
	 * 
	 * Preintercept {@link ModelAdjustmentRequested} and trigger a the creation of a
	 * {@link Snapshot}, if the intercepted event belongs to a reactive
	 * reconfiguration.
	 * 
	 * Beware: all {@link ModelAdjustmentRequested} events that are part of the
	 * simulation run initialisation must be ignored.
	 * 
	 * @param information
	 * @param event
	 * @return
	 */
	@PreIntercept
	public InterceptionResult preInterceptModelAdjustmentRequested(final InterceptorInformation information,
			final ModelAdjustmentRequested event) {
		// only intercept triggered adjustments. do not intercept snapped adjustments..
		// assumption: do not copy adjustor events from the FEL, i.e. the "first"
		// adjustor is always from the snapshot.
		if (adjustmentEvents.contains(event)) {
			LOGGER.debug(String.format("Succesfully route %s to %s", event.getName(),
					information.getEnclosingType().get().getSimpleName()));
			return InterceptionResult.success();
		}

		// keep or delete?
		if (isDrop(event.getScalingPolicy())) {
			return InterceptionResult.abort();
		}

		state.addReasonToLeave(ReasonToLeave.reactiveReconfiguration);
		scheduling.scheduleEvent(new SnapshotInitiated(0, event));

		LOGGER.debug(String.format("Abort routing %s to %s", event.getName(),
				information.getEnclosingType().get().getSimpleName()));
		return InterceptionResult.abort();
	}

	/**
	 * Drop the application of a scaling policy, if the adjustment is a scale in on
	 * the minimal architecture reconfiguration.
	 * 
	 * This is some kind of short-cut to the {@link SnapshotAbortionBehavior} that
	 * ensures a decent state length. If we rely {@link SnapshotAbortionBehavior}
	 * only, we get a lot of aborted states that could have been skipped. Scale ins
	 * on the minimal architecture reconfiguration keep happening if the demand is
	 * very low. But also, reconfiguration makes no sense in these cases, thus it is
	 * reasonable to drop them.
	 *
	 * @param policy
	 * @return true iff the policy shall be dropped, false otherwise.
	 */
	private boolean isDrop(final ScalingPolicy policy) {
		if (policy.getAdjustmentType() instanceof final StepAdjustment adjustment && adjustment.getStepValue() < 0) {
			// Scale in!
			final TargetGroup tg = policy.getTargetGroup();
			if (tg instanceof final ElasticInfrastructure ei) {
				final List<ElasticInfrastructureCfg> elements = config.getTargetCfgs().stream()
						.filter(ElasticInfrastructureCfg.class::isInstance).map(ElasticInfrastructureCfg.class::cast)
						.filter(eic -> eic.getUnit().getId().equals(ei.getUnit().getId())).toList();

				if (elements.size() != 1) {
					throw new RuntimeException("Help, wrong number of matching elastic infrastructure group configs.");
				}

				return elements.get(0).getElements().size() == 1;
			}

			if (tg instanceof final ServiceGroup sg) {
				final List<ServiceGroupCfg> elements = config.getTargetCfgs().stream()
						.filter(ServiceGroupCfg.class::isInstance).map(ServiceGroupCfg.class::cast)
						.filter(sgc -> sgc.getUnit().getId().equals(sg.getUnitAssembly().getId())).toList();

				if (elements.size() != 1) {
					throw new RuntimeException("Help, wrong number of matching service group configs.");
				}

				return elements.get(0).getElements().size() == 1;
			}

		}
		return false;
	}
}
