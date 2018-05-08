package cn.portal.esb.coproc.thread;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySQLDBHelper类功能：利用jdbc对MySQL数据库进行CRUD的操作
 * 
 * @author：liweichao
 */
public class MySQLDBHelper {

	/**
	 * 根据数据库的配置信息获得数据库的连接
	 */
	public static Connection getConnection() {
		Connection connection = null;
		try {
			/* 下面MySQL的配置信息后续可能需要换成配置文件的形式 */
			String driver = "com.mysql.jdbc.Driver";
			String url = "jdbc:MySQL://localhost:3306/redistest";
			String user = "root";
			String password = "065653";
			Class.forName(driver); // 加载数据库驱动
			if (null == connection) {
				connection = DriverManager.getConnection(url, user, password);
			}
		} catch (ClassNotFoundException e) {
			System.out
					.println("ClassNotFoundException！The message is as follow：");
			e.printStackTrace();
		} catch (SQLException e) {
			System.out.println("SQLException！The message is as follow：");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return connection;
	}

	/**
	 * 增删改[add,delete,update]操作
	 * 
	 * @param sql
	 * @return int
	 */
	public static int executeNonQuery(String sql) {
		int result = 0;
		Connection connection = null;
		Statement statement = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			result = statement.executeUpdate(sql);
		} catch (SQLException e) {
			System.out.println("SQLException！The message is as follow：");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			free(null, statement, connection);
		}
		return result;
	}

	/**
	 * 增删改[add,delete,update]操作
	 * 
	 * @param sql
	 * @param obj
	 * @return int
	 */
	public static int executeNonQuery(String sql, Object... object) {
		int result = 0;
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(sql);
			for (int i = 0; i < object.length; i++) {
				preparedStatement.setObject(i + 1, object[i]);
			}
			result = preparedStatement.executeUpdate();
		} catch (SQLException e) {
			System.out.println("SQLException！The message is as follow：");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			free(null, preparedStatement, connection);
		}
		return result;
	}

	/**
	 * 查询[Query]操作
	 * 
	 * @param sql
	 * @return resultSet
	 */
	public static ResultSet executeQuery(String sql) {
		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;
		try {
			connection = getConnection();
			statement = connection.createStatement();
			resultSet = statement.executeQuery(sql);
		} catch (SQLException e) {
			System.out.println("SQLException！The message is as follow：");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			free(resultSet, statement, connection);
		}
		return resultSet;
	}

	/**
	 * 查询[Query]操作
	 * 
	 * @param sql
	 * @param sql
	 * @return resultSet
	 */
	public static ResultSet executeQuery(String sql, Object... object) {
		Connection connection = null;
		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;
		try {
			connection = getConnection();
			preparedStatement = connection.prepareStatement(sql);
			for (int i = 0; i < object.length; i++) {
				preparedStatement.setObject(i + 1, object[i]);
			}
			resultSet = preparedStatement.executeQuery();
		} catch (SQLException e) {
			System.out.println("SQLException！The message is as follow：");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			free(resultSet, preparedStatement, connection);
		}
		return resultSet;
	}

	/**
	 * 判断记录是否存在
	 * 
	 * @param sql
	 * @return Boolean
	 */
	public static Boolean isExist(String sql) {
		ResultSet result = null;
		try {
			result = executeQuery(sql);
			result.last();
			int count = result.getRow();
			if (count > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			free(result);
			return false;
		} finally {
			free(result);
		}
	}

	/**
	 * 判断记录是否存在
	 * 
	 * @param sql
	 * @param object
	 * @return Boolean
	 */
	public static Boolean isExist(String sql, Object... object) {
		ResultSet result = null;
		try {
			result = executeQuery(sql, object);
			result.last();
			int count = result.getRow();
			if (count > 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			free(result);
			return false;
		} finally {
			free(result);
		}
	}

	/**
	 * 获取查询记录的总行数
	 * 
	 * @param sql
	 * @param obj
	 * @return int
	 */
	public static int getCount(String sql, Object... obj) {
		int result = 0;
		ResultSet resultSet = null;
		try {
			resultSet = executeQuery(sql, obj);
			resultSet.last();
			result = resultSet.getRow();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			free(resultSet);
		}
		return result;
	}

	/**
	 * 释放Resultset资源
	 * 
	 * @param resultSet
	 */
	public static void free(ResultSet resultSet) {
		try {
			if (resultSet != null) {
				resultSet.close();
			}
		} catch (SQLException e) {
			System.out.println("SQLException！The message is as follow：");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 释放Resultset资源
	 * 
	 * @param statement
	 */
	public static void free(Statement statement) {
		try {
			if (statement != null) {
				statement.close();
			}
		} catch (SQLException e) {
			System.out.println("SQLException！The message is as follow：");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 释放Connection资源
	 * 
	 * @param connection
	 */
	public static void free(Connection connection) {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			System.out.println("SQLException！The message is as follow：");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 释放所有数据资源
	 * 
	 * @param resultSet
	 * @param statement
	 * @param connection
	 */
	public static void free(ResultSet resultSet, Statement statement,
			Connection connection) {
		free(resultSet);
		free(statement);
		free(connection);
	}

}
