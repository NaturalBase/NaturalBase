package com.example.naturalbase.naturalstorage;

import java.util.List;
import java.sql.*;

import java.util.ArrayList;

public class NaturalStorage {
	
	public static final long TIMESTAMP_NOW = -1;
	
	// JDBC 驱动名及数据库 URL
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
    static final String DB_URL = "jdbc:mysql://49.4.95.51:3306/NaturalBaseDemo";
 
    // 数据库的用户名与密码
    static final String USER = "root";
    static final String PASS = "gaosi2";
    
    static final Connection conNaturalBase = null;
    static final Statement stmt = null;
    
    // sql 语句
    static String creatTable = "CREATE TABLE IF NOT EXISTS DATA("
            + "KEY        TEXT PRIMARY KEY  NOT  NULL,"
            + "VALUE      BLOB              NOT  NULL,"
            + "TIMESTAMP  NUMERIC           NOT  NULL,"
            + "DELETE_BIT INT               NOT  NULL,"
            + "SYNC_BIT   INT               NOT  NULL);";
    static String creatIndex = "CREATE INDEX IF NOT EXISTS IND ON DATA ('TIMESTAMP');"
	
	public static NaturalStorage getInstance() {
        if (instance == null) {
            if (instance == null) {
                instance = new NaturalStorage();
            }
        }
        return instance;
    }

    public NaturalStorage() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conNaturalBase = DriverManager.getConnection(DB_URL,USER,PASS);
            if(!conNaturalBase.isClosed()) {
                // todo: 输出打开数据库成功日志
            }
            stmt = conNaturalBase.createStatement();
            if(0 != stmt.executeLargeUpdate(creatTable)) {
                // todo: 输出创建表失败日志
            }
            if(0 != stmt.executeLargeUpdate(creatIndex)) {
            	// todo: 输出创建索引失败日志
            }
        } catch(SQLException se) {
            se.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
        	stmt.close();
        	conNaturalBase.close();
        }
    }
	
	public long SaveDataFromSync(List<DataItem> dataItemList) {
		long testTimeStamp = 123456;
		return testTimeStamp;
	}
	
	public List<DataItem> GetUnsyncData(long beginT, long endT){
		return new ArrayList<DataItem>();
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
}
