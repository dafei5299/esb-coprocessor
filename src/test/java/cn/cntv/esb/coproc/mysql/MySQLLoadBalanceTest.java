package cn.portal.esb.coproc.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

public class MySQLLoadBalanceTest {

	@Test
	public void loadbalance() throws Exception {
		String jdbc = "jdbc:mysql:loadbalance://centos1,centos2/test"
				+ "?loadBalanceStrategy=bestResponseTime";
		int i = 1;
		try (Connection conn = DriverManager.getConnection(jdbc, "root",
				"111111")) {
			conn.setAutoCommit(false);
			ResultSet rs;
			while (true) {
				System.out.println(i++);
				try {
					rs = conn.createStatement().executeQuery(
							"select @@hostname");
					while (rs.next())
						System.out.println("host: " + rs.getString(1));
					System.in.read();
					conn.commit();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
