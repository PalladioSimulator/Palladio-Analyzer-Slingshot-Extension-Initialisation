package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.AbstractJobEvent;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobAborted;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobProgressed;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.SPDAdjustorState;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.TargetGroupState;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.ResourceDemandRequestAborted;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.ThinkTime;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.entities.User;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.ClosedWorkloadUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.InterArrivalUserInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserAborted;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.common.utils.LambdaVisitor;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemoryRecorder;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.RecordedJob;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotTaken;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.usagemodel.Start;

import de.uka.ipd.sdq.scheduler.resources.active.AbstractActiveResource;

/**
 * This is the camera for taking the snapshot.
 *
 * (my mental image is like this : through my camera's viewfinder i watch stuff
 * change and sometime i release the shutter and get a picture of how stuff
 * looks at a certain point in time.)
 *
 *
 * @author Sophie Stieß
 *
 */
public abstract class Camera {

	protected static final Logger LOGGER = Logger.getLogger(Camera.class);

	/** Beware: keep in sync with original */
	private static final String FAKE = "fakeID";

	/** Access to past events, that must go into the snapshot. */
	private final InMemoryRecorder record;

	/** Access to future events, that must go into the snapshot. */
	private final SimulationEngine engine;

	protected final List<DESEvent> additionalEvents = new ArrayList<>();
	private final Collection<SPDAdjustorState> states;

	private final LambdaVisitor<DESEvent, DESEvent> adjustOffset;

	/**
	 * 
	 * @param record
	 * @param engine
	 * @param states
	 */
	public Camera(final InMemoryRecorder record, final SimulationEngine engine,
			final Collection<SPDAdjustorState> states) {
		this.record = record;
		this.engine = engine;

		this.states = states;

		this.adjustOffset = new LambdaVisitor<DESEvent, DESEvent>()
				.on(UsageModelPassedElement.class).then(this::setOffset)
				.on(SEFFModelPassedElement.class).then(this::setOffset)
				.on(ClosedWorkloadUserInitiated.class).then(this::reduceThinktime)
				.on(InterArrivalUserInitiated.class).then(this::reduceDelay)
				.on(DESEvent.class).then(e -> e);
	}

	/**
	 * ..and this is like releasing the shutter.
	 */
	public abstract Snapshot takeSnapshot();

	/**
	 * include some more events.
	 * 
	 * actually only used for model adjustment requested events.
	 */
	public void addEvent(final DESEvent event) {
		additionalEvents.add(event);
	}

	/**
	 * Adjust the times in all {@link SPDAdjustorState}s.
	 * 
	 * The times are adjusted with the current simulation time.
	 * 
	 * @return collection of state values with updated time
	 */
	protected Collection<SPDAdjustorState> snapStates() {
		return this.states.stream()
				.map(s -> this.copyAndOffset(s, engine.getSimulationInformation().currentSimulationTime())).toList();
	}

	/**
	 * Adjust the time of the latest adjustment and the time of the cooldown to the
	 * reference time. Also creates a copy.
	 *
	 * If the latest adjustment was at t = 5 s, the cooldown ends at t = 15 s, and
	 * the reference time is t = 10 s, then the adjusted values will be latest
	 * adjustment at t = -5 s and cooldown end at t = 5 s.
	 *
	 * @param states        values to be adjusted
	 * @param referenceTime time to adjust to.
	 * @return copy of state values with adjusted values.
	 */
	private SPDAdjustorState copyAndOffset(final SPDAdjustorState states, final double referenceTime) {
		final TargetGroupState tgs = states.getTargetGroupState();
		tgs.addEnactedPolicy(tgs.getLastScalingPolicyEnactmentTime() - referenceTime,
				tgs.getLastEnactedScalingPolicy());

		states.setLatestAdjustmentAtSimulationTime(states.getLatestAdjustmentAtSimulationTime() - referenceTime);

		final double coolDownEnd = states.getCoolDownEnd() > 0.0 ? states.getCoolDownEnd() - referenceTime : 0.0;
		states.setCoolDownEnd(coolDownEnd);

		return states;
	}

	/**
	 *
	 * Get {@link ModelAdjustmentRequested} events, that happened at the point in
	 * time the snapshot was taken, but did not trigger it.
	 *
	 * As there is no guarantee on the order of events, that happen at the same
	 * point in time, the {@link ModelAdjustmentRequested} events are either
	 * directly scheduled, or already wrapped into {@link SnapshotInitiated} or
	 * {@link SnapshotTaken} events.
	 *
	 * @return upcoming {@link ModelAdjustmentRequested} events.
	 */
	protected List<ModelAdjustmentRequested> getScheduledReconfigurations() {
		final List<ModelAdjustmentRequested> events = new ArrayList<>();

		/* Scheduled ModelAdjustmentRequested */
		engine.getScheduledEvents().stream().filter(ModelAdjustmentRequested.class::isInstance)
				.map(ModelAdjustmentRequested.class::cast).forEach(events::add);

		/* ModelAdjustmentRequested already processed into SnapshotInitiated events */
		engine.getScheduledEvents().stream().filter(SnapshotInitiated.class::isInstance)
				.map(SnapshotInitiated.class::cast).filter(e -> e.getTriggeringEvent().isPresent())
				.forEach(e -> events.add(e.getTriggeringEvent().get()));

		/* ModelAdjustmentRequested already processed into SnapshotTaken events */
		engine.getScheduledEvents().stream().filter(SnapshotTaken.class::isInstance).map(SnapshotTaken.class::cast)
				.filter(e -> e.getTriggeringEvent().isPresent()).forEach(e -> events.add(e.getTriggeringEvent().get()));

		return events;
	}

	/**
	 * Denormalizes the demand of the open jobs and creates {@link JobInitiated}
	 * events to reinsert them to their respective Processor Sharing Resource.
	 *
	 * C.f. {@link SerializingCamera#createInitEventsForFCFS(Set, Set)} for details
	 * on the demand denormalized.
	 * 
	 * @param jobrecords
	 * @return
	 */
	protected Set<JobInitiated> createInitEventsForProcSharing(final Set<RecordedJob> jobrecords) {
		final Set<JobInitiated> rval = new HashSet<>();

		for (final RecordedJob jobRecord : jobrecords) {
			// do the Proc Sharing Math
			final double ratio = jobRecord.getNormalizedDemand() == 0 ? 0
					: jobRecord.getCurrentDemand() / jobRecord.getNormalizedDemand();
			final double reducedRequested = jobRecord.getRequestedDemand() * ratio;
			jobRecord.getJob().updateDemand(reducedRequested);
			rval.add(new JobInitiated(jobRecord.getJob()));

		}
		return rval;
	}

	/**
	 * Denormalizes the demand of the open jobs and creates {@link JobInitiated}
	 * events to reinsert them to their respective FCFS Resource.
	 *
	 * The demand must be denormalized, because upon receiving a
	 * {@link JobInitiated} event, the {@link AbstractActiveResource} normalizes a
	 * job's demand with the resource's processing rate. Thus without
	 * denormalisation, the demand would be wrong.
	 *
	 * This is required for ActiveJobs, and for LinkingJobs. In case of LinkingJobs,
	 * the throughput is used as processing rate.
	 *
	 * @param jobrecords     jobs waiting at an FCFS resource at the time of the
	 *                       snapshot
	 * @param fcfsProgressed events scheduled for simulation at the time of the
	 *                       snapshot
	 * @return events to reinsert all open jobs to their respective FCFS Resource
	 */
	protected Set<JobInitiated> createInitEventsForFCFS(final Set<RecordedJob> jobrecords,
			final Set<AbstractJobEvent> fcfsProgressed) {
		final Set<JobInitiated> rval = new HashSet<>();

		final Map<Job, AbstractJobEvent> progressedJobs = new HashMap<>();
		fcfsProgressed.stream().forEach(event -> progressedJobs.put(event.getEntity(), event));

		for (final RecordedJob record : jobrecords) {
			if (record.getNormalizedDemand() == 0) { // For Linking Jobs.
				if (record.getJob().getDemand() != 0) {
					throw new IllegalStateException(
							String.format("Job %s of Type %s: Normalized demand is 0, but acutal demand is not.",
									record.getJob().toString(), record.getJob().getClass().getSimpleName()));
				}
			} else if (progressedJobs.keySet().contains(record.getJob())) {
				final AbstractJobEvent event = progressedJobs.get(record.getJob());
				// time equals remaining demand because of normalization.
				final double remainingDemand = event.time() - engine.getSimulationInformation().currentSimulationTime();
				final double factor = record.getRequestedDemand() / record.getNormalizedDemand();
				final double denormalizedRemainingDemand = remainingDemand * factor;
				record.getJob().updateDemand(denormalizedRemainingDemand);
			} else {
				record.getJob().updateDemand(record.getRequestedDemand());
			}
			rval.add(new JobInitiated(record.getJob()));
		}
		return rval;
	}

	/**
	 * print information about given set of events.
	 *
	 * @param evt
	 */
	protected void log(final Set<DESEvent> evt) {
		LOGGER.info("DEMANDS");
		evt.stream().filter(e -> (e instanceof JobInitiated)).map(e -> (JobInitiated) e)
				.forEach(e -> LOGGER.info(e.getEntity().getDemand()));
		LOGGER.info("TIMES");
		evt.stream().filter(e -> (e instanceof UsageModelPassedElement<?>)).map(e -> (UsageModelPassedElement<?>) e)
				.forEach(e -> LOGGER.info(e.time()));
		LOGGER.info("CWUI");
		evt.stream().filter(e -> (e instanceof ClosedWorkloadUserInitiated)).map(e -> (ClosedWorkloadUserInitiated) e)
				.forEach(e -> LOGGER.info(e.delay() + " " + e.time()));
	}

	protected DESEvent setOffset(final UsageModelPassedElement<?> event) {
		final double simulationTime = engine.getSimulationInformation().currentSimulationTime();
		if (event.getModelElement() instanceof Start && event.time() <= simulationTime) {
			setOffset(event.time(), simulationTime, event);

		}
		return event;
	}

	protected DESEvent setOffset(final SEFFModelPassedElement<?> event) {
		final double simulationTime = engine.getSimulationInformation().currentSimulationTime();
		if (event.getModelElement() instanceof StartAction && event.time() <= simulationTime) {
			setOffset(event.time(), simulationTime, event);
		}
		return event;
	}

	/**
	 * Create a copy of the given event with reduced remaining think time / delay.
	 * 
	 * Must create a copy, because think time get directly transformed to delay, and
	 * delay is immutable.
	 * 
	 * @param event thinktime (delay) of this event will be reduced
	 * @return copy of the given event with reduced remaining thinktime.
	 */
	protected DESEvent reduceThinktime(final ClosedWorkloadUserInitiated event) {
		final double simulationTime = engine.getSimulationInformation().currentSimulationTime();

		final double remainingthinktime = event.time() - simulationTime;

		final CoreFactory coreFactory = CoreFactory.eINSTANCE;
		final PCMRandomVariable var = coreFactory.createPCMRandomVariable();
		var.setSpecification(String.valueOf(remainingthinktime));

		final ThinkTime newThinktime = new ThinkTime(var);
		return new ClosedWorkloadUserInitiated(event.getEntity(), newThinktime);
	}

	/**
	 * Create a copy of the given event with reduced delay.
	 * 
	 * Must create a copy, because the delay is immutable.
	 * 
	 * @param event delay of this event will be reduced
	 * @return copy of the given event with reduced delay
	 */
	protected DESEvent reduceDelay(final InterArrivalUserInitiated event) {
		final double simulationTime = engine.getSimulationInformation().currentSimulationTime();
		return new InterArrivalUserInitiated(event.getEntity(), event.time() - simulationTime);
	}

	/**
	 * Calculate an event's offset with respect to the current simulation time and
	 * save the offset in the event's time attribute.
	 *
	 * If the event was created during the simulation run, the offset is the
	 * difference between current simulation time and the event time. If the event
	 * was created during a previous simulation run, i.e. it already entered this
	 * simulation run with a offset into the past, the new offset is the sum of
	 * simulation time and old offset.
	 *
	 * @param eventTime      publication point in time from previous simulation run
	 * @param simulationTime current time of the simulation
	 * @param event          the event to be modified
	 */
	private void setOffset(final double eventTime, final double simulationTime, final ModelPassedEvent<?> event) {
		if (eventTime < 0) {
			final double offset = -(eventTime - simulationTime);
			event.setTime(offset);
		} else {
			final double offset = simulationTime - eventTime;
			event.setTime(offset);
		}
	}

	/**
	 * Collect all events for reinitialising the simulator.
	 * 
	 * @return Set off all events for reinitialising the simulator.
	 */
	protected Set<DESEvent> collectAndOffsetEvents() {
		final Set<DESEvent> relevantEvents = engine.getScheduledEvents();

		this.handleFELAbortions(relevantEvents);
		relevantEvents.removeIf(e -> this.isAbortion(e));

		// get events to recreate state of queues
		final Set<RecordedJob> fcfsRecords = record.getFCFSJobRecords();
		final Set<RecordedJob> procsharingRecords = record.getProcSharingJobRecords();

		final Set<AbstractJobEvent> progressedFcfs = relevantEvents.stream()
				.filter(e -> (e instanceof JobProgressed) || (e instanceof JobFinished)).map(e -> (AbstractJobEvent) e)
				.collect(Collectors.toSet());

		relevantEvents.addAll(this.createInitEventsForFCFS(fcfsRecords, progressedFcfs));
		relevantEvents.addAll(this.createInitEventsForProcSharing(procsharingRecords));

		relevantEvents.addAll(record.getRecordedCalculators());

		relevantEvents.removeIf(e -> this.isFake(e));

		final Set<DESEvent> offsettedEvents = relevantEvents.stream().map(adjustOffset).collect(Collectors.toSet());
		return offsettedEvents;
	}

	/**
	 * Call the abortion handling of the recorder for all abortion events from the
	 * FEL.
	 * 
	 * This is necessary, because the simulation engine (may) deliver events that
	 * happen at the same point in time in arbitrary order, thus if an abortion
	 * event is published at the same point in time as the {@link SnapshotTaken}
	 * event, we must explicitly remove the aborted jobs and open calculators from
	 * the record.
	 * 
	 * Otherwise, we include superfluous content in the snapshot, that might remain
	 * there forever, as we do not include abortion events in the snapshot, or even
	 * cause problems on (de-)serialiazation because the event's entities refer to
	 * missing model elements.
	 * 
	 * Note: [S3] i did not manage to create this scenario manually, thus this is
	 * untested.
	 * 
	 * @param felEvents events from the FEL
	 */
	private void handleFELAbortions(final Set<DESEvent> felEvents) {
		final List<JobAborted> abortions = felEvents.stream().filter(e -> (e instanceof JobAborted) && !this.isFake(e)).map(e -> (JobAborted) e).toList();
		
		abortions.stream().forEach(e -> record.removeJobRecord(e));

		final List<User> users = abortions.stream().filter(e -> e.getEntity() instanceof ActiveJob).map(e -> (ActiveJob) e.getEntity()).map(e -> e.getRequest().getSeffInterpretationContext().getRequestProcessingContext().getUser()).toList();
		users.stream().forEach(e -> record.removeOpenCalculators(e));
		
		felEvents.stream().filter(e -> (e instanceof UserAborted) && !this.isFake(e)).map(e -> (UserAborted) e)
				.forEach(e -> record.removeOpenCalculators(e.getEntity().getUser()));
		
		felEvents.stream().filter(e -> (e instanceof ResourceDemandRequestAborted) && !this.isFake(e)).map(e -> (ResourceDemandRequestAborted) e)
				.forEach(e -> record.removeOpenCalculators(e.getEntity().getUser()));
	}

	/**
	 * Check whether the given event was injected to trigger a state update only.
	 * 
	 * @param event
	 * @return true, if the event was injected for triggering a state update, false
	 *         if it occured "naturally"
	 */
	private boolean isFake(final DESEvent event) {
		if (event instanceof final AbstractJobEvent jobEvent) {
			return jobEvent.getEntity().getId().equals(FAKE);
		} else {
			return false;
		}
	}
	
	/**
	 * Check whether the given event is part of the abortion handling.
	 * 
	 * As of now, {@link JobAborted}, {@link ResourceDemandRequestAborted} and {@link UserAborted} are events for abortion handling. 
	 * 
	 * @param event
	 * @return true, if the evens is any event of the abortion handling.
	 */
	private boolean isAbortion(final DESEvent event) {
		return JobAborted.class.isInstance(event) || ResourceDemandRequestAborted.class.isInstance(event) ||  UserAborted.class.isInstance(event);
		
	}
}
