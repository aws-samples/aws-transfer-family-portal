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

import com.example.application.data.Organization;
import com.example.application.security.DataSourceImpl;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

@VaadinSessionScope
@Component
public class OrganizationDAO {
	private final static Logger logger = LogManager.getLogger(OrganizationDAO.class);

	public OrganizationDAO() {
	}

	@Autowired
	private DataSourceImpl dataSource;
//	private Map<Long, Organization> organizations = null;
	/*
	public Map<Long, Organization> getOrganizations() {
			loadOrganizations();
		return organizations;
	}*/


	
	public boolean organizationExists(String organizationDescription) {
		boolean exists = true; /* Conservative: prove the username doesn't exist */
		String sql = "SELECT count(id) as mycount FROM organization WHERE description = ?";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);

		) {
			ps.setString(1, organizationDescription);
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
	
	
	public void insert(Organization org) {
		String sql = "INSERT INTO organization (description, active) VALUES (?, ?)";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

		) {
			ps.setString(1, org.getDescription());
			ps.setBoolean(2, org.isActive());
			ps.executeUpdate();
			try (ResultSet rs = ps.getGeneratedKeys()) {
				rs.next();
				org.setId(rs.getLong(1));
			}
		} catch (SQLException e) {
			logger.error(e.getMessage());
		}
	//	this.organizations.put(org.getId(), org);
	}

	public void update(Organization org) {
		String sql = "UPDATE organization SET description = ?, active = ? WHERE id = ?";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);) {
			ps.setString(1, org.getDescription());
			ps.setBoolean(2, org.isActive());
			ps.setLong(3, org.getId());
			ps.executeUpdate();
		} catch (SQLException f) {
			logger.error(f.getMessage());
		}
	}

	public Map<Long, Organization> getOrganizations() {
		logger.info("Loading organizations from the DB");
		Map<Long, Organization> organizations = new HashMap<>();
		String sql = "SELECT id, description, active FROM organization";
		try (

				// Connection con = Toolkit.getRcdmsConnection();
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();

		) {
			while (rs.next()) {
				Organization org = new Organization(rs.getLong("id"), rs.getString("description"),
						rs.getBoolean("active"));
				organizations.put(org.getId(), org);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return organizations;
	}

	
	public Organization getOrganization(long id) {
		String sql = "SELECT id, description, active FROM organization WHERE id = ?";
		Organization org = null;
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
			) {
				ps.setLong(1, id);
				try(ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						org = new Organization(id, rs.getString("description"), rs.getBoolean("active"));
					}
				}} catch(SQLException e) {
				logger.error(e.getMessage());
			}
		
		return org;
	}
		
		/*
		if (this.organizations == null) {
			this.loadOrganizations();
		}
		return organizations.get(id);*/
	//}

}
