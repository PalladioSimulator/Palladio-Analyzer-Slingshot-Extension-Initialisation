package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.configuration.ExplorationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.configuration.InitialiseSimulationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.jobs.InitialiseSimulationRootJob;
import org.palladiosimulator.experimentautomation.application.ExperimentApplication;
import org.palladiosimulator.experimentautomation.application.tooladapter.abstractsimulation.AbstractSimulationConfigFactory;
import org.palladiosimulator.experimentautomation.application.tooladapter.slingshot.model.SlingshotConfiguration;
import org.palladiosimulator.experimentautomation.experiments.Experiment;
import org.palladiosimulator.experimentautomation.experiments.ExperimentRepository;
import org.palladiosimulator.experimentautomation.experiments.ExperimentsPackage;
import org.palladiosimulator.experimentautomation.experiments.InitialModel;

import de.uka.ipd.sdq.simucomframework.SimuComConfig;
import de.uka.ipd.sdq.workflow.BlackboardBasedWorkflow;
import de.uka.ipd.sdq.workflow.WorkflowFailedException;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

/**
 *
 * Application for running a single headles state simulation. Requires an
 * {@link Experiment} model instance, that defines the models et cetera.
 *
 * The path to the {@link Experiment} model instance must be provided as
 * commandline argument.
 *
 * For OSGi runs inside Eclipse, supply the path a additional argument in the
 * field "Program arguments".
 *
 * Based on {@link ExperimentApplication}.
 *
 * @author Sophie Stieß
 *
 */
public class InitialisedSimulationApplication implements IApplication {

	private final String SINGLE_STATE_SIMULATION_ID = "org.palladiosimulator.initialisedsimulation";

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		final Path experimentsLocation = parseCommandlineArguments(context, 1);


		final Experiment experiment = getExperiment(experimentsLocation).orElseThrow(() -> new IllegalArgumentException(
				"No Experiment with tool configuration of type SlingshotConfiguration. Cannot start simulation."));
		
		final InitialiseSimulationWorkflowConfiguration.LocationRecord locationRecord=  createLocationRecord(context);

		initialiseAndLaunchSimulation(experiment, locationRecord);
		
		return IApplication.EXIT_OK;
	}

	/**
	 * Get command line arguments and parse them.
	 *
	 * @param context to parse the arguments from. Must have at least one argument.
	 * @return first command line argument as Path.
	 */
	private Path parseCommandlineArguments(final IApplicationContext context, final int index) {
		final String[] args = (String[]) context.getArguments().get("application.args");

		if (args.length < index - 1) {
			throw new IllegalArgumentException("Less than" + index + "arguments given.");
		}

		return new Path(args[index - 1]);
	}
	
	private InitialiseSimulationWorkflowConfiguration.LocationRecord createLocationRecord(final IApplicationContext context) {
		final java.nio.file.Path snapshotFile = parseCommandlineArguments(context, 2).toFile().toPath();
		final java.nio.file.Path otherConfigsFile = parseCommandlineArguments(context, 3).toFile().toPath();
		final java.nio.file.Path resultFolder = parseCommandlineArguments(context, 4).toFile().toPath();
		
		return new InitialiseSimulationWorkflowConfiguration.LocationRecord(snapshotFile, otherConfigsFile, resultFolder);
	}

	/**
	 * Get an experiment with a {@link SlingshotConfiguration}.
	 *
	 * @param modelLocation path the the *.experiments file
	 * @return first experiment with a {@link SlingshotConfiguration} or
	 *         {@link Optional#empty()} if none exists.
	 */
	private Optional<Experiment> getExperiment(final IPath modelLocation) {

		final List<Experiment> experiments = loadExperimentsFromFile(modelLocation);

		return experiments.stream().filter(e -> e.getToolConfiguration().stream()
				.filter(SlingshotConfiguration.class::isInstance).findFirst().isPresent()).findFirst();
	}

	/**
	 *
	 * Create and execute a workflow for preparing and running a state exploration.
	 *
	 * @param experiment
	 */
	private void initialiseAndLaunchSimulation(final Experiment experiment, final InitialiseSimulationWorkflowConfiguration.LocationRecord locationRecord) {

		final Map<String, Object> configMap = createConfigMap(experiment, SINGLE_STATE_SIMULATION_ID);

		final SimuComConfig simuComconfig = new SimuComConfig(configMap, false);

		final InitialiseSimulationWorkflowConfiguration config = new InitialiseSimulationWorkflowConfiguration(simuComconfig, configMap, locationRecord);

		this.setModelFilesInConfig(experiment, config);

		final BlackboardBasedWorkflow<MDSDBlackboard> workflow = new BlackboardBasedWorkflow<MDSDBlackboard>(
				new InitialiseSimulationRootJob(config, null),
				new MDSDBlackboard());

		try {
			workflow.execute(new NullProgressMonitor());
		} catch (JobFailedException | UserCanceledException e) {
			throw new WorkflowFailedException("Workflow failed", e);
		}
	}

	/**
	 * Get the file location of the initial models, and put the into the
	 * {@code config}.
	 *
	 * This is necessary, to use the {@link ExplorationRootJob}, which load the
	 * model as defined in the {@link ExplorationWorkflowConfiguration}.
	 *
	 * @param models initial models, as defined in the experiments file.
	 * @param config configuration to start the exploration on.
	 */
	private void setModelFilesInConfig(final Experiment experiment, final ExplorationWorkflowConfiguration config) {
		final InitialModel models = experiment.getInitialModel();
		
		this.consumeModelLocation(models.getAllocation(), s -> config.setAllocationFiles(List.of(s)));
		this.consumeModelLocation(models.getUsageModel(), s -> config.setUsageModelFile(s));

		this.consumeModelLocation(models.getScalingDefinitions(), s -> config.addOtherModelFile(s));
		this.consumeModelLocation(models.getSpdSemanticConfiguration(), s -> config.addOtherModelFile(s));
		this.consumeModelLocation(models.getMonitorRepository(), s -> config.addOtherModelFile(s));
		this.consumeModelLocation(models.getServiceLevelObjectives(), s -> config.addOtherModelFile(s));
		this.consumeModelLocation(models.getUsageEvolution(), s -> config.addOtherModelFile(s));
		
		this.consumeModelLocation(experiment, s -> config.addOtherModelFile(s));
	}

	/**
	 *
	 * Make {@code consumer} accept the URI of {@code model}, if it is not
	 * {@code null}.
	 *
	 * If {@code model} is {@code null}, nothing happens.
	 *
	 * @param model    model who's URI will be consumed. May be {@code null}.
	 * @param consumer consumer to consume the model's URI.
	 */
	private void consumeModelLocation(final EObject model, final Consumer<String> consumer) {
		assert consumer != null;
		if (model == null) {
			return;
		}
		consumer.accept(model.eResource().getURI().toString());
	}

	@Override
	public void stop() {
		// Add operations when your plugin is stopped
	}

	/**
	 * Create map with configuration for the {@link SimuComConfig}.
	 *
	 * Uses the factory from the experiment automation and adds the exploration
	 * specific configurations afterwards.
	 *
	 * Beware: For some reason, all values but booleans are expected to be of type
	 * {@link String}.
	 *
	 * @param experiment  input for creating the configuration map
	 * @param simulatorID id of the simulator
	 * @return configurations to create the {@link SimuComConfig}.
	 */
	public Map<String, Object> createConfigMap(final Experiment experiment, final String simulatorID) {

		final SlingshotConfiguration simConfig =
				(SlingshotConfiguration) experiment.getToolConfiguration().stream()
				.filter(SlingshotConfiguration.class::isInstance).findFirst().get();

		final Map<String, Object> map = AbstractSimulationConfigFactory.createConfigMap(experiment, simConfig,
				simulatorID,
				List.of());
		
		return map;
	}

	/**
	 * Loads an experiments model from the given location.
	 *
	 * @param modelLocation path the the *.experiments file
	 * @return list of experiments
	 */
	private static List<Experiment> loadExperimentsFromFile(final IPath modelLocation) {
		System.out.println("Loading resource " + modelLocation.toString() + " from bundle");
		final URI modelUri = URI.createFileURI(modelLocation.toOSString());
		final Resource r = (new ResourceSetImpl()).getResource(modelUri, true);

		final EObject o = r.getContents().get(0);
		if (ExperimentsPackage.eINSTANCE.getExperimentRepository().isInstance(o)) {
			return ((ExperimentRepository) o).getExperiments();
		} else {
			throw new RuntimeException("The root element of the loaded resource is not of the expected type "
					+ ExperimentsPackage.eINSTANCE.getExperimentRepository().getName());
		}
	}
}