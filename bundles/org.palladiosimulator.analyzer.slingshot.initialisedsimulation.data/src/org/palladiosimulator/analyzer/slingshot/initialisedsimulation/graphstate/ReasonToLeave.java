package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate;

/**
 * Describes the possible reasons for terminating a simulation run, i.e
 * finalising one model state and transitioning to the next.
 *
 * Those reasons are not mutually exclusive. E.g., if the
 * simulation run stops after the predefined simulation time t_max, but a
 * reconfiguration is triggered at t_max as well, then the resulting state should
 * be tagged with both reasons.
 * 
 * @author Sophie Stieß
 *
 */
public enum ReasonToLeave {
	/** 
	 * The simulation run ended after the predefined simulation time. 
	 * This is the default reason, if none of the others happened.
	 */
	interval,
	/**
	 * The simulation run ended because a reactive reconfiguration occurred. For
	 * more details, c.f.
	 * {@link org.palladiosimulator.analyzer.slingshot.snapshot.SnapshotTriggeringBehavior}
	 * and also
	 * {@link org.palladiosimulator.analyzer.slingshot.snapshot.SnapshotStateUpdateBehaviour#refineReasonsToLeave}.
	 */
	reactiveReconfiguration,
	/**
	 * The simulation run ended because some measurements were too close to an SLO,
	 * for more details c.f.
	 * {@link org.palladiosimulator.analyzer.slingshot.snapshot.SnapshotSLOTriggeringBehavior}.
	 */
	closenessToSLO,
	/**
	 * The simulation run ended because it is not worth to continue exploring this
	 * branch. Possible reasons for abortion are:
	 * <li> Adjustments that have no effect on the architecture (c.f. {@link org.palladiosimulator.analyzer.slingshot.snapshot.SnapshotAbortionBehavior}).
	 * <li> violation of a hard SLO-threshold (c.f. {@link org.palladiosimulator.analyzer.slingshot.snapshot.SnapshotSLOAbortionBehavior}).
	 */
	aborted;
}
