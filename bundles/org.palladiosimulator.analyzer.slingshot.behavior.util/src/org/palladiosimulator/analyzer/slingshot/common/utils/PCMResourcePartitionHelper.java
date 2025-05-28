package org.palladiosimulator.analyzer.slingshot.common.utils;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.semanticspd.Configuration;
import org.palladiosimulator.semanticspd.SemanticspdPackage;
import org.palladiosimulator.servicelevelobjective.ServiceLevelObjectiveRepository;
import org.palladiosimulator.servicelevelobjective.ServicelevelObjectivePackage;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.SpdPackage;

/**
 * Helper to safely access models from the {@link PCMResourceSetPartition} for
 * which the partition does not provide direct getters.
 *
 * @author Sophie Stieß
 *
 */
public final class PCMResourcePartitionHelper {
	private static final Logger LOGGER = Logger.getLogger(PCMResourcePartitionHelper.class.getName());

	/**
	 * Get the {@link MonitorRepository} instance from the given partition.
	 * 
	 * @param partition partition to access
	 * @return model from the given partition
	 */
	public static MonitorRepository getMonitorRepository(final PCMResourceSetPartition partition) {
		final List<EObject> monitors = partition.getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository());
		if (monitors.size() == 0) {
			throw new IllegalStateException("No MonitorRepository in PCMResourceSetPartition found.");
		}
		return (MonitorRepository) monitors.get(0);
	}

	/**
	 * Get the {@link MeasuringPointRepository} instance from the given partition.
	 * 
	 * @param partition partition to access
	 * @return model from the given partition
	 */
	public static MeasuringPointRepository getMeasuringPointRepository(final PCMResourceSetPartition partition) {
		final List<EObject> measuringpoints = partition
				.getElement(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository());
		if (measuringpoints.size() == 0) {
			throw new IllegalStateException("No MeasuringPointRepository in PCMResourceSetPartition found.");
		}
		return (MeasuringPointRepository) measuringpoints.get(0);
	}

	/**
	 * Get the {@link SPD} instance from the given partition.
	 * 
	 * @param partition partition to access
	 * @return model from the given partition
	 */
	public static SPD getSPD(final PCMResourceSetPartition partition) {
		final List<EObject> spds = partition.getElement(SpdPackage.eINSTANCE.getSPD());
		if (spds.size() == 0) {
			throw new IllegalStateException("No SPD in PCMResourceSetPartition found.");
		}
		return (SPD) spds.get(0);
	}
	
	/**
	 * Get the {@link Configuration} instance from the given partition.
	 * 
	 * @param partition partition to access
	 * @return model from the given partition
	 */
	public static Configuration getSemanticSPD(final PCMResourceSetPartition partition) {
		final List<EObject> semanticSpds = partition.getElement(SemanticspdPackage.eINSTANCE.getConfiguration());
		if (semanticSpds.size() == 0) {
			throw new IllegalStateException("No SemanticConfiguration for SPD in PCMResourceSetPartition found.");
		}
		return (Configuration) semanticSpds.get(0);
	}

	/**
	 * Get the {@link ServiceLevelObjectiveRepository} instance from the given partition.
	 * 
	 * @param partition partition to access
	 * @return model from the given partition
	 */
	public static ServiceLevelObjectiveRepository getSLORepository(final PCMResourceSetPartition partition) {
		final List<EObject> slos = partition
				.getElement(ServicelevelObjectivePackage.eINSTANCE.getServiceLevelObjectiveRepository());
		if (slos.size() == 0) {
			throw new IllegalStateException("No ServiceLevelObjectiveRepository in PCMResourceSetPartition found.");
		}
		return (ServiceLevelObjectiveRepository) slos.get(0);
	}
}
