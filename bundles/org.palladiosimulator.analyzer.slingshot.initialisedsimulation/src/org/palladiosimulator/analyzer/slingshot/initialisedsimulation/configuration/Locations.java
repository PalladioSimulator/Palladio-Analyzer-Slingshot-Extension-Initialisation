package org.palladiosimulator.analyzer.slingshot.initialisedsimulation.configuration;

import java.nio.file.Path;

/**
 * 
 * @author Sophie Stiess
 *
 */
public record Locations(Path snapshotIn, Path configsIn, Path output, Path snapshotOut, Path stateOut) {

}
