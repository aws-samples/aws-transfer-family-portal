package com.example.application.data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class AppUser {

	private long id;
	private String firstName;
	private String lastName;
	private String username;
	private String email;
	private boolean enabled;
	private Role role;
	private String password = "";
	private LocalDateTime passwordExpiration;
	private Organization organization;
	private Map<Long, DirectoryMapping> directoryMappings = new HashMap<>();

	public AppUser(long id, String firstName, String lastName, String username, String email, boolean enabled,
			String password, Role role, LocalDateTime passwordExpiration, Organization organization) {
		this.id = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.username = username;
		this.email = email;
		this.enabled = enabled;
		this.password = password;
		this.role = role;
		this.passwordExpiration = passwordExpiration;
		this.organization = organization;
	}

	public String getFullName() {
		return firstName + " " + lastName;
	}

	public AppUser() {
		enabled = true;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Map<Long, DirectoryMapping> getDirectoryMappings() {
		return directoryMappings;
	}

	public void setDirectoryMappings(Map<Long, DirectoryMapping> directoryMappings) {
		this.directoryMappings = directoryMappings;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public LocalDateTime getPasswordExpiration() {
		return passwordExpiration;
	}

	public void setPasswordExpiration(LocalDateTime passwordExpiration) {
		this.passwordExpiration = passwordExpiration;
	}

	public Organization getOrganization() {
		return organization;
	}

	public void setOrganization(Organization organization) {
		this.organization = organization;
	}

}
