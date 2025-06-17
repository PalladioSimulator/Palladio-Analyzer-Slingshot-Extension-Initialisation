package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.configuration;

import org.palladiosimulator.analyzer.slingshot.core.api.SimulationConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.SimulationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.events.PCMWorkflowConfiguration;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

public class InitialiseSimulationWorkflowConfiguration extends SimulationWorkflowConfiguration implements PCMWorkflowConfiguration, SimulationConfiguration {

	private final Locations locations;
	private final String nextStateId;
	
	/**
	 * Create a new Workflowconfiguration for running an initialised Slingshot simulation.
	 * 
	 * @param configuration
	 * @param locations
	 * @param nextStateId
	 */
	public InitialiseSimulationWorkflowConfiguration(final SimuComConfig configuration, final Locations locations, final String nextStateId) {
		super(configuration);
		this.locations = locations;
		this.nextStateId = nextStateId;
	}

	public Locations getLocations() {
		return this.locations;
	}
	
	public String getNextStateId() {
		return this.nextStateId;
	}
}
