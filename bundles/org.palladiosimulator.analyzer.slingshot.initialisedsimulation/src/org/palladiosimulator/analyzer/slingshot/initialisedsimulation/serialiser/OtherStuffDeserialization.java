package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.common.utils.PCMResourcePartitionHelper;
import org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialisation.OtherInitThings;
import org.palladiosimulator.analyzer.slingshot.snapshot.configuration.SnapshotBehaviourConfigurationParameters;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.spd.SPD;
import org.palladiosimulator.spd.ScalingPolicy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * 
 * Deserialize configuration information provided as input. 
 * 
 * 
 * @author Sophie Stieß
 *
 */
public final class OtherStuffDeserialization implements DeserializeParent<OtherInitThings> {

	protected static final Logger LOGGER = Logger.getLogger(OtherStuffDeserialization.class);
	
	private final Gson gson;
	private final SPD spd;
	
	/**
	 * 
	 * @param partition partition for accesing the spd model.
	 */
	public OtherStuffDeserialization(final PCMResourceSetPartition partition) {
		super();
		this.spd = PCMResourcePartitionHelper.getSPD(partition);
		this.gson = createGson();
	}

	/**
	 * 
	 * @return gson object with all adapter required to deserialize instances of {@link OtherInitThings}.
	 */
	private Gson createGson() {
		return new GsonBuilder()
				.registerTypeAdapter(ScalingPolicy.class, new ScalingPolicyDeserializer())
				.registerTypeAdapter(SnapshotBehaviourConfigurationParameters.class, new ConfigurationParameterDeserializer())
				.create();
	}
	
	@Override
	public OtherInitThings deserialize(final Path path) {
		final String read = read(path.toFile());
		return gson.fromJson(read, OtherInitThings.class);
	}
	
	/**
	 * 
	 * Deserialize a scaling policy.
	 * 
	 * In the json, scaling policies are represented by their string id. 
	 * This deserializer looks the given ids up in the spd model and returns the matching policy.
	 * 
	 * Requires that the spd is already loaded. 
	 * 
	 * Fails, if the scaling policy does not belong to the spd. 
	 * 
	 * @author Sophie Stieß
	 *
	 */
	private class ScalingPolicyDeserializer implements JsonDeserializer<ScalingPolicy> {
		@Override
		public ScalingPolicy deserialize(final JsonElement json, final Type typeOfT,
				final JsonDeserializationContext context) throws JsonParseException {

			if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
				final String id = json.getAsJsonPrimitive().getAsString();

				final List<ScalingPolicy> policies = spd.getScalingPolicies();
				final Optional<ScalingPolicy> matchingPolicy = policies.stream().filter(p -> p.getId().equals(id))
						.findFirst();

				if (matchingPolicy.isEmpty()) {
					throw new JsonParseException(String.format(
							"Cannot deserialise json \"%s\", no matching policy with id \"%s\" in SPD model \"%s\" [id = %s].",
							json.toString(), id, spd.getEntityName(), spd.getId()));
				}
				return matchingPolicy.get();
			}

			throw new JsonParseException(String
					.format("Cannot deserialise json \"%s\", expected policy id but found none.", json.toString()));
		}
	}
	
	/**
	 * 
	 * Deserialize the additional configuration parameters. 
	 * 
	 * @author Sophie Stieß
	 *
	 */
	private class ConfigurationParameterDeserializer implements JsonDeserializer<SnapshotBehaviourConfigurationParameters> {
		@Override
		public SnapshotBehaviourConfigurationParameters deserialize(final JsonElement json, final Type typeOfT,
				final JsonDeserializationContext context) throws JsonParseException {
			if (json.isJsonObject()) {
				final Gson delegate = new GsonBuilder().create();
				final JsonObject obj = json.getAsJsonObject();

				final Type type = new TypeToken<Map<String, Object>>() {
				}.getType();
				final Map<String, Object> map = delegate.fromJson(obj, type);

				return new SnapshotBehaviourConfigurationParameters(map);
			} else {
				throw new JsonParseException(String
					.format("Cannot deserialise json, expected json object but found \"%s\".", json.toString()));
			}
		}
	}
}
