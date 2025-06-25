package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotBehaviourConfigurationParameters;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;

/**
 * 
 * Parent for all configurable behaviour extenstions. 
 * 
 * 
 * 
 * @author Sophie Stieß
 */
public abstract class ConfigurableSnapshotExtension implements SimulationBehaviorExtension {
	protected final SnapshotBehaviourConfigurationParameters toggle;
	
	/**
	 * 
	 * @param configuration
	 */
	public ConfigurableSnapshotExtension(final SnapshotConfiguration configuration) {
		super();
		if (configuration != null && configuration.getConfigurationParameters().containsKey(this.getToggleKey())) {
			this.toggle = configuration.getConfigurationParameters().get(this.getToggleKey());
		} else {
			this.toggle = new SnapshotBehaviourConfigurationParameters(true);
		}	
	}

	/**
	 * Get the key for identifying the {@link SnapshotBehaviourConfigurationParameters} of this class.
	 * 
	 * The default key is the simple class name. 
	 * Override this operation, to set an individual key for a certain class. 
	 * 
	 * @return key for identifying the {@link SnapshotBehaviourConfigurationParameters} of this class
	 */
	public String getToggleKey() {
		return this.getClass().getSimpleName();
	}
	
	@Override
	public final boolean isActive() {
		return toggle.isActive() && this.getActivated();
	}
	
	protected abstract boolean getActivated();
}
