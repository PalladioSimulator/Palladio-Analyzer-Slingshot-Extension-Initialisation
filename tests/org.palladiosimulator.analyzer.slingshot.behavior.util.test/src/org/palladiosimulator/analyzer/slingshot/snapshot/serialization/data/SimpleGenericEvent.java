package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data;

import org.palladiosimulator.analyzer.slingshot.common.events.AbstractGenericEvent;

public class SimpleGenericEvent extends AbstractGenericEvent<SimpleEntity, SimpleEntity> {
	public SimpleGenericEvent(final Class<SimpleEntity> concreteClass, final SimpleEntity entity, final double delay) {
		super(concreteClass, entity, delay);
	}
	
}