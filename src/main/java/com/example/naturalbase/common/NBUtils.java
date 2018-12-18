package com.example.naturalbase.common;

import com.alibaba.fastjson.JSONObject;

public class NBUtils {
	public static String  generateErrorInfo(String s) {
		JSONObject responseBody = new JSONObject();
		responseBody.put("Reason", s);
		return responseBody.toJSONString();
	}
}
