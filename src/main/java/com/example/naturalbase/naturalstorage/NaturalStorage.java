package com.example.naturalbase.naturalstorage;

import java.util.List;
import java.sql.*;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NaturalStorage {
	
	public static final long TIMESTAMP_NOW = -1;
	
	// JDBC 驱动名及数据库 URL
    static final String DB_URL = "jdbc:mysql://localhost:3306/NaturalBaseDemo?user=root&password=gaosi2&autoReconnect=true&failOverReadOnly=false";

    // 数据库的用户名与密码
    static final String USER = "root";
    static final String PASS = "gaosi2";
    
    static Connection conNaturalBase = null;
    static Statement stmt = null;
    static PreparedStatement pStmt = null;
    
    // sql 语句
    static String creatTable = "CREATE TABLE IF NOT EXISTS DATA("
            + "KNAME      VARCHAR(255) PRIMARY KEY  NOT  NULL,"
            + "VALUE      BLOB              NOT  NULL,"
            + "TIMESTAMP  BIGINT            NOT  NULL,"
            + "DELETE_BIT INT               NOT  NULL,"
            + "SYNC_BIT   INT               NOT  NULL,"
            + "DEVICEID   INT               NOT  NULL,"
            + "INDEX (TIMESTAMP),"
            + "INDEX (DEVICEID))";
    static String creatMetaTable = "CREATE TABLE IF NOT EXISTS METADATA("
            + "KNAME      VARCHAR(255) PRIMARY KEY  NOT  NULL,"
            + "VALUE      BLOB           NOT  NULL);";
    static String query1 = "SELECT * FROM DATA WHERE TIMESTAMP > ? AND DEVICEID <> ? ;";
    static String query2 = "SELECT * FROM DATA WHERE TIMESTAMP > ? AND TIMESTAMP < ? AND DEVICEID <> ?;";
    static String query3 = "SELECT * FROM DATA WHERE KNAME = ?;";
    static String query4 = "SELECT * FROM METADATA WHERE KNAME = ?;";
    static String replace = "REPLACE INTO DATA (KNAME, VALUE, TIMESTAMP, DELETE_BIT, SYNC_BIT, DEVICEID) VALUES (?,?,?,?,?,?);";
    static String replaceMeta = "REPLACE INTO METADATA (KNAME, VALUE) VALUES (?,?);";
    
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public NaturalStorage() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conNaturalBase = DriverManager.getConnection(DB_URL);
            if(!conNaturalBase.isClosed()) {
            	logger.debug("Connect database succesful!");
            }            
            stmt = conNaturalBase.createStatement();
            if(0 != stmt.executeLargeUpdate(creatTable)) {
            	logger.error("Creat table failed!");
            }
            if(0 != stmt.executeLargeUpdate(creatMetaTable)) {
            	logger.error("Creat Meta table failed!");
            }
        } catch(SQLException se) {
            se.printStackTrace();
            logger.error("NaturalStorage init sql catch exception. Cause:" + se.getCause().toString());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("NaturalStorage init catch exception. Cause:" + e.getCause().toString());
        } finally {
        }
    }
    
    public void finalize() {
    	try {
    		logger.debug("NaturalStorage start finalize!");
    		if (!pStmt.isClosed()) {
    			pStmt.close();
    		}
    		if (!stmt.isClosed()) {
    			stmt.close();
    		}
    		if (!conNaturalBase.isClosed()) {
    			conNaturalBase.close();
    		}
    		logger.debug("NaturalStorage finish finalize!");
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    		logger.error("NaturalStorage finalize catch exception. Cause:" + e.getCause().toString());
    	}
    }
    
    public void ReConnect() {
    	try {
			if (!conNaturalBase.isValid(0)) {
				conNaturalBase = DriverManager.getConnection(DB_URL,USER,PASS);
	            if(!conNaturalBase.isClosed()) {
	            	logger.debug("ReConnect database succesful!");
	            } 
			} else {
            	logger.debug("ReConnect Connect is not closed!");
            }
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("ReConnect sql catch exception. Cause:" + e.getCause().toString());
		}	
    }
	
	public long SaveDataFromSync(List<DataItem> dataItemList, int deviceId) {
		long maxTimeStamp = -1;
		long tempTimeStamp = -1;
		if (dataItemList == null) {
			return maxTimeStamp;
		}
		
		ReConnect();
		
		for (int i=0; i<dataItemList.size(); i++) {
			if (maxTimeStamp == -1) {
				maxTimeStamp = dataItemList.get(i).TimeStamp;
			} else {
				if (maxTimeStamp < dataItemList.get(i).TimeStamp) {
					maxTimeStamp = dataItemList.get(i).TimeStamp;
				}
			}
			try {
				pStmt = (PreparedStatement) conNaturalBase.prepareStatement(query3);
				pStmt.setString(1, dataItemList.get(i).Key);
				ResultSet rs = pStmt.executeQuery();
				if(rs.next()) {
					tempTimeStamp = rs.getLong("TIMESTAMP");
					// 等于再写一次应该也是没有问题的
					if (tempTimeStamp > dataItemList.get(i).TimeStamp) {
						break;
					}
				}
				pStmt.clearParameters();
				pStmt = (PreparedStatement) conNaturalBase.prepareStatement(replace);
				pStmt.setString(1, dataItemList.get(i).Key);
				pStmt.setString(2, dataItemList.get(i).Value);
				pStmt.setLong(3, dataItemList.get(i).TimeStamp);
				pStmt.setBoolean(4, dataItemList.get(i).DeleteBit);
				pStmt.setBoolean(5, true);
				pStmt.setInt(6, deviceId);
				pStmt.executeUpdate();
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
				logger.error("NaturalStorage save data sql catch exception. Cause:" + e.getCause().toString());
			}	
		}
		return maxTimeStamp;
	}
	
	public List<DataItem> GetUnsyncData(long beginT, long endT, int deviceId) {
		List<DataItem> dataItemList = new ArrayList<DataItem>();
		
		if (endT != TIMESTAMP_NOW && beginT > endT) {
			logger.debug("GetUnsyncData input error beginT > endT !");
		}
		
		ReConnect();
				
		if (endT == TIMESTAMP_NOW) {
			try {
				pStmt = (PreparedStatement) conNaturalBase.prepareStatement(query1);
				pStmt.setLong(1, beginT);
				pStmt.setInt(2, deviceId);
				ResultSet rs = pStmt.executeQuery();
				while(rs.next()) {
					DataItem dataItem = new DataItem();
					dataItem.Key = rs.getString("KNAME");
					dataItem.Value = rs.getString("VALUE");
					dataItem.TimeStamp = rs.getLong("TIMESTAMP");
					dataItem.DeleteBit = rs.getBoolean("DELETE_BIT");
					dataItemList.add(dataItem);
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
				logger.error("NaturalStorage get data 1 sql catch exception. Cause:" + e.getCause().toString());
			}			
		} else {
			try {
				pStmt = (PreparedStatement) conNaturalBase.prepareStatement(query2);
				pStmt.setLong(1, beginT);
				pStmt.setLong(2, endT);
				pStmt.setInt(3, deviceId);
				ResultSet rs = pStmt.executeQuery();
				while(rs.next()) {
					DataItem dataItem = new DataItem();
					dataItem.Key = rs.getString("KNAME");
					dataItem.Value = rs.getString("VALUE");
					dataItem.TimeStamp = rs.getLong("TIMESTAMP");
					dataItem.DeleteBit = rs.getBoolean("DELETE_BIT");
					dataItemList.add(dataItem);
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
				logger.error("NaturalStorage get data 2 sql catch exception. Cause:" + e.getCause().toString());
			}
		}
		return dataItemList;
	}
	
	public boolean SaveData(DataItem dataItem) {
		return true;
	}
	
	public DataItem GetData(String key) {
		return new DataItem();
	}
	
	public boolean RemoveData(String key) {
		return true;
	}
	
	public boolean SaveMetaData(DataItem dataItem) {
		if (dataItem == null) {
			logger.error("SaveMetaData input error dataItem = null");
			return false;
		}
		
		ReConnect();
		
		try {
			pStmt = (PreparedStatement) conNaturalBase.prepareStatement(replaceMeta);
			pStmt.setString(1, dataItem.Key);
			pStmt.setString(2, dataItem.Value);
			pStmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("NaturalStorage save meta data sql catch exception. Cause:" + e.getCause().toString());
		}
		
		return true;
	}
	
	public DataItem GetMetaData(String key) {
		if (key == null) {
			logger.error("GetMetaData input error key = null");
			return null;
		}
		
		ReConnect();
		
		try {
			pStmt = (PreparedStatement) conNaturalBase.prepareStatement(query4);
			pStmt.setString(1,key);
			ResultSet rs = pStmt.executeQuery();
			if(rs.next()) {
				DataItem dataItem = new DataItem();
				dataItem.Key = rs.getString("KNAME");
				dataItem.Value = rs.getString("VALUE");
				rs.close();
				return dataItem;
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.error("NaturalStorage get meta data sql catch exception. Cause:" + e.getCause().toString());
		}	
		return null;
	}
}
