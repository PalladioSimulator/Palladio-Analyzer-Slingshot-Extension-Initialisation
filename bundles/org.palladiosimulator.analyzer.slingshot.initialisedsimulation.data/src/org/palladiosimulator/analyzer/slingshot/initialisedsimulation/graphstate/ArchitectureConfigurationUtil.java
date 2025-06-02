package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.graphstate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.experimentautomation.experiments.ExperimentsPackage;
import org.palladiosimulator.mdsdprofiles.api.ProfileAPI;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.allocation.AllocationPackage;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentFactory;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentPackage;
import org.palladiosimulator.pcm.system.SystemPackage;
import org.palladiosimulator.pcm.usagemodel.UsagemodelPackage;
import org.palladiosimulator.pcm.util.PcmResourceImpl;
import org.palladiosimulator.semanticspd.SemanticspdPackage;
import org.palladiosimulator.servicelevelobjective.ServicelevelObjectivePackage;
import org.palladiosimulator.spd.SpdPackage;
import org.scaledl.usageevolution.UsageevolutionPackage;

import tools.descartes.dlim.DlimPackage;

/**
 * 
 * Util class for creating a copy of a set of models.
 *
 * @author Sophie Stieß
 *
 */
public class ArchitectureConfigurationUtil {

	private static final Logger LOGGER = Logger.getLogger(ArchitectureConfigurationUtil.class.getName());

	/**
	 * EClasses of all models that must be provided to simulate with Slingshot.
	 */
	public static final Set<EClass> MANDATORY_MODEL_ECLASS = Set.of(RepositoryPackage.eINSTANCE.getRepository(),
			AllocationPackage.eINSTANCE.getAllocation(), UsagemodelPackage.eINSTANCE.getUsageModel(),
			SystemPackage.eINSTANCE.getSystem(), ResourceenvironmentPackage.eINSTANCE.getResourceEnvironment());

	/**
	 * EClasses of all models that are simulated by Slingshot, if provided.
	 */
	public static final Set<EClass> OPTIONAL_MODEL_ECLASSES = Set.of(
			MonitorRepositoryPackage.eINSTANCE.getMonitorRepository(),
			MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository(),
			SpdPackage.eINSTANCE.getSPD(),
			SemanticspdPackage.eINSTANCE.getConfiguration(),
			ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository(),
			UsageevolutionPackage.eINSTANCE.getUsageEvolution(),
			ExperimentsPackage.eINSTANCE.getExperimentRepository(),
			DlimPackage.eINSTANCE.getSequence());

	private static final Set<EClass> MERGED_MODEL_ECLASSES = new HashSet<>();
	static {
		MERGED_MODEL_ECLASSES.addAll(MANDATORY_MODEL_ECLASS);
		MERGED_MODEL_ECLASSES.addAll(OPTIONAL_MODEL_ECLASSES);
	}

	/**
	 * EClasses of all models that maybe persisted as part of the
	 * {@link ArchitectureConfiguration}
	 */
	public static final Set<EClass> MODEL_ECLASS_WHITELIST = Set.copyOf(MERGED_MODEL_ECLASSES);

	/**
	 * Get all {@link Resource}s from the given {@link ResourceSet} that contain a
	 * whitelisted model.
	 *
	 * @param set
	 * @return resources with whitelisted models.
	 */
	public static List<Resource> getWhitelistedResources(final ResourceSet set) {
		return set.getResources().stream().filter(r -> isWhitelisted(r)).toList();
	}

	/**
	 * Checks whether a given resource is whitelisted.
	 *
	 * A resource is considered whitelisted if its URI is either platform resource
	 * or file URI (pathmap excluded) and if it contains at least one model of a
	 * whitelisted type.
	 *
	 * @param resource to be checked, must not be null.
	 * @return true if the resource is whitelisted, false otherwise.
	 */
	public static boolean isWhitelisted(final Resource resource) {
		assert resource.getURI() != null : "Cannot check resource, URI is null";
		return (resource.getURI().isFile() || resource.getURI().isPlatformResource()) && resource.getContents().stream()
				.filter(o -> MODEL_ECLASS_WHITELIST.stream().anyMatch(allowedEClass -> allowedEClass == o.eClass()))
				.findAny().isPresent();
	}

	/**
	 * Save all whitelisted resources in the given set to their respective URI.
	 *
	 * @param set
	 */
	public static void saveWhitelisted(final ResourceSet set) {
		final List<Resource> whitelisted = getWhitelistedResources(set);

		for (final Resource resource : whitelisted) {
			ResourceUtils.saveResource(resource);
			LOGGER.debug(String.format("Saved resource %s.", resource.getURI().toString()));
		}
	}
	
	/**
	 * Changes als the location of all models in the given set to the given
	 * destination Folder and writes them to file.
	 * 
	 * First, this operation resolves all references within the models and ensures
	 * that cost-stereotyes are loaded before the copy. However, this operation
	 * cannot resolve external references to model element, e.g. inside the
	 * snapshot. It is upon the caller to enure, that those are resolved upfront.
	 * 
	 * Then this operation updates the URIs of all models in the given set and
	 * creates the copy by saving them to file. The URI is not reset to the original
	 * value and remains changed.
	 * 
	 * @param set               set containing the models
	 * @param destinationFolder folder to copy the models to.
	 */
	public static void copyToURI(final ResourceSet set, final URI destinationFolder) {
		String cleanLocation = destinationFolder.toString();

		if (destinationFolder.hasTrailingPathSeparator()) {
			cleanLocation = cleanLocation.substring(0, cleanLocation.length() - 1);
		}

		applyStereotypeFake();
		EcoreUtil.resolveAll(set);

		final List<Resource> whitelisted = ArchitectureConfigurationUtil.getWhitelistedResources(set);

		for (final Resource resource : whitelisted) {
			final String file = resource.getURI().lastSegment();

			final URI newUri = URI.createURI(cleanLocation).appendSegment(file);
			resource.setURI(newUri);
		}

		ArchitectureConfigurationUtil.saveWhitelisted(set);
	}
	
	/**
	 * Applying a Profile and and the Stereotypes is necessary, because otherwise
	 * {@link EcoreUtil#resolveAll(org.eclipse.emf.ecore.resource.ResourceSet)}
	 * fails to resolve stereotype applications for cost.
	 * 
	 * This behaviour can also be observed in the PalladioBench UI. When opening a
	 * resource environment model with stereotypes and profile, we get an exception
	 * (PackageNotFound). If we create a new resource environment model, apply
	 * profiles and stereotypes to the new model, and only open the actual resource
	 * environment model afterwards, it opens just fine.
	 * 
	 * Thus, basically, this operation simulates what i have to do in the
	 * PalladioBench UI. I assume, that they fucked up the loading of profile
	 * models, but somehow the get loaded upon calling
	 * {@link ProfileAPI#applyProfile(Resource, String)} and
	 * {@link StereotypeAPI#applyStereotype(org.eclipse.emf.ecore.EObject, String)}.
	 * 
	 * <br>
	 * <b>Note</b>: Re-applying Profiles to the original model (which works in the
	 * UI) fails here, because re-application throws an error.
	 * 
	 * <br>
	 * <b>Note</b>: The resource and model created in this operation exist solely
	 * for getting the cost profile and stereotypes loaded. They have no other
	 * purpose and (probably) get garbage collect after this operation.
	 * 
	 * <br>
	 * <b>Note</b>: For the state exploration, we must do this <b>before</b> we
	 * create the root node. When creating the root node, we save a copy of the
	 * models to file, and all unresolved references go missing on save.
	 */
	private static void applyStereotypeFake() {
		final ResourceEnvironment fake = ResourceenvironmentFactory.eINSTANCE.createResourceEnvironment();
		final Resource fakeRes = new PcmResourceImpl(URI.createFileURI(java.lang.System.getProperty("java.io.tmpdir")));
		fakeRes.getContents().add(fake);
		ProfileAPI.applyProfile(fakeRes, "Cost");
		StereotypeAPI.applyStereotype(fake, "CostReport");
	}
}