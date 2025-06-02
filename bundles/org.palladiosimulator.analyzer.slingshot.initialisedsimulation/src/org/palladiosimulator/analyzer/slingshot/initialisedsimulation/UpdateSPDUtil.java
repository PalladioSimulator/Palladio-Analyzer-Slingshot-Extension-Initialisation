package org.palladiosimulator.analyzer.slingshot.initialisedsimulation;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.triggers.BaseTrigger;
import org.palladiosimulator.spd.triggers.ScalingTrigger;
import org.palladiosimulator.spd.triggers.expectations.ExpectedTime;
import org.palladiosimulator.spd.triggers.stimuli.SimulationTime;

/**
 *
 * The preprocessor creates the {@link SimulationInitConfiguration} for the next
 * simulation run.
 * 
 * The order of simulation of the planned transitions is as defined by the
 * fringe. The preprocessor does not change the order. However, it can drop a
 * planned transition, e.g. because it is now in the past compared to the time
 * in the managed system.
 *
 * @author Sophie Stieß
 *
 */
public class UpdateSPDUtil {

	private static final Logger LOGGER = Logger.getLogger(UpdateSPDUtil.class.getName());

	/**
	 *
	 * Ensures, that the state graph node, that will be simulated with the resulting configuration is already connected to the state graph. 
	 * Ensures, that the at max one planned change is removed from the fringe.
	 * 
	 * Ensures, that the SPD model of the new state graph node is updated (e.g. reduced triggertimes for simulation time base triggers, and that the changes are saved to file as well. 
	 * 
	 * Ensures, that the {@link ModelAdjustmentRequested events} get copied and point to the corrcet (?) SPD file.
	 *
	 * @return Configuration for the next simulation run, or empty optional, if
	 *         fringe has no viable change.
	 */
	public static void reduceTriggerTime(final SPD spd, final double duration) {
		updateSimulationTimeTriggeredPolicy(spd, duration);
		ResourceUtils.saveResource(spd.eResource());
	}

	/**
	 * Reduces the {@link ExpectedTime} value for scaling policies with trigger
	 * stimulus {@link SimulationTime} or deactivates the policy if the trigger is
	 * in the past with regard to global time.
	 *
	 * The {@link ExpectedTime} value is reduced by the duration of the previous
	 * state.
	 *
	 * @param spd    current scaling rules.
	 * @param offset duration of the previous state
	 */
	private static void updateSimulationTimeTriggeredPolicy(final SPD spd, final double offset) {
		spd.getScalingPolicies().stream()
				.filter(policy -> policy.isActive() && isSimulationTimeTrigger(policy.getScalingTrigger()))
				.map(policy -> ((BaseTrigger) policy.getScalingTrigger()))
				.forEach(trigger -> updateValue(((ExpectedTime) trigger.getExpectedValue()), offset));
	}

	/**
	 * Update expected time value and deactivate, if the policy is in the past
	 * necessary.
	 * 
	 * @param time                  model element to be updated
	 * @param previousStateDuration duration to subtract from {@code time}.
	 */
	private static  void updateValue(final ExpectedTime time, final double previousStateDuration) {
		final double triggerTime = time.getValue();

		final ScalingPolicy policy = (ScalingPolicy) time.eContainer().eContainer();

		if (triggerTime < previousStateDuration) {
			policy.setActive(false);
			LOGGER.debug(String.format("Deactivate Policy %s as Triggertime is in the past.", policy.getEntityName()));
		} else {
			time.setValue(time.getValue() - previousStateDuration);
			LOGGER.debug(String.format("Reduce Triggertime of Policy %s by %f to %f.", policy.getEntityName(),
					previousStateDuration, time.getValue()));
		}
	}

	/**
	 * Check whether the given trigger is based on {@link SimulationTime} and
	 * {@link ExpectedTime}.
	 * 
	 * TODO take compound triggers into consideration.
	 * 
	 * @param trigger trigger to be checked.
	 * @return true iff the trigger is based on {@link SimulationTime} and
	 *         {@link ExpectedTime}
	 */
	private static boolean isSimulationTimeTrigger(final ScalingTrigger trigger) {
		return trigger instanceof final BaseTrigger base && base.getStimulus() instanceof SimulationTime
				&& base.getExpectedValue() instanceof ExpectedTime;
	}
}
