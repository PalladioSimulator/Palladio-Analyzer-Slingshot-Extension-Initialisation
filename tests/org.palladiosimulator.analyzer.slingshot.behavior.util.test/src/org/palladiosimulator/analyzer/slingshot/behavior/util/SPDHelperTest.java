package org.palladiosimulator.analyzer.slingshot.behavior.util;



import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.palladiosimulator.analyzer.slingshot.behavior.util.ModelCreationHelper.TestInputPair;
import org.palladiosimulator.analyzer.slingshot.common.utils.SPDHelper;
import org.palladiosimulator.pcm.resourceenvironment.ResourceContainer;
import org.palladiosimulator.semanticspd.TargetGroupCfg;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.TargetGroup;

/**
 * 
 * Simple Test cases for some operations of {@link SPDHelper}.
 * 
 * @author Sophie Stieß
 *
 */
public class SPDHelperTest {

	private final ModelCreationHelper helper = new ModelCreationHelper();
	
	/**
	 * Test correct behaviour for deciding on min/max, if the architecture in neither maximal nor minimal.
	 */
	@Test
	public void testAverageArchConfig() {
		final TestInputPair pair = helper.createAverageArchConfig();
		
		assertFalse(SPDHelper.isMinArchitecture(pair.configuration(), pair.spd()));
		assertFalse(SPDHelper.isMaxArchitecture(pair.configuration(), pair.spd()));
		
	}
	
	/**
	 * Test correct behaviour for deciding on min/max, if the architecture in maximal.
	 */
	@Test
	public void testMaxArchConfig() {
		final TestInputPair pair = helper.createMaxArchConfig();
		
		assertFalse(SPDHelper.isMinArchitecture(pair.configuration(), pair.spd()));
		assertTrue(SPDHelper.isMaxArchitecture(pair.configuration(), pair.spd()));
		
	}
	
	/**
	 * Test correct behaviour for deciding on min/max, if the architecture in minimal.
	 */
	@Test
	public void testMinArchConfig() {
		final TestInputPair pair = helper.createMinArchConfig();
		
		assertTrue(SPDHelper.isMinArchitecture(pair.configuration(), pair.spd()));
		assertFalse(SPDHelper.isMaxArchitecture(pair.configuration(), pair.spd()));
		
	}
	
	/**
	 * Test correct behaviour for deciding on min/max, if there are no target group configurations.
	 */
	@Test
	public void testEmptyConfiguration() {
		final TestInputPair pair = helper.createEmptyConfiguration();
		
		assertFalse(SPDHelper.isMinArchitecture(pair.configuration(), pair.spd()));
		assertFalse(SPDHelper.isMaxArchitecture(pair.configuration(), pair.spd()));
		
	}
	
	/**
	 * Test correct behaviour in case of target groups and their configuration do match.
	 */
	@Test
	public void testMatching() {
		final TestInputPair pair = helper.createMinArchConfig(); // min arch has only one container
	
		final TargetGroup tg =  pair.spd().getTargetGroups().get(0);
		final TargetGroupCfg tgcfg = pair.configuration().getTargetCfgs().get(0);
		
		assertEquals(tgcfg, SPDHelper.getMatchingTargetGroupCfg(tg, pair.configuration()));
		assertTrue(SPDHelper.getMatchingTargetGroup(tgcfg, pair.spd()).isPresent());
		assertEquals(tg, SPDHelper.getMatchingTargetGroup(tgcfg, pair.spd()).get());
	}
	
	/**
	 * Test correct behaviour in case of broken references between semantic model and spd model.
	 */
	@Test
	public void testMatchingExceptionsOnWrongModelLink() {
		final TestInputPair pair = helper.createMinArchConfig();
		pair.configuration().setSpd(null);
	
		final TargetGroup tg =  pair.spd().getTargetGroups().get(0);
		final TargetGroupCfg tgcfg = pair.configuration().getTargetCfgs().get(0);
		
		assertThrows(IllegalArgumentException.class, () -> SPDHelper.getMatchingTargetGroupCfg(tg, pair.configuration()));
		assertThrows(IllegalArgumentException.class, () -> SPDHelper.getMatchingTargetGroup(tgcfg, pair.spd()));
		
		pair.configuration().setSpd(helper.createMinArchConfig().spd());
		
		assertThrows(IllegalArgumentException.class, () -> SPDHelper.getMatchingTargetGroupCfg(tg, pair.configuration()));
		assertThrows(IllegalArgumentException.class, () -> SPDHelper.getMatchingTargetGroup(tgcfg, pair.spd()));
	}
	
	/**
	 * Test correct behaviour in case target groups and their configuration do not match.
	 */
	@Test
	public void testNoMatching() {
		final TestInputPair pair = helper.createMinArchConfig();
		final ResourceContainer rc = helper.createResourceEnvironment(1).getResourceContainer_ResourceEnvironment().get(0);
		
		final TargetGroup tg =  pair.spd().getTargetGroups().get(0);
        ((ElasticInfrastructure) tg).setUnit(rc);		
		final TargetGroupCfg tgcfg = pair.configuration().getTargetCfgs().get(0);

		
		assertThrows(IllegalArgumentException.class, () -> SPDHelper.getMatchingTargetGroupCfg(tg, pair.configuration()));
		assertTrue(SPDHelper.getMatchingTargetGroup(tgcfg, pair.spd()).isEmpty());
	}

}
