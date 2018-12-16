package com.example.naturalbase.naturalstorage;

import java.util.List;
import java.util.ArrayList;

public class NaturalStorage {
	
	public static final long TIMESTAMP_NOW = -1;
	
	public NaturalStorage(){
		
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
