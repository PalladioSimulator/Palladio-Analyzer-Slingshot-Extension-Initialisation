package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractSimulationEvent;

public class SimpleEvent extends AbstractSimulationEvent {
	
	private final SimpleEntity thing = new SimpleEntity("value");

	public SimpleEntity getThing() {
		return thing;
	}
}