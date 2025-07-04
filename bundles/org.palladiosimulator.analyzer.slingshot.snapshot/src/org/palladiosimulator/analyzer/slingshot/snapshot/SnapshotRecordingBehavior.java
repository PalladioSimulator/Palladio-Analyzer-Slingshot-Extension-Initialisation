package org.palladiosimulator.analyzer.slingshot.snapshot;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.ActiveJob;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.entities.jobs.Job;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobAborted;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobFinished;
import org.palladiosimulator.analyzer.slingshot.behavior.resourcesimulation.events.JobInitiated;
import org.palladiosimulator.analyzer.slingshot.behavior.spd.data.ModelAdjustmentRequested;
import org.palladiosimulator.analyzer.slingshot.behavior.systemsimulation.events.SEFFModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UsageModelPassedElement;
import org.palladiosimulator.analyzer.slingshot.behavior.usagemodel.events.UserAborted;
import org.palladiosimulator.analyzer.slingshot.common.utils.events.ModelPassedEvent;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationEngine;
import org.palladiosimulator.analyzer.slingshot.core.api.SimulationScheduling;
import org.palladiosimulator.analyzer.slingshot.core.extension.PCMResourceSetPartitionProvider;
import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PostIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.PreIntercept;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.Subscribe;
import org.palladiosimulator.analyzer.slingshot.eventdriver.annotations.eventcontract.OnEvent;
import org.palladiosimulator.analyzer.slingshot.eventdriver.entity.interceptors.InterceptorInformation;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.InterceptionResult;
import org.palladiosimulator.analyzer.slingshot.eventdriver.returntypes.Result;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.InitWrapper;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Camera;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.EventRecorder;
import org.palladiosimulator.analyzer.slingshot.snapshot.api.Snapshot;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.InMemoryRecorder;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.PlainSnapshotCamera;
import org.palladiosimulator.analyzer.slingshot.snapshot.entities.RecordedJob;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotFinished;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotInitiated;
import org.palladiosimulator.analyzer.slingshot.snapshot.events.SnapshotTaken;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.allocation.AllocationContext;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;


/**
 *
 * Behaviour responsible for creating a {@link Snapshot}.
 * 
 * This includes keeping track of and recording information, that must be
 * included in the snapshot but cannot be accessed directly once taking a
 * snapshot is triggered. This behaviour subscribes to, or pre- and postintercepts the
 * recorded events, but the actual recording is forwarded to a instance of
 * {@link EventRecorder}.
 * 
 * For taking the actual snapshot, this class uses an instance of {@link Camera}.
 *
 * @author Sophie Stieß
 *
 */
@OnEvent(when = ModelPassedEvent.class, then = {})
@OnEvent(when = JobFinished.class, then = {})
@OnEvent(when = JobAborted.class, then = {})
@OnEvent(when = UserAborted.class, then = {})
@OnEvent(when = SnapshotTaken.class, then = SnapshotFinished.class)
@OnEvent(when = SnapshotInitiated.class, then = SnapshotTaken.class)
public class SnapshotRecordingBehavior implements SimulationBehaviorExtension {
	private static final Logger LOGGER = Logger.getLogger(SnapshotRecordingBehavior.class);
	private static final String FAKE = "fakeID";

	/* flags to prevent duplicate snapshots */
	private boolean snapshotIsTaken = false;
	private boolean snapshotIsFinished = false;

	private final InMemoryRecorder recorder;
	
	private final Camera camera;

	private final SimulationScheduling scheduling;

	@Inject
	public SnapshotRecordingBehavior(final SimulationEngine engine, final Allocation allocation, final SimulationScheduling scheduling,
			final PCMResourceSetPartitionProvider set, final InitWrapper wrapper) {
		// can i somehow include this in the injection part?
		// should work with this Model and the 'bind' instruction.

		this.recorder = new InMemoryRecorder();
		this.camera = new PlainSnapshotCamera(this.recorder, engine, wrapper.getStates());
		
		this.scheduling = scheduling;
	}

	@Subscribe(reified = Start.class)
	public void onUsageScenarioStarted(final UsageModelPassedElement<Start> event) {
		this.recorder.addInitiatedCalculator(event);

	}

	@Subscribe(reified = Stop.class)
	public void onUsageScenarioStoped(final UsageModelPassedElement<Stop> event) {
		this.recorder.removeFinishedCalculator(event);
	}

	@Subscribe(reified = StartAction.class)
	public void onSEFFStarted(final SEFFModelPassedElement<StartAction> event) {
		this.recorder.addInitiatedCalculator(event);

	}

	@Subscribe(reified = StopAction.class)
	public void onSEFFStoped(final SEFFModelPassedElement<StopAction> event) {
		this.recorder.removeFinishedCalculator(event);
	}

	@Subscribe
	public void removeJobRecord(final JobFinished event) {
		this.recorder.removeJobRecord(event);
	}
	
	@Subscribe
	public void removeJobRecord(final JobAborted event) {
		this.recorder.removeJobRecord(event);
	}

	@Subscribe
	public void removeAbortedCalculators(final UserAborted event) {
		this.recorder.removeOpenCalculators(event.getEntity().getUser());
	}
	
	/**
	 * Create a {@link RecordedJob} before the {@link JobInitiated} get processed to capture the initial
	 * demand.
	 *
	 * @see {@link SnapshotRecordingBehavior#postInterceptJobInitiated(InterceptorInformation, JobInitiated, Result)s}
	 * @param information
	 * @param event
	 * @return
	 */
	@PreIntercept
	public InterceptionResult preInterceptJobInitiated(final InterceptorInformation information,
			final JobInitiated event) {
		if (!event.getEntity().getId().equals(FAKE)) {
			recorder.createJobRecord(event);
		}
		return InterceptionResult.success();
	}

	/**
	 * Update JobRecord after the {@link JobInitiated} got processed, to capture the
	 * normalized demand. This is necessary because the demand is normalized with the resources processing rate, when entering the resource, and we need both demands.
	 *
	 * @see {@link SnapshotRecordingBehavior#preInterceptJobInitiated(InterceptorInformation, JobInitiated)}
	 * @param information
	 * @param event
	 * @param result
	 * @return
	 */
	@PostIntercept
	public InterceptionResult postInterceptJobInitiated(final InterceptorInformation information,
			final JobInitiated event, final Result<?> result) {
		if (!event.getEntity().getId().equals(FAKE)) {
			recorder.updateJobRecord(event);
		}
		return InterceptionResult.success();
	}
	
	/**
	 * When recreating the simulator state in {@link }
	 * @param information
	 * @param event
	 * @return
	 */
	@PreIntercept
	public InterceptionResult preInterceptJobAborted(final InterceptorInformation information, final JobAborted event) {
		if (event.getEntity().getId().equals(FAKE)) {
			return InterceptionResult.abort();
		}
		return InterceptionResult.success();
	}

	/**
	 * The actual snapping.
	 *
	 * @param snapshotTaken event to signify that every things is ready for the snapshot to be taken.
	 * @return event to signify that the snapshot has been taken.
	 */
	@Subscribe
	public Result<SnapshotFinished> onSnapshotTakenEvent(final SnapshotTaken snapshotTaken) {

		if (this.snapshotIsFinished) {
			return Result.of();
		}
		this.snapshotIsFinished = true;

		if (snapshotTaken.getTriggeringEvent().isPresent()) {
			final ModelAdjustmentRequested triggeringeEvent = snapshotTaken.getTriggeringEvent().get();
			camera.addEvent(triggeringeEvent);
		}
		
		final Snapshot snapshot = camera.takeSnapshot();

		return Result.of(new SnapshotFinished(snapshot));
	}

	/**
	 *
	 * Trigger updates to all processor sharing resource states.
	 *
	 * @param snapshotInitiated
	 * @return
	 */
	@Subscribe
	public Result<SnapshotTaken> onSnapshotInitiatedEvent(final SnapshotInitiated snapshotInitiated) {

		if (this.snapshotIsTaken) {
			return Result.of(new SnapshotTaken(0, snapshotInitiated.getTriggeringEvent()));
		}

		this.snapshotIsTaken = true;

		// Cast to ActiveJob is feasible, because LinkingJobs are always FCFS.
		this.scheduleProcSharingUpdatesHelper(recorder.getProcSharingJobRecords().stream()
				.map(record -> (ActiveJob) record.getJob()).collect(Collectors.toSet()));

		return Result.of(new SnapshotTaken(0, snapshotInitiated.getTriggeringEvent()));
	}

	/**
	 * Schedule exactly one fake {@link JobInitiated} to each
	 * {@link AllocationContext} with a processor sharing resource.
	 *
	 * They are scheduled directly to the {@link SimulationScheduling}, to have them
	 * posted before the {@link SnapshotTaken}.
	 *
	 * @param procSharingJobs
	 */
	private void scheduleProcSharingUpdatesHelper(final Set<ActiveJob> procSharingJobs) {
		// these are the resource containers i must update
		final Set<AllocationContext> allocationContexts = procSharingJobs.stream()
				.map(job -> job.getAllocationContext()).collect(Collectors.toSet());

		for (final ActiveJob job : procSharingJobs) {
			final AllocationContext context = job.getAllocationContext();
			if (!allocationContexts.contains(context)) {
				continue;
			}
			allocationContexts.remove(context);

			final Job updateJob = this.createFakeJob(job);
			scheduling.scheduleEvent(new JobInitiated(updateJob));
		}
	}

	/**
	 * Create a copy of the given job, but replace the id with
	 * {@link SnapshotRecordingBehavior#FAKE}, such that the job can be recognised
	 * and filtered out later on.
	 *
	 * @param job blueprint to copy from
	 * @return fake job
	 */
	private ActiveJob createFakeJob(final ActiveJob job) {
		return ActiveJob.builder().withAllocationContext(job.getAllocationContext()).withDemand(0).withId(FAKE)
				.withProcessingResourceType(job.getProcessingResourceType()).build();
	}
}
