package org.palladiosimulator.analyzer.slingshot.initialisedsimulation;

import java.nio.file.Path;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.core.Slingshot;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationDriver;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.AdditionalConfigurationModule;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.EventsToInitOnWrapper;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.InitStateDeSerialization;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.OtherStuffDeserialization;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.ResultStateSerialization;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.data.InitState;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser.data.OtherInitThings;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;
import org.palladiosimulator.analyzer.slingshot.stateexploration.data.ArchitectureConfigurationUtil;
import org.palladiosimulator.analyzer.slingshot.stateexploration.data.ExploredStateBuilder;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 * Core Component of the Exploration.
 *
 * Responsible for exploring new states.
 *
 * @author Sarah Stieß
 *
 */
public class SimulationStarter {

	private static final Logger LOGGER = Logger.getLogger(SimulationStarter.class.getName());
	
	private static final String resultFile = "state.json";
	private static final String snapshotFile = "snapshot.json";

	private final PCMResourceSetPartition initModels;
	
	final SimulationDriver driver;
	final ExploredStateBuilder stateBuilder;

	public SimulationStarter(final SimuComConfig config, final IProgressMonitor monitor, 
			final MDSDBlackboard blackboard, final Path initStateLocation, final Path otherLocation, final Path resultLocation) {
		super();
		this.initModels = (PCMResourceSetPartition) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);
		
	
		final InitState initstate = getInitState(initStateLocation);
		final OtherInitThings otherInitThings = getOthers(otherLocation);
		
		final URI resultFolder = URI.createFileURI(resultLocation.toString());
		ArchitectureConfigurationUtil.copyToURI(initModels.getResourceSet(), resultFolder);

		
		final EventsToInitOnWrapper wrapper = new Preprocessor(this.initModels, initstate.getSnapshot(), otherInitThings.getIncomingPolicies()).createWrapper();
		final SnapshotConfiguration snaphshotConfig = new SnapshotConfiguration(initstate.getPointInTime() > 0.0, otherInitThings.getSensibility(), config.getSimuTime());			
		this.stateBuilder  = new ExploredStateBuilder(initstate.getId(), initstate.getPointInTime(), this.initModels);
		
		AdditionalConfigurationModule.snapConfigProvider.set(snaphshotConfig);
		AdditionalConfigurationModule.eventsToInitOnProvider.set(wrapper);
		AdditionalConfigurationModule.defaultStateProvider.set(stateBuilder);
		
		this.driver = Slingshot.getInstance().getSimulationDriver();
		this.driver.init(config, monitor);
	}

	
	private InitState getInitState(final Path location) {
		final InitStateDeSerialization thing = new InitStateDeSerialization(this.initModels);
		return thing.deserialize(location);
	}
	
	private OtherInitThings getOthers(final Path location) {
		final OtherStuffDeserialization stuff = new OtherStuffDeserialization(this.initModels);
		return stuff.deserialize(location);
	}

	public void simulateSingleState(final Path location) {
		LOGGER.info("********** DefaultGraphExplorer.explore() **********");

		driver.start();
		UpdateSPDUtil.reduceTriggerTime(PCMResourcePartitionHelper.getSPD(initModels), stateBuilder.getDuration());
		
	
		final Path snapPath = Path.of(location.toString(), snapshotFile);
		(new InitStateDeSerialization(this.initModels)).serialize(stateBuilder.buildInitState(), snapPath);
		

		final Path resultPath = Path.of(location.toString(), resultFile);
		(new ResultStateSerialization()).serialize(stateBuilder.buildResultState(), resultPath);
	}
}
