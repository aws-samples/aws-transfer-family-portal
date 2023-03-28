package com.example.application.security;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
//import java.util.logging.Logger;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

import com.example.application.Toolkit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Component
public class DataSourceImpl implements DataSource {
	private final static Logger logger = LogManager.getLogger(DataSourceImpl.class);

	public DataSourceImpl() {

	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getLoginTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	public Connection getConnection() throws SQLException {
		Connection con = null;
		String jdbc = null;
		try {
			Class.forName(Toolkit.mysqldriver);
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		try {
			jdbc = "jdbc:mysql://" + Toolkit.RDS_ENDPOINT
					+ ":3306/transfer?verifyServerCertificate=false&useSSL=false&requireSSL=false";
			con = DriverManager.getConnection(jdbc, Toolkit.RDS_USERNAME, Toolkit.RDS_PASSWORD);
			con.setAutoCommit(true);
		} catch (SQLException e) {
			logger.error("Failed to connect to " + jdbc);
			logger.info(e.getMessage());
		}
		return con;
	}*/

	
	@Override
	public Connection getConnection() throws SQLException {
		Toolkit.init();
    	try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
        String JDBC_URL = "jdbc:mysql://" + Toolkit.RDS_ENDPOINT + ":3306/FileTransferAdminPortal";
        Connection con = null;
        try {
        	con = DriverManager.getConnection(JDBC_URL, Toolkit.setMySqlConnectionProperties());
        	con.setAutoCommit(true);
        } catch(SQLException e) {
        	e.printStackTrace();
        	logger.info("Can't get SQL connection for  " + JDBC_URL);
        }
        return con;
    }
	
	
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return null;
	}

}
