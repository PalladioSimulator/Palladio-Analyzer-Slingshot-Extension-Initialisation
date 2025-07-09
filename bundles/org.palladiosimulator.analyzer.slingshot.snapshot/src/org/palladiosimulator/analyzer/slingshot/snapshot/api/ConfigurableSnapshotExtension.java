package org.palladiosimulator.analyzer.slingshot.snapshot.api;

import org.palladiosimulator.analyzer.slingshot.core.extension.SimulationBehaviorExtension;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotBehaviourConfigurationParameters;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotConfiguration;

/**
 * 
 * Parent for all configurable behaviour extenstions. <br>
 * <br>
 * Defines the key for mapping the configuration parameter to their respective
 * behaviour extension. For more details, confer
 * {@link ConfigurableSnapshotExtension#getKey()} <br>
 * <br>
 * The configuration parameters are extracted from a config-json. The key in the
 * config-json must match the key returned by
 * {@link ConfigurableSnapshotExtension#getKey()}, to associate the config
 * parameters with the correct behaviour extension. <br>
 * 
 * @author Sophie Stieß
 */
public abstract class ConfigurableSnapshotExtension implements SimulationBehaviorExtension {
	protected final SnapshotBehaviourConfigurationParameters configParameters;
	
	/**
	 * 
	 * @param configuration
	 */
	public ConfigurableSnapshotExtension(final SnapshotConfiguration configuration) {
		super();
		if (configuration != null && configuration.getConfigurationParameters().containsKey(this.getKey())) {
			this.configParameters = configuration.getConfigurationParameters().get(this.getKey());
		} else {
			this.configParameters = new SnapshotBehaviourConfigurationParameters(true);
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
	public String getKey() {
		return this.getClass().getSimpleName();
	}
	
	@Override
	public final boolean isActive() {
		return configParameters.isActive() && this.getActivated();
	}
	
	protected abstract boolean getActivated();
}
