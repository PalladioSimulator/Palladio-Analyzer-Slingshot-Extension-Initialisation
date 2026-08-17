package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.StateBuilder;



/**
 * Provides the {@link StateBuilder} that represents the next simulation run.
 *
 * @author Sarah Stieß
 *
 */
@Singleton
public class DefaultStateProvider implements Provider<StateBuilder> {

	private StateBuilder state;

	public void set(final StateBuilder state) {
		this.state = state;
	}

	@Override
	public StateBuilder get() {
		return state;
	}

}
