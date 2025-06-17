package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.serialiser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 
 * @author Sophie Stieß
 *
 */
public interface DeserializeParent<T>{	
	
	public T deserialize(final Path path);
		
	default String read(final File file) {
		try {
			return Files.readString(file.toPath());
		} catch (final IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
