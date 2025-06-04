package org.palladiosimulator.analyzer.slingshot.snapshot.entities;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorState;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;

/**
 *
 * Snaphshot of a Simulation, with out any additional duplication etc.
 * 
 * As most {@link DESEvent}s are mutable, proceed with caution, if you want use
 * this snapshot to initialise a simulation run.
 *
 * @author Sophie Stieß
 *
 */
public final class PlainSnapshot implements Snapshot {	
	
	private final Set<DESEvent> events;
	private final List<ModelAdjustmentRequested> modelAdjustmentRequestedEvents;
	private final Collection<SPDAdjustorState> states;

	public PlainSnapshot(final Set<DESEvent> events, final Collection<SPDAdjustorState> states) {
		this.modelAdjustmentRequestedEvents = events.stream().filter(ModelAdjustmentRequested.class::isInstance).map(ModelAdjustmentRequested.class::cast).toList();
		this.events = new HashSet<>(events);
		this.events.removeAll(this.modelAdjustmentRequestedEvents);
		this.states = states;
	}


	@Override
	public Set<DESEvent> getEvents() {
		return Set.copyOf(this.events);
	}

	@Override
	public List<ModelAdjustmentRequested> getModelAdjustmentRequestedEvent() {
		return List.copyOf(this.modelAdjustmentRequestedEvents);
	}

	@Override
	public Collection<SPDAdjustorState> getSPDAdjustorStates() {
		return Set.copyOf(this.states);
	}
}
