package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.common.utils.SPDHelper;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.StateBuilder;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.InitWrapper;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.ConfigurableSnapshotExtension;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.TargetGroupCfg;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.adjustments.StepAdjustment;
import org.palladiosimulator.spd.targets.TargetGroup;

/**
 *
 * Initiates the creation of a {@link Snapshot} if a reconfiguration was
 * triggered reactively.
 * <br>
 * Beware: The snapshot will be taken <i>before</i> the reconfiguration is applied to the architecture. 
 * In Slinghsot, a reconfiguration has two phases, denoted by the events {@link ModelAdjustmentRequested} and {@link ModelAdjusted}.
 * Instances of {@link ModelAdjustmentRequested} are published to indicated that a policy should be applied accoording to its triggers. The architecture is still unchanged.
 * Instances of {@link ModelAdjusted} are published to indicate that a policy has been applied, i.e. the architecutre has changed.
 * <br>
 * Drops reconfigurations under certain circumstances, see {@link SnapshotTriggeringBehavior#isDrop(ScalingPolicy)} for details.
 * To deactivate the dropping, add {@code "doDrop" : false} to the configuration paratmeters.
 * <br>
 * This extension provides a configuration parameter called "dropInterval". The value must be a double.
 * If the value is greater than 0.0, this behaviour extension drops all reactive reconfigurations during the interval [0.0, value).
 * Beware, this also changes the beheaviour of the simulation, as droping a reconfiguration is implemented by aborting the respective {@link ModelAdjustmentRequested} event.
 * <br>
 * <br>
 *
 * @author Sophie Stieß
 *
 */
public class SnapshotTriggeringBehavior extends ConfigurableSnapshotExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotTriggeringBehavior.class);

	private final List<ModelAdjustmentRequested> adjustmentEvents;

	private final StateBuilder state;
	private final SimulationScheduling scheduling;

	private final boolean activated;

	private final Configuration config;
	
	private final boolean doDrop;
	private final double delay;
	private final static String DO_DROP = "dodrop"; // TODO : dropEffectlessAdaptions
	private final static String DELAY = "dropInterval";
	
	@Inject
	public SnapshotTriggeringBehavior(final @Nullable StateBuilder state,
			final @Nullable InitWrapper eventsWapper, final SimulationScheduling scheduling,
			final @Nullable Configuration semanticSpd, final @Nullable SnapshotConfiguration config) {
		
		super(config);
		
		this.state = state;
		this.scheduling = scheduling;
		this.adjustmentEvents = eventsWapper == null ? null : eventsWapper.getAdjustmentEvents();
		this.config = semanticSpd;

		this.doDrop = this.toggle.hasParameter(DO_DROP, Boolean.class) ? this.toggle.getParameter(DO_DROP) : true; 
		this.delay = this.toggle.hasParameter(DELAY, Double.class) ? this.toggle.getParameter(DELAY) : 0.0; 
		
		this.activated = state != null && eventsWapper != null;
	}

	@Override
	public boolean getActivated() {
		return this.activated;
	}

	/**
	 * 
	 * Preintercept {@link ModelAdjustmentRequested} events and initiate creation of
	 * a {@link Snapshot}, if the intercepted event belongs to a reactive
	 * reconfiguration.
	 * 
	 * Beware: all {@link ModelAdjustmentRequested} events that are part of the
	 * simulation run initialisation must and will be ignored.
	 * 
	 * Beware: this must be a preinterception instead of a regular subscription, as
	 * we must initiate the snapshot before the architecture changes, i.e. before
	 * the behaviour extension that executes the architecture transformations
	 * receives the {@link ModelAdjustmentRequested} event. To ensure that the
	 * architecture remains unchanged for the snapshot, the
	 * {@link ModelAdjustmentRequested} events are aborted, if a snapshot was
	 * initiated.
	 * 
	 * @param information
	 * @param event
	 * @return
	 */
	@PreIntercept
	public InterceptionResult preInterceptModelAdjustmentRequested(final InterceptorInformation information,
			final ModelAdjustmentRequested event) {
		// only intercept triggered adjustments. do not intercept snapped adjustments..
		// assumption: do not copy adjustment requested events from the FEL, i.e. the "first"
		// adjustment requested events are always from the snapshot.
		if (adjustmentEvents.contains(event)) {
			LOGGER.debug(String.format("Succesfully route %s to %s", event.getName(),
					information.getEnclosingType().get().getSimpleName()));
			return InterceptionResult.success();
		}
		
		if (event.time() < this.delay) {
			return InterceptionResult.abort();
		}

		// keep or delete?
		if (isDrop(event.getScalingPolicy())) {
			return InterceptionResult.abort();
		}

		state.addReasonToLeave(ReasonToLeave.reactiveReconfiguration);
		scheduling.scheduleEvent(new SnapshotInitiated(event));

		LOGGER.debug(String.format("Abort routing %s to %s", event.getName(),
				information.getEnclosingType().get().getSimpleName()));
		return InterceptionResult.abort();
	}

	/**
	 * Drop the application of a scaling policy, if the adjustment is a scale in on
	 * a minimal architecture reconfiguration.
	 * 
	 * This is a short-cut to the {@link SnapshotAbortionBehavior} that
	 * ensures a decent state length. If we rely on {@link SnapshotAbortionBehavior}
	 * only, we get a lot of aborted states that could have been skipped. Scale ins
	 * on the minimal architecture reconfiguration keep happening if the demand is
	 * very low. But also, reconfiguration makes no sense in these cases, thus it is
	 * reasonable to drop them.
	 *
	 * @param policy
	 * @return true iff the policy shall be dropped, false otherwise.
	 */
	private boolean isDrop(final ScalingPolicy policy) {
		if (this.doDrop && policy.getAdjustmentType() instanceof final StepAdjustment adjustment && adjustment.getStepValue() < 0) {
			// Scale in!
			final TargetGroup tg = policy.getTargetGroup();			
			final TargetGroupCfg tgCfg = SPDHelper.getMatchingTargetGroupCfg(tg, config);
			
			return SPDHelper.isMinTargetGroup(tgCfg, SPDHelper.getTargetGroupSizeConstraint(tg));

		}
		return false;
	}
}
