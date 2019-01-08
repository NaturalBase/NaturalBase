package com.example.naturalbase.naturalbase;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HttpTask {
	public static final int RET_FAIL = -1;
	public static final int RET_OK = HttpsURLConnection.HTTP_OK;
	private static int BUFFER_SIZE = 1024;
	private static int END_OF_READ = -1;
	private static final String grantType = "grant_type=authorization_code&";
	private static final String authCodeStr = "code=";
	private static final String appIdStr = "&client_id=100564881&";
	private static final String secretKeyStr = "client_secret=e4c9dba375cb71844ee208b94c5f32a4&";
	private static final String uriStr = "redirect_uri=";
	private static final String uriStr1 = "hms://redirect_url";
	private URL targetUrl = null;
	private int connectTimeout = 0;
	private int readTimeout = 0;
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public HttpTask(String inUrl, int inConnectTimeout, int inReadTimeout) {
		logger.debug("sendAndWaitResponse Enter ! inUrl=" +inUrl);
		targetUrl = getURL(inUrl);
		connectTimeout = inConnectTimeout;
		readTimeout = inReadTimeout;
	}
	
	private URL getURL(String inUrl) {
		URL outUrl = null;
		try {
			outUrl = new URL(inUrl);
		} catch (MalformedURLException e) {
			logger.error("getURL catch exception. Cause:" + e.getMessage());
		}
		return outUrl;		
	}
	
	public int sendAndWaitResponse(String authCode) {		
		logger.debug("sendAndWaitResponse Enter! authCode=" +authCode);
		
		if (authCode == null) {
			logger.error("sendAndWaitResponse authCode == null");
			return RET_FAIL;
		}
		
		HttpsURLConnection  authConnection = prepareConnection();
		if (authConnection == null) {
			logger.error("sendAndWaitResponse authConnection failed!");
			return RET_FAIL;
		}
		
		if (!fillRequestBody(authConnection, authCode)) {
			logger.error("sendAndWaitResponse fillRequestBody failed!");
			return RET_FAIL;
		}
		
		int retCode = waitForResponse(authConnection);
		logger.debug("sendAndWaitResponse Exit retCode=!" +retCode);
		return retCode;	
	}

	private HttpsURLConnection prepareConnection() {
		HttpsURLConnection authConnection = null;
		logger.debug("HttpsURLConnection Enter!");
		
		if (targetUrl == null || connectTimeout < 0 || readTimeout < 0) {
			logger.error("HttpsURLConnection para error !");
			return null;
		}

		try {
			authConnection = (HttpsURLConnection)targetUrl.openConnection();
			authConnection.setRequestMethod("POST");
			authConnection.setConnectTimeout(connectTimeout);
			authConnection.setReadTimeout(readTimeout);
			authConnection.setDoInput(true);
			authConnection.setDoOutput(true);
			authConnection.setUseCaches(false);
			authConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			authConnection.connect();
		} catch (IOException e) {
			logger.error("HttpsURLConnection catch exception. Cause:" + e.getMessage());
			if (authConnection != null) {
				authConnection.disconnect();
				authConnection = null;
			}
		}
		
		return authConnection;
	}
	
	private boolean fillRequestBody(HttpsURLConnection authConnection, String authCode) {
		OutputStream outPutData = null;
		boolean errorFlag = false;
		logger.debug("fillRequestBody Enter!");
		try {
			outPutData = authConnection.getOutputStream();
			String msgStr = new String();
			authCode = URLEncoder.encode(authCode, "GBK");
			String redirect = URLEncoder.encode(uriStr1, "GBK");
			msgStr = grantType + authCodeStr + authCode + appIdStr + secretKeyStr + uriStr + redirect;
			logger.debug("fillRequestBody  msgStr=" +msgStr);
			outPutData.write(msgStr.getBytes());
			outPutData.flush();
			return true;
		} catch (IOException e) {
			logger.error("fillRequestBody catch exception. Cause:" + e.getMessage());
			errorFlag = true;
			return false;
		} finally {
			if (errorFlag) {
				logger.debug("fillRequestBody disconnect !");
				authConnection.disconnect();
			}
			closeCloseable(outPutData);	
		}
	}
	
	private int waitForResponse(HttpsURLConnection authConnection) {
		logger.debug("waitForResponse Enter!");
		try {
			int responseCode = authConnection.getResponseCode();
			if (responseCode == HttpsURLConnection.HTTP_OK) {
				InputStream inData = authConnection.getInputStream();
				String outMsg = getStringFromInputStream(inData);
				if (outMsg == null) {
					logger.error("waitForResponse  outMsg == null!");
					return RET_FAIL;
				}
				logger.debug("waitForResponse outMsg=" +outMsg);
			} else {
				logger.debug("waitForResponse responseCode=" +responseCode);
			}
			return responseCode;
		} catch (IOException e) {
			logger.error("waitForResponse catch exception. Cause:" + e.getMessage());
			return RET_FAIL;
		} finally {
			authConnection.disconnect();
			logger.debug("waitForResponse disconnect !");
		}
	}

	private String getStringFromInputStream(InputStream inData) {
		logger.debug("getStringFromInputStream Enter!");
		ByteArrayOutputStream outByteArray = new ByteArrayOutputStream();
		byte[] readBuff = new byte[BUFFER_SIZE];
		long totalLen = 0;
		
		try {
			int readLen;
			while ((readLen = inData.read(readBuff)) != END_OF_READ) {
				totalLen += readLen;
				outByteArray.write(readBuff, 0 , readLen);
			}
			logger.debug("getStringFromInputStream totalLen=" +totalLen);
			String responsMsg = outByteArray.toString();
			return responsMsg;
		} catch (IOException e) {
			logger.error("getStringFromInputStream catch exception. Cause:" + e.getMessage());
			return null;
		} finally {
			logger.debug("getStringFromInputStream close inputStrem!");
			closeCloseable(inData);
		}
	}

	private void closeCloseable(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				logger.error("closeCloseable catch exception. Cause:" + e.getMessage());
			}
		}
	}
	
}
