package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.StateBuilder;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;

/**
 *
 * Additional provisionings for state exploration.
 *
 * @author Sarah Stieß
 *
 */
public class AdditionalConfigurationModule extends AbstractSlingshotExtension{

	public static final SnapshotConfigurationProvider snapConfigProvider = new SnapshotConfigurationProvider();
	public static final ExploredStateBuilderProvider defaultStateProvider = new ExploredStateBuilderProvider();
	public static final EventsToInitOnProvider eventsToInitOnProvider = new EventsToInitOnProvider();

	public AdditionalConfigurationModule() {
	}

	@Override
	protected void configure() {
		bind(SnapshotConfiguration.class).toProvider(snapConfigProvider);
		bind(StateBuilder.class).toProvider(defaultStateProvider);
		bind(EventsToInitOnWrapper.class).toProvider(eventsToInitOnProvider);
	}

	@Override
	public String getName() {
		return "Additional Configuration for Simulation Initalisation";
	}
}
