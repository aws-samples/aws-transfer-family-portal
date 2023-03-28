package com.example.application.data;

import java.util.Objects;

public class Organization {
	private long id;
	private String description;
	private boolean active;

	public Organization(long id, String description, boolean active) {
		this.id = id;
		this.description = description;
		this.active = active;
	}

	public Organization() {
		active = true;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Organization other = (Organization) obj;
		return id == other.id;
	}

}
