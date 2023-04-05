package org.palladiosimulator.analyzer.slingshot.stateexploration.explorer.planning;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.slingshot.stateexploration.rawgraph.ToDoChange;

/**
 * TODO
 *
 * this is where i want to decide whether i will explore a given possible future, or not.
 * I have not yet any clue, how to decide o_O
 *
 * idk, this kinda looks like it could be a funcitonal interface...?
 *
 * @author stiesssh
 *
 */
public class CutOffConcerns {
	private static final Logger LOGGER = Logger.getLogger(CutOffConcerns.class.getName());

	public boolean shouldExplore(final ToDoChange future) {
		LOGGER.debug(String.format("Evaluation future %s.", future.toString()));
		LOGGER.debug(String.format("Future %s is rosy, will explore.", future.toString()));

		return true;
	}
}
