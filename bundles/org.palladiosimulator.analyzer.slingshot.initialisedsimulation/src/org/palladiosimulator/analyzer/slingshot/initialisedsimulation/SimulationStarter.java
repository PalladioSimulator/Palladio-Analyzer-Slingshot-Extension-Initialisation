package org.palladiosimulator.analyzer.slingshot.initialisedsimulation;

import java.nio.file.Path;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.ArchitectureConfigurationUtil;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate.StateBuilder;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.AdditionalConfigurationModule;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.InitWrapper;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialisation.InitState;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialisation.OtherInitThings;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.InitStateDeSerialization;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.OtherStuffDeserialization;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.ResultStateSerialization;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * Initialises and runs a singles simulation and saves the results to file.
 *
 * @author Sophie Stieß
 *
 */
public class SimulationStarter {

	private static final Logger LOGGER = Logger.getLogger(SimulationStarter.class.getName());

	private static final String resultFile = "state.state";
	private static final String snapshotFile = "snapshot.snapshot";

	private final PCMResourceSetPartition initModels;

	final SimulationDriver driver;
	final StateBuilder stateBuilder;

	/**
	 * Create a new {@link SimulationStarter} and already initiate the simulation driver.
	 * 
	 * @param config
	 * @param monitor
	 * @param blackboard
	 * @param initStateLocation
	 * @param otherLocation
	 * @param resultLocation
	 */
	public SimulationStarter(final SimuComConfig config, final IProgressMonitor monitor,
			final MDSDBlackboard blackboard, final Path initStateLocation, final Path otherLocation,
			final Path resultLocation, final String nextStateId) {
		super();
		this.initModels = (PCMResourceSetPartition) blackboard
				.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);

		// copy BEFORE loading the snapshot, because of the cost stereotype resolution
		final URI resultFolder = URI.createFileURI(resultLocation.toString());
		ArchitectureConfigurationUtil.copyToURI(initModels.getResourceSet(), resultFolder);

		final InitState initstate = getInitState(initStateLocation);
		final OtherInitThings otherInitThings = getOthers(otherLocation);

		final InitWrapper wrapper = new Preprocessor(this.initModels, initstate.getSnapshot(),
				otherInitThings.getIncomingPolicies()).createWrapper();
		final SnapshotConfiguration snaphshotConfig = new SnapshotConfiguration(initstate.getPointInTime() > 0.0,
				otherInitThings.getSensibility(), config.getSimuTime());
		this.stateBuilder  = new StateBuilder(initstate.getId(), initstate.getPointInTime(), this.initModels, nextStateId);
		AdditionalConfigurationModule.snapConfigProvider.set(snaphshotConfig);
		AdditionalConfigurationModule.eventsToInitOnProvider.set(wrapper);
		AdditionalConfigurationModule.defaultStateProvider.set(stateBuilder);

		this.driver = Slingshot.getInstance().getSimulationDriver();
		this.driver.init(config, monitor);
	}

	/**
	 * Load the initial state of the simulator from file.
	 * 
	 * Beware: Due to a bug(?) regarding the resolution and loading of the stereotypes,
	 * this operation must only be called AFTER
	 * {@link ArchitectureConfigurationUtil#copyToURI(org.eclipse.emf.ecore.resource.ResourceSet, URI)}. 
	 * 
	 * @param location location of the file to be loaded
	 * @return state to initiate the simulator to
	 */
	private InitState getInitState(final Path location) {
		final InitStateDeSerialization thing = new InitStateDeSerialization(this.initModels);
		return thing.deserialize(location);
	}

	/**
	 * Load dditional configurations for initialising the simulator and the snapshot components from file. 
	 * 
	 * @param location location of the file to be loaded
	 * @return additional configurations for initialising the simulator and the snapshot components.
	 */
	private OtherInitThings getOthers(final Path location) {
		final OtherStuffDeserialization stuff = new OtherStuffDeserialization(this.initModels);
		return stuff.deserialize(location);
	}

	/**
	 * Exectue the simulation and save the results to file.
	 * 
	 * @param resultLocation location to save the state and snapshot to.
	 */
	public void simulateSingleState(final Path resultLocation) {
		LOGGER.info("********** DefaultGraphExplorer.explore() **********");

		driver.start();
		UpdateSPDUtil.reduceTriggerTime(PCMResourcePartitionHelper.getSPD(initModels), stateBuilder.getDuration());

		final Path snapPath = Path.of(resultLocation.toString(), snapshotFile);
		(new InitStateDeSerialization(this.initModels)).serialize(stateBuilder.buildInitState(), snapPath);

		final Path resultPath = Path.of(resultLocation.toString(), resultFile);
		(new ResultStateSerialization()).serialize(stateBuilder.buildResultState(), resultPath);
	}
}
