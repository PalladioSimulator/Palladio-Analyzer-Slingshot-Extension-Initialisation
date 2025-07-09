package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.annotations.Nullable;
import org.palladiosimulator.analyzer.slingshot.common.events.modelchanges.ModelAdjusted;
import org.palladiosimulator.analyzer.slingshot.common.utils.SPDHelper;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.ReasonToLeave;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.StateBuilder;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.InitWrapper;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.ConfigurableSnapshotExtension;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.TargetGroupCfg;
import org.palladiosimulator.spd.SPD;

/**
 *
 * Abort simulation runs, that start with scaling policies that yield no
 * changes. <br>
 * Possible policies that yield no changes:
 * <ul>
 * <li>Scale-in on an already minimal architecture, where not further resources
 * or services can be removed.</li>
 * <li>Scale-in or scale-out that are blocked due to target group size
 * constraints, i.e., when attempting to scale from 3 to 4 resources, while a
 * target group constraint limit the number of resources to 3.</li>
 * <li>Combined scale-in and scale-out policies, that cancel each other, e.g., a
 * stepwise scale-out of +1 and a stepwise scale-in of -1.</li>
 * </ul>
 * 
 * In all of the above cases the architecture remains unchanged. As such, the
 * simulation run would behave similar to a simulation run with the same parent
 * state and a NOOP transition.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = ModelAdjusted.class, then = {})
public class SnapshotAbortionBehavior extends ConfigurableSnapshotExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotAbortionBehavior.class);

	private final List<ModelAdjustmentRequested> adjustmentEvents;

	private int adjusmentCounter = 0;

	private final StateBuilder state;
	private final SimulationScheduling scheduling;

	private final boolean activated;

	/* Covers all of the three types. */
	private final Map<TargetGroupCfg, Integer> tg2size;

	private final Configuration config;

	@Inject
	public SnapshotAbortionBehavior(final @Nullable StateBuilder state,
			final @Nullable InitWrapper eventsWapper, final SimulationScheduling scheduling,
			@Nullable final Configuration semanticSpd, @Nullable final SPD spd, @Nullable final SnapshotConfiguration snapConfig) {
		
		super(snapConfig);
		
		this.state = state;
		this.scheduling = scheduling;
		this.adjustmentEvents = eventsWapper == null ? null : eventsWapper.getAdjustmentEvents();

		this.config = semanticSpd;

		this.tg2size = new HashMap<>();

		if (spd != null && semanticSpd != null) {

			/*
			 * [S3] Consider only target group configs with a matching target group. This is
			 * necessary, because apparently some scale ins reduce the number of assemblies,
			 * but not the number of resource containers. Unclear whether this is a bug or a
			 * feature in the SPD transformations.
			 */
			final Set<Entity> targetGroups = spd.getTargetGroups().stream().map(tg -> SPDHelper.getUnitOf(tg))
					.collect(Collectors.toSet());

			for (final TargetGroupCfg tgcfg : semanticSpd.getTargetCfgs()) {
				if (targetGroups.contains(SPDHelper.getUnitOf(tgcfg))) {
					tg2size.put(tgcfg, SPDHelper.getSizeOf(tgcfg));
				}
			}
		}

		this.activated = state != null && eventsWapper != null && !this.tg2size.isEmpty();
	}

	@Override
	public boolean getActivated() {
		return this.activated;
	}

	/**
	 * 
	 * Checker whether the number of elements in any target group changed after all
	 * planned adjustment were applied.
	 * 
	 * If the number of elements in all target groups remains unchanged or are back
	 * to the initial number, this simulation is aborted. As this operation works
	 * based on the number of elements, the elements themselves might still change,
	 * e.g., if a sale-in removes a old element and a scale-out adds a new one, the
	 * total number of elements is the same, eventhough one element was replaced.
	 * 
	 * Requires, that all injected reconfigurations happen before further reactive
	 * reconfiguration happen. This is not explicitly ensured by the simulation
	 * engine, but as of now it always held.
	 * 
	 * @param modelAdjusted
	 */
	@Subscribe
	public void onModelAdjusted(final ModelAdjusted modelAdjusted) {
		adjusmentCounter++;

		if (modelAdjusted.time() > 0.0 && adjusmentCounter <= adjustmentEvents.size()) {
			throw new IllegalStateException("Missing model adjusted events from init");
		}

		if (!modelAdjusted.isWasSuccessful()) {
			; // TODO is this the correct behaviour?
		}

		if (adjusmentCounter == adjustmentEvents.size()) {
			LOGGER.debug("Beginn Abortion check");

			for (final TargetGroupCfg tgcfg : config.getTargetCfgs()) {
				if (tg2size.containsKey(tgcfg)) {
					LOGGER.debug(tgcfg.getClass().getSimpleName() + ": old " + tg2size.get(tgcfg) + " new "
							+ SPDHelper.getSizeOf(tgcfg));
					if (tg2size.get(tgcfg) != SPDHelper.getSizeOf(tgcfg)) {
						return;
					}
				}
			}
			state.addReasonToLeave(ReasonToLeave.aborted);
			scheduling.scheduleEvent(new SnapshotInitiated(0.0));
			LOGGER.debug("Abort");
		}
	}



}
