package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.adapters;

import java.io.IOException;

import org.palladiosimulator.analyzer.slingshot.snapshot.serialization.util.SnapshotSerialisationUtils;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * 
 * Serializes the content of fields of type {@link TypeToken} as the type token's raw type's canonical name. 
 * 
 * @author Sophie Stieß
 *
 */
public class TypeTokenTypeAdapter extends TypeAdapter<TypeToken<?>> {

	public TypeTokenTypeAdapter() {
		super();
	}

	@Override
	public void write(final JsonWriter out, final TypeToken<?> value) throws IOException {
		out.value(value.getRawType().getCanonicalName());
	}

	@Override
	public TypeToken<?> read(final JsonReader in) throws IOException {
		final String s = in.nextString();
		
		try {
			return new TypeToken<>() {}.resolveType(SnapshotSerialisationUtils.getClassHelper(s));
		} catch (final ClassNotFoundException e) {
			throw new JsonParseException("Could not create TypeToke, failed to parse" + s + " to java class.", e);
		}
	}

}
