package org.palladiosimulator.analyzer.slingshot.snapshot.serialization;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.palladiosimulator.pcm.core.CoreFactory;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryFactory;
import org.palladiosimulator.pcm.repository.util.RepositoryResourceFactoryImpl;
import org.palladiosimulator.pcm.seff.ResourceDemandingBehaviour;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.SeffFactory;
import org.palladiosimulator.pcm.seff.StartAction;
import org.palladiosimulator.pcm.seff.StopAction;
import org.palladiosimulator.pcm.usagemodel.ClosedWorkload;
import org.palladiosimulator.pcm.usagemodel.Delay;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.Start;
import org.palladiosimulator.pcm.usagemodel.Stop;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcm.usagemodel.UsagemodelFactory;
import org.palladiosimulator.pcm.usagemodel.util.UsagemodelResourceFactoryImpl;

public class ModelCreationHelper {
	
	final ResourceSet set = new ResourceSetImpl();
	
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
	
	
	public ResourceDemandingBehaviour createResourceDemandingBehaviour() {
		
		final ResourceDemandingSEFF rdseff = SeffFactory.eINSTANCE.createResourceDemandingSEFF();
		final BasicComponent component = RepositoryFactory.eINSTANCE.createBasicComponent();
		final Repository repo = RepositoryFactory.eINSTANCE.createRepository();
		
		
		final StartAction start = SeffFactory.eINSTANCE.createStartAction();
		final StopAction stop = SeffFactory.eINSTANCE.createStopAction();
		
		start.setSuccessor_AbstractAction(stop);
		stop.setPredecessor_AbstractAction(start);
		
		rdseff.getSteps_Behaviour().add(start);
		rdseff.getSteps_Behaviour().add(stop);
		
		component.setRepository__RepositoryComponent(repo);
		component.getServiceEffectSpecifications__BasicComponent().add(rdseff);
		
		final Resource res  = (new RepositoryResourceFactoryImpl()).createResource(URI.createFileURI("model.repository"));
		res.getContents().add(repo);
		
		set.getResources().add(res);
		
		return rdseff;
	}
}
