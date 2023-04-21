// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.example.application.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.application.data.*;
import com.example.application.security.DataSourceImpl;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

@VaadinSessionScope
@Component
public class AppUserDAO {

	@Autowired
	private DataSourceImpl dataSource;
	// @Autowired
	private OrganizationDAO organizationDAO;
	private final static Logger logger = LogManager.getLogger(AppUserDAO.class);

	public AppUserDAO(@Autowired OrganizationDAO organizationDAO) {
		this.organizationDAO = organizationDAO;
	}

	/***
	 * Check if the given username exists. This is an important validation step in
	 * creating a new user.
	 * 
	 * @param username
	 * @return
	 */
	public boolean usernameExists(String username) {
		boolean exists = true; /* Conservative: prove the username doesn't exist */
		String sql = "SELECT count(id) as mycount FROM AppUser WHERE username = ?";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);

		) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					exists = rs.getInt("mycount") > 0;
				}
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		return exists;
	}

	/***
	 * Load a user given their username.
	 * 
	 * @param username
	 * @return - User corresponding to the username.
	 */

	public AppUser getAppUser(String username) {
		String sql = "Select id, firstName, lastName, username, email, enabled, password, passwordExpiration, role, organizationId ";
		sql += "FROM AppUser WHERE username = ?";
		AppUser user = null;
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);

		) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					user = new AppUser(
							rs.getLong("id"),
							rs.getString("firstName"),
							rs.getString("lastName"),
							rs.getString("username"),
							rs.getString("email"),
							rs.getBoolean("enabled"),
							rs.getString("password"),
							Role.getAppRole(rs.getString("role")),
							rs.getTimestamp("passwordExpiration").toLocalDateTime(),
							this.organizationDAO.getOrganization(rs.getLong("organizationId")));
				}
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		sql = "SELECT id as directoryMappingId, entry, target, `write` ";
		sql += "FROM directoryMapping WHERE userId = ?";
		try (
				// Connection con = Toolkit.getRcdmsConnection();
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
			ps.setLong(1, user.getId());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					DirectoryMapping mapping = new DirectoryMapping(
							rs.getLong("directoryMappingId"),
							user.getId(),
							rs.getString("entry"),
							rs.getString("target"),
							rs.getBoolean("write"));
					user.getDirectoryMappings().put(mapping.getDirectoryMappingId(), mapping);
				}
			}

		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		if (user == null) {
			throw new NullPointerException("User " + username
					+ " returned a null.  Either the user couldn't be found or there's a database connectivity issue");
		}

		return user;
	}

	
	public static AppUser getAppUserStatic(String username) {
		String sql = "Select id, firstName, lastName, username, email, enabled, password, passwordExpiration, role, organizationId ";
		sql += "FROM AppUser WHERE username = ?";
		AppUser user = null;
		DataSourceImpl dataSource = new DataSourceImpl();
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);

		) {
			ps.setString(1, username);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					user = new AppUser(
							rs.getLong("id"),
							rs.getString("firstName"),
							rs.getString("lastName"),
							rs.getString("username"),
							rs.getString("email"),
							rs.getBoolean("enabled"),
							rs.getString("password"),
							Role.getAppRole(rs.getString("role")),
							rs.getTimestamp("passwordExpiration").toLocalDateTime(),
							OrganizationDAO.getOrganizationStatic(rs.getLong("organizationId")));
				}
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		sql = "SELECT id as directoryMappingId, entry, target, `write` ";
		sql += "FROM directoryMapping WHERE userId = ?";
		try (
				// Connection con = Toolkit.getRcdmsConnection();
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
			ps.setLong(1, user.getId());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					DirectoryMapping mapping = new DirectoryMapping(
							rs.getLong("directoryMappingId"),
							user.getId(),
							rs.getString("entry"),
							rs.getString("target"),
							rs.getBoolean("write"));
					user.getDirectoryMappings().put(mapping.getDirectoryMappingId(), mapping);
				}
			}

		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		if (user == null) {
			throw new NullPointerException("User " + username
					+ " returned a null.  Either the user couldn't be found or there's a database connectivity issue");
		}

		return user;
	}
	
	public Map<Long, AppUser> userMap = null;

	public Map<Long, AppUser> getAppUsers(boolean forceRefresh) {
		if (userMap == null || forceRefresh) {
			loadAppUsers();
		}

		return userMap;
	}

	
	public void deactivateAllUsers(Organization org) {
		if (userMap!=null) {
			userMap.values().stream().filter(user->user.getOrganization().getId()==org.getId()).forEach(user->user.setEnabled(false));
		}
		String sql = "UPDATE AppUser SET enabled = 0 WHERE organizationId = ?";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
				ps.setLong(1, org.getId());
				ps.executeUpdate();
				
		} catch(SQLException f) {
			logger.error(f.getMessage());
		}
		
	}
	public static Map<Long, AppUser> loadAppUsersStatic() {
		Map<Long, AppUser> userMap = new HashMap<>();
		logger.info("Loading app users");
		String sql = "Select id, firstName, lastName, username, email, enabled, password, passwordExpiration, role, organizationId ";
		sql += "FROM AppUser";
		DataSourceImpl dataSource = new DataSourceImpl();
		
		try (
				// Connection con = Toolkit.getRcdmsConnection();
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();) {
			while (rs.next()) {
				AppUser user = new AppUser(
						rs.getLong("id"),
						rs.getString("firstName"),
						rs.getString("lastName"),
						rs.getString("username"),
						rs.getString("email"),
						rs.getBoolean("enabled"),
						rs.getString("password"),
						Role.getAppRole(rs.getString("role")),
						rs.getTimestamp("passwordExpiration").toLocalDateTime(),
						OrganizationDAO.getOrganizationStatic(rs.getLong("organizationId")));
				if (user.getOrganization()==null) {
					logger.error("Organization null for user " + user.getUsername());
					System.exit(1);
				}
				userMap.put(user.getId(), user);

			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		sql = "SELECT id as directoryMappingId, userId, entry, target, `read`, `write` FROM directoryMapping";
		try (
				// Connection con = Toolkit.getRcdmsConnection();
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();) {

			while (rs.next()) {
				long userId = rs.getLong("userId");
				DirectoryMapping mapping = new DirectoryMapping(
						rs.getLong("directoryMappingId"),
						userId,
						rs.getString("entry"),
						rs.getString("target"),
						rs.getBoolean("write")

				);
				userMap.get(userId).getDirectoryMappings().put(mapping.getDirectoryMappingId(), mapping);
			}

		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		 return userMap;
	}

	
	
	/**
	 * Load all users.
	 * 
	 * @return - Map of all users indexed by id.
	 */
	private void loadAppUsers() {
		userMap = new HashMap<>();
		logger.info("Loading app users");
		String sql = "Select id, firstName, lastName, username, email, enabled, password, passwordExpiration, role, organizationId ";
		sql += "FROM AppUser";

		try (
				// Connection con = Toolkit.getRcdmsConnection();
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();) {
			while (rs.next()) {
				AppUser user = new AppUser(
						rs.getLong("id"),
						rs.getString("firstName"),
						rs.getString("lastName"),
						rs.getString("username"),
						rs.getString("email"),
						rs.getBoolean("enabled"),
						rs.getString("password"),
						Role.getAppRole(rs.getString("role")),
						rs.getTimestamp("passwordExpiration").toLocalDateTime(),
						organizationDAO.getOrganization(rs.getLong("organizationId")));
				if (user.getOrganization()==null) {
					logger.error("Organization null for user " + user.getUsername());
					System.exit(1);
				}
				userMap.put(user.getId(), user);

			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		sql = "SELECT id as directoryMappingId, userId, entry, target, `read`, `write` FROM directoryMapping";
		try (
				// Connection con = Toolkit.getRcdmsConnection();
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();) {

			while (rs.next()) {
				long userId = rs.getLong("userId");
				DirectoryMapping mapping = new DirectoryMapping(
						rs.getLong("directoryMappingId"),
						userId,
						rs.getString("entry"),
						rs.getString("target"),
						rs.getBoolean("write")

				);
				userMap.get(userId).getDirectoryMappings().put(mapping.getDirectoryMappingId(), mapping);
			}

		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		// return userMap;
	}

	public void update(AppUser user) {
		logger.info("Calling update AppUser for user " + user.getUsername());
		String sql = "UPDATE AppUser SET firstName = ?, lastName = ?, userName = ?, email = ?, password = ?,";
		sql += "role = ?, passwordExpiration = ?, enabled = ?, organizationId = ? WHERE id = ?";
		try (
				// Connection con = Toolkit.getRcdmsConnection();
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);

		) {
			ps.setString(1, user.getFirstName());
			ps.setString(2, user.getLastName());
			ps.setString(3, user.getUsername());
			ps.setString(4, user.getEmail());
			ps.setString(5, user.getPassword());
			ps.setString(6, user.getRole().getDescription());
			// ps.setString(6, user.getRole());
			ps.setTimestamp(7, java.sql.Timestamp.valueOf(user.getPasswordExpiration()));
			ps.setBoolean(8, user.isEnabled());
			ps.setLong(9, user.getOrganization().getId());
			ps.setLong(10, user.getId());
			ps.executeUpdate();
			logger.info("Updated user " + user.getUsername() + " in database");
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}

	public void update(DirectoryMapping directoryMapping) {
		String sql = "UPDATE directoryMapping SET entry = ?, target = ?, `write`=? WHERE id = ?";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
			ps.setString(1, directoryMapping.getEntry());
			ps.setString(2, directoryMapping.getTarget());
			ps.setBoolean(3, directoryMapping.isWrite());
			ps.setLong(4, directoryMapping.getDirectoryMappingId());
			ps.executeUpdate();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

	}

	public void insert(AppUser user/* , List<DirectoryMapping> directoryMappingSeeds */) {
		String sql = "INSERT INTO AppUser (firstName, lastName, username, email, enabled, role, password, passwordExpiration, organizationId) ";
		sql += "VALUES (?,?,?,?,?,?,?,?,?)";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

		) {
			ps.setString(1, user.getFirstName());
			ps.setString(2, user.getLastName());
			ps.setString(3, user.getUsername());
			ps.setString(4, user.getEmail());
			ps.setBoolean(5, user.isEnabled());
			ps.setString(6, user.getRole().getDescription());
			ps.setString(7, user.getPassword());
			ps.setTimestamp(8, java.sql.Timestamp.valueOf(user.getPasswordExpiration()));
			ps.setLong(9, user.getOrganization().getId());
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys();) {
				rs.next();
				user.setId(rs.getLong(1));
			}

		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
		if (userMap != null) {
			this.userMap.put(user.getId(), user);
		}
	}

	public void insertToDb(DirectoryMapping directoryMapping) {
		String sql = "INSERT INTO directoryMapping(userId, entry,target,`write`) VALUES(?,?,?,?);";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

		) {
			ps.setLong(1, directoryMapping.getUserId());
			ps.setString(2, directoryMapping.getEntry());
			ps.setString(3, directoryMapping.getTarget());
			ps.setBoolean(4, directoryMapping.isWrite());
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				rs.next();
				directoryMapping.setDirectoryMappingId(rs.getLong(1));
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	}

	public void deleteUserDirectoryMapping(AppUser user, long directoryMappingId) {
		logger.info("Deleting directory mapping from database");
		String sql = "DELETE FROM directoryMapping WHERE id = ?";
		try (
				// Connection con = Toolkit.getRcdmsConnection();
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
			ps.setLong(1, directoryMappingId);
			ps.executeUpdate();
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

		user.getDirectoryMappings().remove(directoryMappingId);

		logger.info("Deletion of directory mapping complete");

	}

}
