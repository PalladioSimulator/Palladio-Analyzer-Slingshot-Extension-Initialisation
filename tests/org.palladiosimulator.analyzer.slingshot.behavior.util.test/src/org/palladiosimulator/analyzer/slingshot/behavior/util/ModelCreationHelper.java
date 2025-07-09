package org.palladiosimulator.analyzer.slingshot.behavior.util;

import org.palladiosimulator.pcm.resourceenvironment.LinkingResource;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.pcm.resourceenvironment.ResourceEnvironment;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentFactory;
import org.palladiosimulator.pcm.resourceenvironment.ResourceenvironmentPackage;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;
import org.palladiosimulator.semanticspd.SemanticspdFactory;
import org.palladiosimulator.semanticspd.SemanticspdPackage;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.SpdFactory;
import org.palladiosimulator.spd.SpdPackage;
import org.palladiosimulator.spd.constraints.target.TargetFactory;
import org.palladiosimulator.spd.constraints.target.TargetGroupSizeConstraint;
import org.palladiosimulator.spd.constraints.target.TargetPackage;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.TargetGroup;
import org.palladiosimulator.spd.targets.TargetsFactory;
import org.palladiosimulator.spd.targets.TargetsPackage;

/**
 * 
 * Helper for creating different combinations of {@link SPD} and
 * {@link Configuration}.
 * 
 * Beware: the created models are partially invalid, as they only cover those
 * aspects required for executing {@link SPDHelperTest}.
 * 
 * @author Sophie Stieß
 *
 */
public class ModelCreationHelper {

	/**
	 * Create a new {@link ModelCreationHelper}.
	 * 
	 * Also loads all required packages. 
	 */
	public ModelCreationHelper() {
		final SpdPackage pack = SpdPackage.eINSTANCE;
		final TargetsPackage tpack = TargetsPackage.eINSTANCE;
		final TargetPackage tcpack = TargetPackage.eINSTANCE;
		final SemanticspdPackage semanticSpdPack = SemanticspdPackage.eINSTANCE;
		final ResourceenvironmentPackage resenvPack = ResourceenvironmentPackage.eINSTANCE;
	}

	
	// Creating all the needed factories
	final ResourceenvironmentFactory resenvFactory = ResourceenvironmentFactory.eINSTANCE;
	final SemanticspdFactory semanticSpdfactory = SemanticspdFactory.eINSTANCE;
	final TargetFactory tgconstraintFactory = TargetFactory.eINSTANCE;
	final TargetsFactory tgFactory = TargetsFactory.eINSTANCE;
	final SpdFactory spdFactory = SpdFactory.eINSTANCE;

	/** Pair for returing both values at once. */
	record TestInputPair(SPD spd, Configuration configuration) {
	}

	/**
	 * @return {@link Configuration} with an empty list of target group
	 *         configurations.
	 */
	public TestInputPair createEmptyConfiguration() {
		final ResourceEnvironment resenv = createResourceEnvironment(1);

		final SPD spd = createSPD(resenv);
		final Configuration configuration = createConfiguration(resenv, spd);

		configuration.getTargetCfgs().clear();

		return new TestInputPair(spd, configuration);
	}

	/**
	 * @return spd and semantic model with exactly one resource container.
	 */
	public TestInputPair createMinArchConfig() {
		final ResourceEnvironment resenv = createResourceEnvironment(1);

		final SPD spd = createSPD(resenv);
		final Configuration configuration = createConfiguration(resenv, spd);

		return new TestInputPair(spd, configuration);
	}

	/**
	 * @return spd and semantic model with three resource container and a constraint
	 *         for max 3 containers.
	 */
	public TestInputPair createMaxArchConfig() {
		final ResourceEnvironment resenv = createResourceEnvironment(3);

		final SPD spd = createSPD(resenv);
		final Configuration configuration = createConfiguration(resenv, spd);

		addConstraint(spd.getTargetGroups().get(0), 0, 3);

		return new TestInputPair(spd, configuration);
	}

	/**
	 * @return spd and semantic model with two resource container and a constraint
	 *         for max 3 containers.
	 */
	public TestInputPair createAverageArchConfig() {
		final ResourceEnvironment resenv = createResourceEnvironment(2);

		final SPD spd = createSPD(resenv);
		final Configuration configuration = createConfiguration(resenv, spd);

		addConstraint(spd.getTargetGroups().get(0), 0, 3);

		return new TestInputPair(spd, configuration);
	}

	/**
	 * 
	 * Add a {@link TargetGroupSizeConstraint} to the given target group.
	 * 
	 * @param tg  target group to add constraint to
	 * @param min lower limit for tg size.
	 * @param max upper limit for tg size.
	 * @return
	 */
	public TargetGroup addConstraint(final TargetGroup tg, final int min, final int max) {
		final TargetGroupSizeConstraint constraint = tgconstraintFactory.createTargetGroupSizeConstraint();

		constraint.setMaxSize(max);
		constraint.setMinSize(min);

		tg.getTargetConstraints().add(constraint);

		return tg;
	}

	/**
	 * 
	 * @param resenv resource environment with at least 1 container
	 * @return spd with a target group for the first resource container in the
	 *         environment.
	 */
	public SPD createSPD(final ResourceEnvironment resenv) {

		final ElasticInfrastructure ei = tgFactory.createElasticInfrastructure();
		ei.setUnit(resenv.getResourceContainer_ResourceEnvironment().get(0));

		final SPD spd = spdFactory.createSPD();
		spd.getTargetGroups().add(ei);

		return spd;
	}

	/**
	 * 
	 * @param resenv resource environment with at least 1 container and at least 1
	 *               linking resource
	 * @param spd    the spd for which semantic apply
	 * @return semantic model with a target group configuration for the first
	 *         resource container in the environment.
	 */
	public Configuration createConfiguration(final ResourceEnvironment resenv, final SPD spd) {
		final Configuration configuration = semanticSpdfactory.createConfiguration();

		configuration.setSpd(spd);

		final ElasticInfrastructureCfg eicfg = semanticSpdfactory.createElasticInfrastructureCfg();

		eicfg.setResourceEnvironment(resenv);
		eicfg.setLinkingResource(resenv.getLinkingResources__ResourceEnvironment().get(0));
		eicfg.setUnit(resenv.getResourceContainer_ResourceEnvironment().get(0));
		eicfg.getElements().addAll(resenv.getResourceContainer_ResourceEnvironment());

		configuration.getTargetCfgs().add(eicfg);

		return configuration;
	}

	/**
	 * 
	 * @param numberOfContainers number of containers, that will be placed in the
	 *                           resource environment. must be at least 1.
	 * @return resource environment with one linking resource and the given number
	 *         of containers, all of them connected to the single linkin resource.
	 */
	public ResourceEnvironment createResourceEnvironment(final int numberOfContainers) {
		if (numberOfContainers < 1) {
			throw new IllegalArgumentException(
					"number of resource containers must be at least 1, but is " + numberOfContainers + ".");
		}

		final ResourceEnvironment resenv = resenvFactory.createResourceEnvironment();

		final LinkingResource link = resenvFactory.createLinkingResource();
		resenv.getLinkingResources__ResourceEnvironment().add(link);
		for (int i = 0; i < numberOfContainers; i++) {
			final ResourceContainer container = resenvFactory.createResourceContainer();
			resenv.getResourceContainer_ResourceEnvironment().add(container);
			link.getConnectedResourceContainers_LinkingResource().add(container);
		}

		return resenv;
	}

}
