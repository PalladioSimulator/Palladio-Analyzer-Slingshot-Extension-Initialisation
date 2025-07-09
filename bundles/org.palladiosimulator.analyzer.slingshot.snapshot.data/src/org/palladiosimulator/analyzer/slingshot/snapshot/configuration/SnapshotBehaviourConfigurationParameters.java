package org.palladiosimulator.analyzer.slingshot.snapshot.configuration;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 
 * This class represents the configuration parameters for one specific
 * configurable behaviour extension. <br>
 * <br>
 * Theses configuration parameters are extracted from the value part of the the
 * {@code parameters} map in the config-json. Differend behaviour extensions may
 * define configuration parameters of differen types and names. To achieve
 * utmost generalizability, the configuration parameters are stored in a map of
 * type {@code Map<String, Object>}. <br>
 * Intended access to configuration parameter values is to first check whether
 * the parameter is available and has a value assignable to the given type and
 * then get it:
 * <pre>
 * <code>
 * protected final SnapshotBehaviourConfigurationParameters configParameters = ...;
 * ...
 * if (configParameters.hasParameter("parameterName", Double.class) { 
 * 	final double variable = configParameters.getParameter("parameterName"))
 * }
 * </code>
 * </pre>
 * 
 * All configuration parameters always contain an parameter named {@code "active"} with a value of type {@code boolean}. 
 * Unless it is explicitly set to {@code false}, it always defaults to {@code true}.
 * Any parameters named {@code "active"} with a value of a type other than {@code boolean} will be overwritten.
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
	 * @param active activation value, {@code true} for configuring to active, {@code false} otherwise.
	 */
	public SnapshotBehaviourConfigurationParameters(final boolean active) {
		this(Map.of(ACTIVE_KEY, active));
	}
	
	/**
	 * Create new instance with given configuration parameter.
	 * 
	 * If the given map has no entry for key
	 * {@link SnapshotBehaviourConfigurationParameters#ACTIVE_KEY}, or the type of
	 * the value is not {@code boolean}, a new entry is added to the parameters
	 * or the entry with the wrong type is overwritten.
	 * 
	 * @param map of configuration parameters
	 */
	public SnapshotBehaviourConfigurationParameters(final Map<String, Object> configParams) {
		super();
		this.configParams = configParams;
		if (!this.hasParameter(ACTIVE_KEY, Boolean.class)) {
			this.configParams.put(ACTIVE_KEY, true);
		}
	}
	
	/**
	 * 
	 * @return true, iff this toggle configures a behaviour to be active.
	 */
	public boolean isActive() {
		return getParameter(ACTIVE_KEY);
	}

	/**
	 * 
	 * @param key parameter to look up
	 * @param type desired type of value
	 * @return true if a parameter for the given key exists and the associated values is of the given type
	 */
	public boolean hasParameter(final String key, final Class<?> type) {
		return configParams.containsKey(key) && configParams.get(key).getClass().isAssignableFrom(type);
	}
	
	/**
	 * 
	 * @param <T> 
	 * @param key parameter to look up
	 * @return value for the given key
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
	 * @return copy of the map of all configuration parameters. 
	 */
	public Map<String, Object> getConfigParams() {
		return Map.copyOf(this.configParams);
	}
}
