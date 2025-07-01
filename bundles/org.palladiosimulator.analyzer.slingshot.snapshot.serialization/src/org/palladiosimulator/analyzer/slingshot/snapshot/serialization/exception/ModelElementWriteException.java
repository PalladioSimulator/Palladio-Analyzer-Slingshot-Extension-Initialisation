package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.exception;

import java.io.IOException;

/**
 * An exception of this type indicates that writing a ModelElement to JSON failed. 
 * 
 * @author Sophie Stieß
 *
 */
public class ModelElementWriteException extends IOException {

    public ModelElementWriteException(final String message) {
        super(message);
    }
}
