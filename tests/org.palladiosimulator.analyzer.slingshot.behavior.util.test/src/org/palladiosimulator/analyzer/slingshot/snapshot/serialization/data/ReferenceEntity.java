package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data;

import java.util.Objects;

public class ReferenceEntity {
	public SimpleEntity getEntity1() {
		return entity1;
	}

	public SimpleEntity getEntity2() {
		return entity2;
	}

	private final SimpleEntity entity1;
	private final SimpleEntity entity2;
	
	public ReferenceEntity(final SimpleEntity entity1, final SimpleEntity entity2) {
		super();
		this.entity1 = entity1;
		this.entity2 = entity2;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ReferenceEntity other = (ReferenceEntity) obj;
		return Objects.equals(entity1, other.entity1) && Objects.equals(entity2, other.entity2);
	}

	
	
}
