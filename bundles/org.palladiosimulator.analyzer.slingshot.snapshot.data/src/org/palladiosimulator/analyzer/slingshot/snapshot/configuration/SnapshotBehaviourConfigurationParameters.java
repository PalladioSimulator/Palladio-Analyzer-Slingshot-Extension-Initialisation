package org.palladiosimulator.analyzer.slingshot.snapshot.configuration;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 
 * @author Sophie Stieß
 *
 */
public class SnapshotBehaviourConfigurationParameters {
	
	private final static String ACTIVE_KEY = "active";
	
	/**
	 * Must alway contain an entry for key {@link SnapshotBehaviourConfigurationParameters#ACTIVE_KEY}.
	 */
	private final Map<String, Object> configParams;

	/**
	 * Create new instance with only activation configuration parameter.
	 * 
	 * @param active activation value, true for configuring to active, false otherwise.
	 */
	public SnapshotBehaviourConfigurationParameters(final boolean active) {
		this(Map.of(ACTIVE_KEY, active));
	}
	
	/**
	 * Create new instance with given configuration parameter.
	 * 
	 * @param map of configuration parameters, must contain an entry for key {@link SnapshotBehaviourConfigurationParameters#ACTIVE_KEY}.
	 */
	public SnapshotBehaviourConfigurationParameters(final Map<String, Object> configParams) {
		super();
		this.configParams = configParams;
	}
	
	/**
	 * 
	 * @return true, iff this toggle configures a behaviour to be active.
	 */
	public boolean isActive() {
		return hasParameter(ACTIVE_KEY, Boolean.class) ? getParameter(ACTIVE_KEY) : true;
	}

	/**
	 * 
	 * @param key
	 * @param type
	 * @return
	 */
	public boolean hasParameter(final String key, final Class<?> type) {
		return configParams.containsKey(key) && configParams.get(key).getClass().isAssignableFrom(type);
	}
	
	/**
	 * 
	 * @param <T>
	 * @param key
	 * @return
	 */
	public <T> T getParameter(final String key) {
		if (configParams.containsKey(key)) {
			return (T) configParams.get(key);
		} else {
			throw new NoSuchElementException("Missing config parameter for key " + key + ".");
		}
	}
	
	/**
	 * 
	 * @return map of all configuration parameters. 
	 */
	public Map<String, Object> getConfigParams() {
		return Map.copyOf(this.configParams);
	}
}
