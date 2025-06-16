package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.jobs;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.SimulationStarter;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.configuration.InitialiseSimulationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.workflow.WorkflowConfigurationModule;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * This class is responsible for starting the explorer.
 *
 * @author Sarah Stieß
 */
public class InitialiseAndRunSimulationJob implements IBlackboardInteractingJob<MDSDBlackboard> {

	private static final Logger LOGGER = Logger.getLogger(InitialiseAndRunSimulationJob.class.getName());

	private MDSDBlackboard blackboard;
	private final PCMResourceSetPartitionProvider pcmResourceSetPartitionProvider;

	private final InitialiseSimulationWorkflowConfiguration configuration;

	public InitialiseAndRunSimulationJob(final InitialiseSimulationWorkflowConfiguration config) {
		this.configuration = config;
		this.pcmResourceSetPartitionProvider = Slingshot.getInstance()
				.getInstance(PCMResourceSetPartitionProvider.class);
	}

	@Override
	public void execute(final IProgressMonitor monitor) throws JobFailedException, UserCanceledException {

		LOGGER.info("**** SimulationJob.prepare ****");
		final PCMResourceSetPartition partition = (PCMResourceSetPartition) this.blackboard
				.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);

		WorkflowConfigurationModule.simuComConfigProvider.set(this.configuration.getSimuComConfig());
		WorkflowConfigurationModule.blackboardProvider.set(this.blackboard);

		this.pcmResourceSetPartitionProvider.set(partition);

		LOGGER.debug("monitor: " + monitor.getClass().getName());
		monitor.beginTask("Start Simulation", 3);

		monitor.subTask("Initialize driver");

		// this.configuration.getlaunchConfigParams() --> access to all the params.

		final SimulationStarter explorer = new SimulationStarter(this.configuration.getSimuComConfig(), monitor,
				this.blackboard, this.configuration.getSnapshotFile(), this.configuration.getOtherConfigsFile(), this.configuration.getResultsFolder(), this.configuration.getNextStateId());
		explorer.simulateSingleState(this.configuration.getResultsFolder());

		monitor.worked(1);

		monitor.subTask("Start simulation");
		monitor.worked(1);

		monitor.subTask("Restore");
		monitor.worked(1);

		monitor.done();

		LOGGER.info("**** SimulationJob.execute  - Done ****");
	}

	@Override
	public void cleanup(final IProgressMonitor monitor) throws CleanupFailedException {

	}

	@Override
	public String getName() {
		return this.getClass().getCanonicalName();
	}

	@Override
	public void setBlackboard(final MDSDBlackboard blackboard) {
		this.blackboard = blackboard;
	}

}
