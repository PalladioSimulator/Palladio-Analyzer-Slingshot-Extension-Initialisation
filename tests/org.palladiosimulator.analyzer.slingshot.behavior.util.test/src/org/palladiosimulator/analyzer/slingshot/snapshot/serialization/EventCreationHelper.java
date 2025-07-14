package org.palladiosimulator.analyzer.slingshot.snapshot.serialization;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.palladiosimulator.analyzer.slingshot.common.events.DESEvent;
import org.palladiosimulator.analyzer.slingshot.core.events.SimulationStarted;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.ClassThing;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.EListThing;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.LoopThingChild;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.LoopThingParent;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.OptionalThing;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.PCMEvent;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.PCMThing;
import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data.rubbisch.Thing;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.usagemodel.ClosedWorkload;
import org.palladiosimulator.pcm.usagemodel.Delay;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcm.usagemodel.UsagemodelFactory;
import org.palladiosimulator.pcm.usagemodel.util.UsagemodelResourceFactoryImpl;

public class EventCreationHelper {
	
	final ResourceSet set = new ResourceSetImpl();
	
	public Set<DESEvent> createEvents() {	
		final UsageModel model = createUsageModel();
		
		final LoopThingParent loopParent = new LoopThingParent("loopParent");
		final LoopThingChild loopChild1 = new LoopThingChild("loopChild1", loopParent);
		final LoopThingChild loopchild2 = new LoopThingChild("loopChild2", loopParent);
		
		loopParent.addLoopChild(loopChild1);
		loopParent.addLoopChild(loopchild2);
		
		
		final Thing thing1 = new Thing("thing1", null);
		final Thing thing2 = new Thing("thing2", thing1);
		final Thing thing3 = new Thing("thing3", thing2);
		
		final PCMThing pcmThing1 = new PCMThing(model, thing1);
		final PCMThing pcmThing2 = new PCMThing(model, thing2);
		
		final Thing thing4 = new Thing("thing4", pcmThing2);
		

		final ClassThing classThing1 = new ClassThing<Double>(Double.class);
		final ClassThing classThing2 = new ClassThing<Thing>(Thing.class);
		
		final EListThing elistThing1 = new EListThing(model.getUsageScenario_UsageModel().get(0).getScenarioBehaviour_UsageScenario().getActions_ScenarioBehaviour());
		
		final OptionalThing<Thing> optionalThing1 = new OptionalThing<>(thing1);
		final OptionalThing<Thing> optionalThing11 = new OptionalThing<>(thing1);
		final OptionalThing<Optional<Thing>> optionalThing2 = new OptionalThing<>(Optional.of(thing1));
		final OptionalThing<Thing> optionalThing3 = new OptionalThing<>(null);
		final OptionalThing<UsageModel> optionalThing4 = new OptionalThing<>(model);
		
		final Set<DESEvent> events = new HashSet<>();
		events.add(new SimulationStarted());
//		events.add(new SimulationStarted());
//		events.add(new SimulationFinished());
		events.add(new PCMEvent(model, loopParent));
//		events.add(new PCMEvent(model, optionalThing1));
//		events.add(new PCMEvent(model, optionalThing11));
		
//		events.add(new PCMEvent(model, thing2));
//		events.add(new PCMEvent(model, thing2));
//		events.add(new PCMEvent(model, optionalThing3));
//		events.add(new PCMEvent(model, optionalThing4));
//		events.add(new PCMEvent(model, pcmThing1));
//		events.add(new PCMEvent(model, pcmThing2));
//		events.add(new PCMEvent(model, pcmThing2));
//		events.add(new PCMEvent(model, thing4));
		
//		events.add(new GenericPCMEvent(model));
//		events.add(new GenericPCMEvent2<>(model));
//		events.add(new GenericPCMEvent2<>(model.getUsageScenario_UsageModel().get(0)));
//		events.add(new GenericPCMEvent2<>(model));
//		events.add(new GenericPCMEvent2<>(thing1));
		
		return events;
	}
	
	public UsageModel createUsageModel() {
		final UsageModel usageModel = UsagemodelFactory.eINSTANCE.createUsageModel();
		final UsageScenario usageScenario = UsagemodelFactory.eINSTANCE.createUsageScenario();

		// workload
		final ClosedWorkload closedWorkload = UsagemodelFactory.eINSTANCE.createClosedWorkload();
		// set bi-directional reference to usage scenario
		closedWorkload.setUsageScenario_Workload(usageScenario);
		closedWorkload.setPopulation(1);
		final PCMRandomVariable thinkTime = CoreFactory.eINSTANCE.createPCMRandomVariable();
		thinkTime.setSpecification("1.0");
		closedWorkload.setThinkTime_ClosedWorkload(thinkTime);

		usageScenario.setWorkload_UsageScenario(closedWorkload);

		// usage behavior
		// entities
		final ScenarioBehaviour behavior = UsagemodelFactory.eINSTANCE.createScenarioBehaviour();
		behavior.setEntityName("scenarioBehavior");
		final Start startEntity = UsagemodelFactory.eINSTANCE.createStart();
		startEntity.setEntityName("start");
		final Delay delayEntity = UsagemodelFactory.eINSTANCE.createDelay();
		final PCMRandomVariable delayTime = CoreFactory.eINSTANCE.createPCMRandomVariable();
		delayTime.setSpecification("1.0");
		delayEntity.setTimeSpecification_Delay(delayTime);
		delayEntity.setEntityName("delay");
		final Stop stopEntity = UsagemodelFactory.eINSTANCE.createStop();
		stopEntity.setEntityName("stop");

		// references
		startEntity.setScenarioBehaviour_AbstractUserAction(behavior);
		delayEntity.setScenarioBehaviour_AbstractUserAction(behavior);
		stopEntity.setScenarioBehaviour_AbstractUserAction(behavior);
		startEntity.setSuccessor(delayEntity);
		delayEntity.setSuccessor(stopEntity);
		stopEntity.setPredecessor(delayEntity);

		behavior.setUsageScenario_SenarioBehaviour(usageScenario);
		usageScenario.setScenarioBehaviour_UsageScenario(behavior);
		usageModel.getUsageScenario_UsageModel().add(usageScenario);
		
		final Resource res  = (new UsagemodelResourceFactoryImpl()).createResource(URI.createFileURI("model.usagemodel"));
		res.getContents().add(usageModel);
		
		
		set.getResources().add(res);
		
		return usageModel;
	}
}
