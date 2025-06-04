package org.palladiosimulator.analyzer.slingshot.initialisedsimulation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorState;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.TargetGroupState;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.common.utils.ResourceUtils;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.InitWrapper;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;
import org.palladiosimulator.spd.constraints.policy.CooldownConstraint;
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
public class Preprocessor {

	private static final Logger LOGGER = Logger.getLogger(Preprocessor.class.getName());

	private final SPD spd;
	private final List<ScalingPolicy> policiesToProcess;
	private final Snapshot snapshot;
	
	public Preprocessor(final PCMResourceSetPartition partition, final Snapshot snapshot, final List<ScalingPolicy> policiesToProcess) {
		this.spd = PCMResourcePartitionHelper.getSPD(partition);
		this.snapshot = snapshot;
		this.policiesToProcess = policiesToProcess;
		this.deactivatePolicies();
	}

	/**
	 * TODO 
	 */
	private void deactivatePolicies() {
		this.deactivateReactivePolicies(this.spd, policiesToProcess);
		ResourceUtils.saveResource(this.spd.eResource());
	}
	
	/**
	 * 
	 * @return
	 */
	public InitWrapper createWrapper() {
		final Set<SPDAdjustorState> states = this.collectStates(snapshot);
		final List<ModelAdjustmentRequested> initialAdjustments = this.createInitialModelAdjustmentRequested();
		
		return new InitWrapper(initialAdjustments, states, snapshot.getEvents());
	}
	
	/**
	 * 
	 * @return
	 */
	private List<ModelAdjustmentRequested> createInitialModelAdjustmentRequested() {
		return policiesToProcess.stream().map(p -> new ModelAdjustmentRequested(p)).toList();
	}
	
	/**
	 * 
	 * @param snapshot
	 * @return
	 */
	private Set<SPDAdjustorState> collectStates(final Snapshot snapshot) {
			final Set<SPDAdjustorState> initValues = new HashSet<>();
		
			// process exisitng states
			for (final SPDAdjustorState oldState : snapshot.getSPDAdjustorStates()) {
				if(policiesToProcess.contains(oldState.getScalingPolicy())) {
					initValues.add(updateSPDStates(oldState));					
				} else {
					initValues.add(oldState);
				}
			}
			
			// add new states, if none exist.
			for (final ScalingPolicy policy : policiesToProcess) {
				if(snapshot.getSPDAdjustorStates().stream().filter(value -> value.getScalingPolicy().equals(policy)).findAny().isEmpty()) {
					initValues.add(updateSPDStates(new SPDAdjustorState(policy, new TargetGroupState(policy.getTargetGroup()))));
				}
			}
			return initValues;
		}

	/**
	 * Deactivate all reactively applied policies at the given SPD model, that have
	 * a simulation time based trigger.
	 * 
	 * Usually, it does not help to deactivate the policies directly via the
	 * reconfiguration, because the reconfiguration points to the wrong copy of the
	 * policies.
	 * 
	 * @param spd model to deactivate policies at.
	 * @param rea policies to deactivate
	 */
	private void deactivateReactivePolicies(final SPD spd, final List<ScalingPolicy> rea) {
		final List<String> ids = rea.stream().map(p -> p.getId()).toList();
		spd.getScalingPolicies().stream().filter(p -> isSimulationTimeTrigger(p.getScalingTrigger()))
				.filter(p -> ids.contains(p.getId())).forEach(p -> p.setActive(false));
	}
	
	
	/**
	 * 
	 * @param policy
	 * @param state
	 * @return
	 */
	private SPDAdjustorState updateSPDStates(final SPDAdjustorState state) {
		final ScalingPolicy policy = state.getScalingPolicy(); 
		
		final TargetGroupState tgs = state.getTargetGroupState();
		
		final Optional<CooldownConstraint> cooldownConstraint = policy.getPolicyConstraints().stream()
				.filter(CooldownConstraint.class::isInstance).map(CooldownConstraint.class::cast).findAny();
		
		if (cooldownConstraint.isPresent()) {
			if (state.getNumberOfScalesInCooldown() < cooldownConstraint.get().getMaxScalingOperations()) {
				state.incrementNumberOfAdjustmentsInCooldown();
			} else {
				state.setNumberOfScalesInCooldown(0);
				state.setCoolDownEnd(cooldownConstraint.get().getCooldownTime());
			}
		}

		state.setLatestAdjustmentAtSimulationTime(0.0);
		tgs.addEnactedPolicy(0.0, policy);
		
		return state;
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
	private boolean isSimulationTimeTrigger(final ScalingTrigger trigger) {
		return trigger instanceof final BaseTrigger base && base.getStimulus() instanceof SimulationTime
				&& base.getExpectedValue() instanceof ExpectedTime;
	}

}
