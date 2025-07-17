package org.palladiosimulator.analyzer.slingshot.common.utils;

import java.util.List;
import java.util.Optional;

import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.semanticspd.CompetingConsumersGroupCfg;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.ElasticInfrastructureCfg;
import org.palladiosimulator.semanticspd.ServiceGroupCfg;
import org.palladiosimulator.semanticspd.TargetGroupCfg;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.constraints.target.TargetGroupSizeConstraint;
import org.palladiosimulator.spd.targets.CompetingConsumersGroup;
import org.palladiosimulator.spd.targets.ElasticInfrastructure;
import org.palladiosimulator.spd.targets.ServiceGroup;
import org.palladiosimulator.spd.targets.TargetGroup;

/**
 * 
 * With the current (July 2025) versions of {@link SPD} and
 * {@link Configuration}, the {@link TargetGroup} of the former and the
 * {@link TargetGroupCfg} of the latter have no direct connection. Instead they
 * must be associated by checking their units. If they reference the same unit,
 * the belong together.
 * <br>
 * As the code for this matching is required in multiple places, we extracted it as helpers into this very util class. <br>
 * <br>
 * In addition, neither {@link TargetGroup} nor {@link TargetGroupCfg} provide generic access to the units.
 * Accessing the unit requires casting to the actual subtypes.
 * This is cumbersome.
 * To avoid repeated code for typechecking and casting, extracted the code for accessing the units as helpers into this very util class as well.<br>
 * 
 * @author Sophie Stieß
 *
 */
public final class SPDHelper {

	/**
	 * 
	 * Check whether the architecture is at its minimum configuration.
	 * 
	 * An architecture is considered minimal with respect to a set of scaling
	 * policies, if none of the policies can further reduce the number of elements
	 * in the architecture.
	 * 
	 * If the configuration has no target group configurations it is NOT considered minimal.
	 * 
	 * @param config sematic configuration for the given spd.
	 * @param spd set of scaling policies for determining minimalism.
	 * @return true iff the architecture is minimal.
	 */
	public static boolean isMinArchitecture(final Configuration config, final SPD spd) {		
		boolean isMin = !config.getTargetCfgs().isEmpty();
	
		for (final TargetGroupCfg tgcfg : config.getTargetCfgs()) {
			final Optional<TargetGroup> match = getMatchingTargetGroup(tgcfg, spd);
			if (match.isPresent()) {
				isMin &= SPDHelper.isMinTargetGroup(tgcfg, getTargetGroupSizeConstraint(match.get()));
			} else {
				isMin &= SPDHelper.isMinTargetGroup(tgcfg, Optional.empty());				
			}
		} 
		return isMin;
	}
	
	/**
	 * Check whether the architecture is at its maximum configuration.
	 * 
	 * An architecture is considered maximal with respect to a set of scaling
	 * policies, if none of the policies can further increase the number of elements
	 * in the architecture.
	 * 
	 *  If the configuration has no target group configurations it is considered maximal.
	 *  
	 * 
	 * @param config sematic configuration for the given spd.
	 * @param spd set of scaling policies for determining maximalism.
	 * @return true iff the architecture is maximal.
	 */
	public static boolean isMaxArchitecture(final Configuration config, final SPD spd) {
		boolean isMax = !config.getTargetCfgs().isEmpty();
		
		for (final TargetGroupCfg tgcfg : config.getTargetCfgs()) {
			final Optional<TargetGroup> match = getMatchingTargetGroup(tgcfg, spd);
			if (match.isPresent()) {
				isMax &= SPDHelper.isMaxTargetGroup(tgcfg, getTargetGroupSizeConstraint(match.get()));
			} else {
				isMax &= SPDHelper.isMaxTargetGroup(tgcfg, Optional.empty());				
			}
		} 
		return isMax;
	}
	
	/**
	 * Check whether the given target group configuration is minimal.
	 * 
	 * A target group is considered minimal, if the number of elements in the target group cannot be reduced further.
	 * This happens either because there is only one element (the unit) left, or because of a {@link TargetGroupSizeConstraint}.
	 * 
	 * @param tgcfg to be checked for minimalism.
	 * @param constraint {@link TargetGroupSizeConstraint}, if defined.
	 * @return true if the given TargetGroupCfg is minimal.
	 */
	public static boolean isMinTargetGroup(final TargetGroupCfg tgcfg, final Optional<TargetGroupSizeConstraint> constraint) {
		if (constraint.isEmpty()) {
			return getSizeOf(tgcfg) == 1;			
		}
		return getSizeOf(tgcfg) <= constraint.get().getMinSize();
	}
	
	/**
	 * Check whether the given target group configuration is maximal.
	 * 
	 * A target group is considered maximal, if the number of elements in the target group cannot be increased further.
	 * This might happen because of a {@link TargetGroupSizeConstraint}.
	 * 
	 * @param tgcfg to be checked for maximalism.
	 * @param constraint {@link TargetGroupSizeConstraint}, if defined.
	 * @return true if the given TargetGroupCfg is maximal.
	 */
	public static boolean isMaxTargetGroup(final TargetGroupCfg tgcfg, final Optional<TargetGroupSizeConstraint> constraint) {
		if (constraint.isEmpty()) {
			return false;			
		}
		return getSizeOf(tgcfg) >= constraint.get().getMaxSize();
	}
	
	/**
	 * Get a {@link TargetGroup} from the given {@link SPD} that references the same unit as the given {@link TargetGroupCfg}.
	 * 
	 * Requires, that the target group configuration's semantic model belong to the given {@link SPD}, throws an {@link IllegalArgumentException} otherwise.
	 * 
	 * @param tgcfg semantic configuration 
	 * @param spd spd to extract the matching target group from 
	 * @return target group with the same unit as the given target group configuration, or an empty optional, if no match exists.
	 */
	public static Optional<TargetGroup> getMatchingTargetGroup(final TargetGroupCfg tgcfg, final SPD spd) {
		if (!spd.equals(((Configuration) tgcfg.eContainer()).getSpd())) {
			throw new IllegalArgumentException("The given target group configuration does not belong to the given spd.");
		}
		
		final Entity unit = getUnitOf(tgcfg);
		
		return spd.getTargetGroups().stream().filter(tg -> getUnitOf(tg).equals(unit)).findAny();
	}
	
	/**	
	 * 
	 * Get a {@link TargetGroupCfg} from the given {@link Configuration} that references the same unit as the given {@link TargetGroup}.
	 * 
	 * Requires, that the semantic configuration model belong to the given target group's {@link SPD}, throws an {@link IllegalArgumentException} otherwise.
	 * 
	 * Throws an {@link IllegalArgumentException} if no match exists.
	 * 
	 * @param tg target group
	 * @param config semantic configuration to extract the matching target group configuration from 
	 * @return target group configuration with the same unit as the given target group.
	 */
	public static TargetGroupCfg getMatchingTargetGroupCfg(final TargetGroup tg, final Configuration config) {
		if (!((SPD) tg.eContainer()).equals(config.getSpd())) {
			throw new IllegalArgumentException("The given target group does not belong to the given semantic configuration.");
		}
		
		final Entity unit = getUnitOf(tg);
		
		return config.getTargetCfgs().stream().filter(tgcfg -> getUnitOf(tgcfg).equals(unit)).findAny()
				.orElseThrow(() -> new IllegalArgumentException(String.format(
						"No matching TargetGroupCfg for TargetGroup %s[%s] in %s. This is a violation on the model requirements. Every TargetGroupCfg needs a corresponding TargetGroup.",
						tg.getEntityName(), tg.getId(), config.toString())));
		}
	
	/**
	 * 
	 * @param tg a target group
	 * @return the {@link TargetGroupSizeConstraint} of the target group, or an empty optional if there is none.
	 */
	public static Optional<TargetGroupSizeConstraint> getTargetGroupSizeConstraint(final TargetGroup tg) {
		return tg.getTargetConstraints().stream().filter(TargetGroupSizeConstraint.class::isInstance).map(TargetGroupSizeConstraint.class::cast).findAny();
	}
	
	/**
	 * Helper for accessing the size of a {@link TargetGroupCfg} (the thing from the semantic SPD).
	 * 
	 * @param tgcfg the target group configuration to be accessed
	 * @return size of the target group configuration
	 */
	public static int getSizeOf(final TargetGroupCfg tgcfg) {
		return getElementsOf(tgcfg).size();
	}
	

	/**
	 * Helper for accessing the unit of a {@link TargetGroupCfg} (the thing from the semantic SPD).
	 * 
	 * Unit is returned as {@link Entity}, because that is the smalles common supertype of the different target group configurations' units.
	 * 
	 * @param tgcfg the target group configuration to be accessed
	 * @return unit of the given target group configuration
	 */
	public static Entity getUnitOf(final TargetGroupCfg tgcfg) {
		if (tgcfg instanceof final ElasticInfrastructureCfg ecfg) {
			return ecfg.getUnit();
		} else if (tgcfg instanceof final ServiceGroupCfg scfg) {
			return scfg.getUnit();
		} else if (tgcfg instanceof final CompetingConsumersGroupCfg ccfg) {
			return ccfg.getUnit();
		} else {
			throw new IllegalArgumentException(
					"TargetGroupConfiguration of unknown type, cannot access unit.");
		}
	}
	
	/**
	 * Helper for accessing the elements of a {@link TargetGroupCfg} (the thing from the semantic SPD).
	 * 
	 * Elements are returned as {@link Entity}, because that is the smalles common supertype of the different target group configurations' elements.
	 * 
	 * @param tgcfg the target group configuration to be accessed
	 * @return elements of the given target group configuration
	 */
	public static List<Entity> getElementsOf(final TargetGroupCfg tgcfg) {
		if (tgcfg instanceof final ElasticInfrastructureCfg ecfg) {
			return ecfg.getElements().stream().map(e -> (Entity) e).toList();
		} else if (tgcfg instanceof final ServiceGroupCfg scfg) {
			return scfg.getElements().stream().map(e -> (Entity) e).toList();
		} else if (tgcfg instanceof final CompetingConsumersGroupCfg ccfg) {
			return ccfg.getElements().stream().map(e -> (Entity) e).toList();
		} else {
			throw new IllegalArgumentException(
					"TargetGroupConfiguration of unknown type, cannot access elements.");
		}
	}

	/**
	 * Helper for accessing the unit of a {@link TargetGroup} (the thing from the normal SPD).
	 * 
	 * Unit is returned as {@link Entity}, because that is the smalles common supertype of the different target groups' units.
	 * 
	 * @param tg the target group to be accessed.
	 * @return unit of the given target group
	 */
	public static Entity getUnitOf(final TargetGroup tg) {
		if (tg instanceof final ElasticInfrastructure etg) {
			return etg.getUnit();
		} else if (tg instanceof final ServiceGroup stg) {
			return stg.getUnitAssembly();
		} else if (tg instanceof final CompetingConsumersGroup ctg) {
			return ctg.getUnitAssembly();
		} else {
			throw new IllegalArgumentException("TargetGroup of unknown type, cannot access unit.");
		}
	}
}
