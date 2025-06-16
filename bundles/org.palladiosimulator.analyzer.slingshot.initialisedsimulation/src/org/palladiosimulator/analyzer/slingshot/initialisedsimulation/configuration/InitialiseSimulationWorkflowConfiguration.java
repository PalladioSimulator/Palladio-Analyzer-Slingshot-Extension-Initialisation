package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.configuration;

import java.nio.file.Path;
import java.util.Map;

import org.palladiosimulator.analyzer.slingshot.core.api.SimulationConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.events.PCMWorkflowConfiguration;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;

public class InitialiseSimulationWorkflowConfiguration extends ExplorationWorkflowConfiguration
		implements PCMWorkflowConfiguration, SimulationConfiguration {

	private final Path snapshotFile;
	private final Path otherConfigsFile;
	private final Path resultsFolder;
	
	private final String nextStateId;
	
	/**
	 * Create a new Workflowconfiguration for running an initialised Slingshot simulation.
	 * 
	 * @param configuration
	 * @param launchConfigurationParams
	 * @param snapshotFile
	 * @param otherConfigsFile
	 * @param resultsFolder
	 * @param nextStateId
	 */
	public InitialiseSimulationWorkflowConfiguration(final SimuComConfig configuration,
			final Map<String, Object> launchConfigurationParams, final Path snapshotFile, final Path otherConfigsFile,
			final Path resultsFolder, final String nextStateId) {
		super(configuration, launchConfigurationParams);
		this.snapshotFile = snapshotFile;
		this.otherConfigsFile = otherConfigsFile;
		this.resultsFolder = resultsFolder;
		this.nextStateId = nextStateId;
	}

	public Path getSnapshotFile() {
		return this.snapshotFile;
	}

	public Path getResultsFolder() {
		return this.resultsFolder;
	}

	public Path getOtherConfigsFile() {
		return this.otherConfigsFile;
	}
	
	public String getNextStateId() {
		return this.nextStateId;
	}
}
