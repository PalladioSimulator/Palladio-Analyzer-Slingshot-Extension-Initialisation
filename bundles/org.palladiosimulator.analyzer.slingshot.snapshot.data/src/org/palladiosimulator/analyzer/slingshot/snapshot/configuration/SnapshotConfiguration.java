package org.palladiosimulator.analyzer.slingshot.snapshot.configuration;

import java.util.Map;

/**
 *
 * i'm probably putting this at the wrong places, put i need to place it somewhere.
 *
 * this configuration should contain simulation configuration regarding the snapshot / preinitialisation, such as
 * whether to start normal or with init events, when to take a snapshot, etc.
 *
 * recording snapshot :
 * - record at all (yes / no)
 * - point in time (single, interval)
 * - boolean condition (e.g. after reconfiguration triggered)
 *
 * starting from snapshot :
 * - yes / no
 * - which snapshot?
 *
 * @author Sophie Stieß
 *
 */
public final class SnapshotConfiguration {

	private final double minDuration;
	private final boolean startFromSnapshot;
	
	private final Map<String, SnapshotBehaviourConfigurationParameters> parameters;

	public SnapshotConfiguration(final boolean startFromSnapshot, final double minDuration, final Map<String, SnapshotBehaviourConfigurationParameters> parameters) {
		this.startFromSnapshot = startFromSnapshot;
		this.minDuration = minDuration;
		this.parameters = parameters;
	}

	public double getMinDuration() {
		return this.minDuration;
	}

	public Map<String, SnapshotBehaviourConfigurationParameters> getConfigurationParameters() {
		return this.parameters;
	}

	public boolean isStartFromSnapshot() {
		return startFromSnapshot;
	}
}
