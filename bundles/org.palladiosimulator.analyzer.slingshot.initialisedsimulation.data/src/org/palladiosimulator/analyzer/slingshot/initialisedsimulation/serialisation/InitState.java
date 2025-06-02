package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialisation;

import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;

/**
 * Class to (de-)serialize the snapshots from/to.
 *
 * @author Sophie Stieß
 *
 */
public class InitState {

	private final String id;
	private final double pointInTime;
	private final Snapshot snapshot;
	
	/**
	 * 
	 * @param pointInTime point in time of the snapshot
	 * @param snapshot actual content of the snapshot
	 * @param id id of the state the snapshot was taken for
	 */
	public InitState(final double pointInTime, final Snapshot snapshot, final String id) {
		this.id = id;
		this.pointInTime = pointInTime;
		this.snapshot = snapshot;
		
	}

	/**
	 * 
	 * @return the snapshot
	 */
	public Snapshot getSnapshot() {
		return snapshot;
	}

	/**
	 * 
	 * @return id of the state the snapshot was taken for
	 */
	public String getId() {
		return id;
	}

	/**
	 * 
	 * @return point in time of the snapshot
	 */
	public double getPointInTime() {
		return this.pointInTime;
	}
}
