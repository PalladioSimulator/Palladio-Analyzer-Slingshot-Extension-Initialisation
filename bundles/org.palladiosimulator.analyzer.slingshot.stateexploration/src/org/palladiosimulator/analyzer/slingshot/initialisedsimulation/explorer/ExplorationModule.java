package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.explorer;

import org.palladiosimulator.analyzer.slingshot.core.extension.AbstractSlingshotExtension;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.providers.SLOModelProvider;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;




public class ExplorationModule extends AbstractSlingshotExtension {

	@Override
	protected void configure() {
		provideModel(ServiceLevelObjectiveRepository.class, SLOModelProvider.class);
	}

}
