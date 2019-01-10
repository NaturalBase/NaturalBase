package com.example.naturalbase.naturalbase;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class HttpTask {
	public static final int RET_FAIL = -1;
	public static final int RET_OK = HttpsURLConnection.HTTP_OK;
	private static final int BUFFER_SIZE = 1024;
	private static final int END_OF_READ = -1;
	private static final int TIME_REFRESH = 30;//30 minutes go get new token by refresh_token
	private static final String grantType = "grant_type=authorization_code&";
	private static final String grantType1 = "grant_type=refresh_token&";
	private static final String authCodeStr = "code=";
	private static final String appIdStr = "&client_id=100564881&";
	private static final String secretKeyStr = "client_secret=e4c9dba375cb71844ee208b94c5f32a4&";
	private static final String uriStr = "redirect_uri=";
	private static final String uriStr1 = "hms://redirect_url";
	private static final String refreshStr = "refresh_token=";
	private URL targetUrl = null;
	private int connectTimeout = 0;
	private int readTimeout = 0;
	private static AccessToken tokenData = null;
	private static HttpsURLConnection  authConnection = null;
	private static int timerCount = 1;
	
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
		
		authConnection = prepareConnection();
		if (authConnection == null) {
			logger.error("sendAndWaitResponse authConnection failed!");
			return RET_FAIL;
		}
		
		if (tokenData == null) {
			tokenData = new AccessToken();
		}
		
		tokenData.authCode = authCode;
		
		if (!fillRequestBody(1)) {
			logger.error("sendAndWaitResponse fillRequestBody failed!");
			return RET_FAIL;
		}
		
		int retCode = waitForResponse(1);
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
	
	private boolean fillRequestBody(int getTokenType) {
		OutputStream outPutData = null;
		boolean errorFlag = false;

		logger.debug("fillRequestBody Enter!");
		
		if (authConnection == null) {
			logger.error("fillRequestBody authConnection = null fata error !!!");
			return false;
		}
			
		try {
			outPutData = authConnection.getOutputStream();
			String msgStr = new String();
			if (getTokenType == 1) {
				/* get access token by authcode, the msg body content type like:
				 * grant_type=authorization_code&code=Etersdfasgh74ddga%3d&client_id=12345&
				 * client_secret=0rDdfgyhytRtznPQSzr5pVw2&redirect_uri=hms%3A%2F%2Fredirect_url
				 */
				String authCode = URLEncoder.encode(tokenData.authCode, "GBK");
				String redirect = URLEncoder.encode(uriStr1, "GBK");
				msgStr = grantType + authCodeStr + authCode + appIdStr + secretKeyStr + uriStr + redirect;
			} else {
				/* get access token by refreshToken, the msg body content type like:
				 *  grant_type=refresh_token&client_id=12345&client_secret=
				 * bKaZ0VE3EYrXaXCdCe3d2k9few&refresh_token=2O9BSX675FGAJYK92KKGG
				 */
				String refreshToke = URLEncoder.encode(tokenData.refreshToken, "GBK");
				msgStr = grantType1 + appIdStr + secretKeyStr + refreshStr + refreshToke;
			}

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
	
	private int waitForResponse(int getTokenType) {
		logger.debug("waitForResponse Enter!");
		
		if (authConnection == null) {
			logger.error("waitForResponse authConnection = null fata error !!!");
			return RET_FAIL;
		}
		
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
				if (getTokenType == 1) {
					parseTokenData(outMsg, getTokenType);
					startRefreshTokenGet();
				} else {
					parseTokenData(outMsg, getTokenType);
				}
			} else {
				logger.debug("waitForResponse responseCode=" +responseCode);
			}
			return responseCode;
		} catch (IOException e) {
			logger.error("waitForResponse catch exception. Cause:" + e.getMessage());
			return RET_FAIL;
		} finally {
			authConnection.disconnect();
			authConnection = null;
		}
	}

	private void startRefreshTokenGet() {
		Timer timer = new Timer();
		logger.debug("startRefreshTokenGet enter !");
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (timerCount == TIME_REFRESH) {
					timerCount = 1;
					authConnection = prepareConnection();
					if (authConnection == null) {
						logger.error("startRefreshTokenGet authConnection failed!");
						return;
					}
					
					if (!fillRequestBody(2)) {
						logger.error("startRefreshTokenGet fillRequestBody failed!");
						return;
					}
					
					int retCode = waitForResponse(2);
					logger.debug("startRefreshTokenGet retCode=" +retCode);
				} else {
					logger.debug("startRefreshTokenGet timerCount=" +timerCount);
					timerCount++;
				}
			}
		}, 60000, 60000);
	}

	private void parseTokenData(String outMsg, int getTokenType) {
		JSONObject message = null;
		message = JSONObject.parseObject(outMsg);
		tokenData.accessToken = message.getString("access_token");
		tokenData.expiresTime = message.getIntValue("expires_in");
		if (getTokenType == 1) {
			tokenData.refreshToken = message.getString("refresh_token");
		}
		tokenData.scope = message.getString("scope");
		if (getTokenType == 1) {
			tokenData.tokenType = message.getString("token_type");
		}
		logger.debug("parseAccessTokenData AT= " + tokenData.accessToken +
				"expiresTime = " + tokenData.expiresTime +
				"RT=" +tokenData.refreshToken +
				"scope=" + tokenData.scope + 
				"tokenType=" + tokenData.tokenType);
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
