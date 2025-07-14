package org.palladiosimulator.analyzer.slingshot.snapshot.serialization.data;

import java.util.Objects;

public class SimpleEntity {
	public String field;

	public SimpleEntity(final String field) {
		this.field = field;
	}
	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SimpleEntity other = (SimpleEntity) obj;
		return Objects.equals(field, other.field);
	}	
}