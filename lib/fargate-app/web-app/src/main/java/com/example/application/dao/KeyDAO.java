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

import com.example.application.data.Role;
import com.example.application.data.AppUser;
import com.example.application.data.Key;
import com.example.application.security.DataSourceImpl;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

@VaadinSessionScope
@Component
public class KeyDAO {
	private final static Logger logger = LogManager.getLogger(KeyDAO.class);

	@Autowired
	private DataSourceImpl dataSource;


	public KeyDAO() {

	}

	public void insertToDb(Key key) {
		String sql = "INSERT INTO `keys` (userId, s3PrivateKeyPath,s3PublicKeyPath, created) VALUES(?,?,?,?)";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);) {
			ps.setLong(1, key.getUserId());
			ps.setString(2, key.getS3PrivateKeyPath());
			ps.setString(3, key.getS3PublicKeyPath());
			ps.setTimestamp(4, java.sql.Timestamp.valueOf(key.getCreated()));
			ps.executeUpdate();
			try (
					ResultSet rs = ps.getGeneratedKeys();) {
				rs.next();
				key.setId(rs.getLong(1));
			}

		} catch (SQLException e) {
			logger.error(e.getMessage());
		}

	}

	public void deleteFromDb(Key key) {
		String sql = "DELETE FROM `keys` WHERE id = ?";
		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);

		) {
			ps.setLong(1, key.getId());
			ps.executeUpdate();
		} catch (SQLException e) {
			logger.error(e.getMessage());

		}
	}

	public Map<Long,Key> getKeys(AppUser appUser) {
		Map<Long,Key> keys = new HashMap<>();
		
		String sql = "SELECT k.id, k.userId, k.s3PrivateKeyPath, k.s3PublicKeyPath, k.created, a.username ";
		sql += " FROM `keys` k INNER JOIN AppUser a on a.id =k.userId ";

		try (
				Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql);
				ResultSet rs = ps.executeQuery();

		) {
			while (rs.next()) {
				Key key = new Key(
						rs.getLong("id"),
						rs.getLong("userId"),
						rs.getString("s3PrivateKeyPath"),
						rs.getString("s3PublicKeyPath"),
						rs.getTimestamp("created").toLocalDateTime());
				String username = rs.getString("username");
				/*
				 * Allow admins to view all keys, user's can view their own.
				 */

				if (appUser.getRole() == Role.ROLE_ADMIN || appUser.getUsername().equals(username)) {
					keys.put(key.getId(), key);
				}
			}
		} catch(SQLException e) {
			logger.error(e.getMessage());
		}
		return keys;
	}
	
}
