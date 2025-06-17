package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.application;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.configuration.InitialiseSimulationWorkflowConfiguration;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.configuration.Locations;
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
		final Arguments arguments = newParseCLI(context);

		final Experiment experiment = getExperiment(arguments.experimentPath)
				.orElseThrow(() -> new IllegalArgumentException(
						"No Experiment with tool configuration of type SlingshotConfiguration. Cannot start simulation."));

		initialiseAndLaunchSimulation(experiment, arguments);

		return IApplication.EXIT_OK;
	}

	private Arguments newParseCLI(final IApplicationContext context) {
		final String[] args = (String[]) context.getArguments().get("application.args");

		final Arguments arguments = new Arguments(Arrays.asList(args));

		return arguments;
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
	 * Create and execute a workflow for preparing and running an initialised simulation.
	 *
	 * @param experiment information about the simulation to run
	 * @param args already parsed application arguments
	 */
	private void initialiseAndLaunchSimulation(final Experiment experiment, final Arguments args) {

		final Map<String, Object> configMap = createConfigMap(experiment, SINGLE_STATE_SIMULATION_ID);

		final SimuComConfig simuComconfig = new SimuComConfig(configMap, false);
		
		final Locations locations = new Locations(args.snapshotFile, args.otherConfigsFile, args.resultFolder, args.snapshotOutputFile, args.stateOutputFile);

		final InitialiseSimulationWorkflowConfiguration config = new InitialiseSimulationWorkflowConfiguration(simuComconfig, locations, args.id);

		this.setModelFilesInConfig(experiment, config);

		final BlackboardBasedWorkflow<MDSDBlackboard> workflow = new BlackboardBasedWorkflow<MDSDBlackboard>(
				new InitialiseSimulationRootJob(config, null), new MDSDBlackboard());

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
	 * @param config configuration to start the simulation on.
	 */
	private void setModelFilesInConfig(final Experiment experiment, final InitialiseSimulationWorkflowConfiguration config) {
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

		final SlingshotConfiguration simConfig = (SlingshotConfiguration) experiment.getToolConfiguration().stream()
				.filter(SlingshotConfiguration.class::isInstance).findFirst().get();

		final Map<String, Object> map = AbstractSimulationConfigFactory.createConfigMap(experiment, simConfig,
				simulatorID, List.of());

		return map;
	}

	/**
	 * Loads an experiments model from the given location.
	 *
	 * @param modelLocation path the the *.experiments file
	 * @return list of experiments
	 */
	private static List<Experiment> loadExperimentsFromFile(final IPath modelLocation) {
		final URI modelUri = URI.createFileURI(modelLocation.toOSString());

		Resource r;
		try {
			r = (new ResourceSetImpl()).getResource(modelUri, true);
		} catch (final Exception e) {
			throw new IllegalArgumentException("Could not load Resource for experiments-file at \""
					+ modelLocation.toString() + "\". Did you specify the correct file?", e);
		}

		if (r.getContents().isEmpty()) {
			throw new IllegalStateException(
					"Contents of Loaded Resource are empty. This should not have happend and i have now clue how this was even possible.");
		}

		final EObject o = r.getContents().get(0);
		if (ExperimentsPackage.eINSTANCE.getExperimentRepository().isInstance(o)) {
			return ((ExperimentRepository) o).getExperiments();
		} else {
			throw new IllegalArgumentException("The root element of the resource at the spedified loaction \""
					+ modelLocation.toString() + "\" is not of the expected type "
					+ ExperimentsPackage.eINSTANCE.getExperimentRepository().getName() + ". Instead it is of type "
					+ o.getClass().getName() + ". Did you specify the correct file?");
		}
	}

	private class Arguments {
		/**
		 * 
		 * @param args
		 */
		public Arguments(final List<String> args) {

			final Map<String, String> mappedArgs = createArgumentsMap(args);
			final List<File> files = parseInputFiles(mappedArgs);

			this.id = mappedArgs.containsKey(ID) ? mappedArgs.get(ID) : UUID.randomUUID().toString();

			this.otherConfigsFile = parseSingleArg(mappedArgs, files, CONFIG, CONFIG_SFX).toPath();
			this.snapshotFile = parseSingleArg(mappedArgs, files, SNAPSHOT, SNAPSHOT_SFX).toPath();
			this.experimentPath = new org.eclipse.core.runtime.Path(parseSingleArg(mappedArgs, files, EXPERIMENTS, EXPERIMENTS_SFX).toString());

			this.resultFolder = parseOutputFolder(mappedArgs).toPath();
			
			this.snapshotOutputFile = parseSnapshotOutputArg(mappedArgs);
			this.stateOutputFile = parseStateOutputArg(mappedArgs);
		}

		/**
		 * 
		 * @param mappedArgs
		 * @return
		 */
		private File parseOutputFolder(final Map<String, String> mappedArgs) {
			if (mappedArgs.containsKey(OUTPUT)) {
				final File file = new File(mappedArgs.get(OUTPUT));
				if (file.isAbsolute()) {
					if (file.exists() && !file.isDirectory()) {
						throw new IllegalArgumentException(
								"The path of the output folder must be point to a directory. However, the given path \""
										+ file.toString() + "\" points to an existing file, that is not a directory.");
					}
					return file;
				} else {
					throw new IllegalArgumentException(
							"The path to the output folder must be absolute. However, the given path is \""
									+ file.toString() + "\".");
				}
			} else {
				throw new IllegalArgumentException("No output folder specified. Use the option " + OUTPUT
						+ " to specify a the absolute path of the folder for saving the output files.");
			}
		}

		/**
		 * 
		 * @param mappedArgs
		 * @return
		 */
		private List<File> parseInputFiles(final Map<String, String> mappedArgs) {
			final List<File> files = new ArrayList<>();

			if (mappedArgs.containsKey(INPUT)) {
				final File input = new File(mappedArgs.get(INPUT));
				if (input.exists() && input.isDirectory()) {
					final List<File> folderContent = Arrays.asList(input.listFiles());

					folderContent.stream().filter(f -> f.getAbsolutePath().endsWith(SNAPSHOT_SFX))
							.forEach(f -> files.add(f));
					folderContent.stream().filter(f -> f.getAbsolutePath().endsWith(EXPERIMENTS_SFX))
							.forEach(f -> files.add(f));
					folderContent.stream().filter(f -> f.getAbsolutePath().endsWith(CONFIG_SFX))
							.forEach(f -> files.add(f));
				} else {
					throw new IllegalArgumentException(
							"The input path must point no an existing directory, but the given path \""
									+ input.toString() + "\" does not.");
				}
			}
			return files;
		}

		/**
		 * Parse the arguments list to a map of option-value pairs.
		 * 
		 * Options and values are identified by index. All list entries with even index
		 * are considered options specifiers and all list entries with uneven index are
		 * considere values. The list must adhere to this format. 
		 * 
		 * @param args lsit of arguments.
		 * @return map of option-value pairs
		 * @throws IllegalArgumentException if any option of the afore mentioned
		 *                                  preconditions is violated.
		 */
		private Map<String, String> createArgumentsMap(final List<String> args) {
			final Predicate<String> isValidThing = (final String s) -> s.equals(ID) || s.equals(OUTPUT)
					|| s.equals(INPUT) || s.equals(SNAPSHOT) || s.equals(EXPERIMENTS) || s.equals(CONFIG) || s.equals(SNAPSHOT_OUTPUT) || s.equals(STATE_OUTPUT);

			final Map<String, String> mappedArgs = new HashMap<>();
			for (int i = 0; i < args.size(); i = i + 2) {
				if (isValidThing.test(args.get(i))) {
					if (!(i + 1 < args.size()) || isValidThing.test(args.get(i + 1))) {
						throw new IllegalArgumentException("Missing options value for " + args.get(i));
					} else if (mappedArgs.containsKey(args.get(i))) {
						throw new IllegalArgumentException("Each option must be used at most once, but " + args.get(i)
								+ " is used multiple times.");
					} else {
						mappedArgs.put(args.get(i), args.get(i + 1));
					}
				} else {
					throw new IllegalArgumentException("Expected options specifier, but found " + args.get(i));
				}
			}
			return mappedArgs;
		}

		/**
		 * 
		 * @param mappedArgs
		 * @param files
		 * @param option
		 * @param sfx
		 * @return
		 */
		private File parseSingleArg(final Map<String, String> mappedArgs, final List<File> files, final String option,
				final String sfx) {
			if (mappedArgs.containsKey(option)) {
				final File file = new File(mappedArgs.get(option));
				if (file.isAbsolute()) {
					if (file.exists() && file.isFile()) {
						return file;
					} else {
						throw new IllegalArgumentException(
								"When using the " + option + " option, the given value must point to an existing " + sfx
										+ "-file. However, the given value is \"" + file.toString() + "\", which "
										+ (file.exists() ? "is not a file." : "does not exist."));
					}
				} else {
					throw new IllegalArgumentException("When using the " + option + " option, the path to the " + sfx
							+ "-file must be absolute. However, the given path is \"" + file.toString() + "\".");
				}
			} else if(!mappedArgs.containsKey(INPUT)) {
				throw new IllegalArgumentException("Missing" + sfx +"-file. Either use the " + option + " option to specify the absolute path of the file to be used, or use the " + INPUT+ " option to specify an input folder that contains a " + sfx + "-file.");
			} else {
				final List<File> configs = files.stream().filter(f -> f.getAbsolutePath().endsWith(sfx)).toList();
				if (configs.size() == 1) {
					return configs.get(0);
				} else {
					throw new IllegalArgumentException("Input folder contains " + configs.size() + " " + sfx
							+ "-files. Either put exactly one " + sfx + "-file in the input folder, or use the "
							+ option + " option to specify the absolute path of the file to be used.");
				}
			}
		}
		
		/**
		 * 
		 * @param mappedArgs
		 * @param output
		 * @param option
		 * @return
		 */
		private Path parseSnapshotOutputArg(final Map<String, String> mappedArgs) {
			if (mappedArgs.containsKey(SNAPSHOT_OUTPUT)) {
				return fromMapped(mappedArgs.get(SNAPSHOT_OUTPUT));
			} else {
				final Path resolved = resultFolder.resolve(this.snapshotFile.getFileName());
				return resolved;
			}
		}
		
		private Path parseStateOutputArg(final Map<String, String> mappedArgs) {
			if (mappedArgs.containsKey(STATE_OUTPUT)) {
				return fromMapped(mappedArgs.get(STATE_OUTPUT));
			} else {
				return resultFolder.resolve(this.id + STATE_SFX);
			}
		}

		private Path fromMapped(final String location) {
			final Path path = new File(location).toPath();
			if (path.isAbsolute()) {
				return path.toFile().toPath();
			} else {
				return resultFolder.resolve(location);
			}
		}

		private final Path snapshotFile;
		private final Path otherConfigsFile;
		private final Path resultFolder;
		private final org.eclipse.core.runtime.Path experimentPath;
		private final String id;
		
		private final Path snapshotOutputFile;
		private final Path stateOutputFile;

		private static final String ID = "-id";

		private static final String OUTPUT = "-output";
		private static final String INPUT = "-input";

		private static final String SNAPSHOT = "-snapshot";
		private static final String EXPERIMENTS = "-experiments";
		private static final String CONFIG = "-config";
		
		private static final String SNAPSHOT_OUTPUT = "-outputSnapshot";
		private static final String STATE_OUTPUT = "-outputState";

		private static final String SNAPSHOT_SFX = ".snapshot";
		private static final String EXPERIMENTS_SFX = ".experiments";
		private static final String CONFIG_SFX = ".config";
		private static final String STATE_SFX = ".state";
	}
}