package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.explorer.configuration;

import org.eclipse.debug.core.ILaunch;
import org.palladiosimulator.analyzer.workflow.jobs.LoadModelIntoBlackboardJob;
import org.palladiosimulator.analyzer.workflow.jobs.PreparePCMBlackboardPartitionJob;

import de.uka.ipd.sdq.workflow.jobs.ICompositeJob;
import de.uka.ipd.sdq.workflow.jobs.SequentialBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 *
 * Root Job to run an exploration.
 *
 * Prepares the Blackboar, the Partition within the Blackboard, and starts the
 * exploration itself.
 *
 * @author Sarah Stieß
 *
 */
public class InitialiseSimulationRootJob extends SequentialBlackboardInteractingJob<MDSDBlackboard> implements ICompositeJob {

	public InitialiseSimulationRootJob(final InitialiseSimulationWorkflowConfiguration config, final ILaunch launch) {
		super(InitialiseSimulationRootJob.class.getName(), false);

		if (launch == null) {
			// Nothing. just stay aware that lunch is null, if it is a headless run.
		}

		this.addJob(new PreparePCMBlackboardPartitionJob());
		config.getPCMModelFiles()
		.forEach(modelFile -> LoadModelIntoBlackboardJob.parseUriAndAddModelLoadJob(modelFile, this));
		this.addJob(new InitialiseAndRunSimulationJob(config));


	}
}
