package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers;

import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Provides the event to init the next simulation run on.
 */
@Singleton
public class EventsToInitOnProvider implements Provider<InitWrapper> {

	private InitWrapper eventsToInitOn;

	public void set(final InitWrapper eventsToInitOn) {
		this.eventsToInitOn = eventsToInitOn;
	}

	@Override
	public InitWrapper get() {
		return eventsToInitOn;
	}

}
