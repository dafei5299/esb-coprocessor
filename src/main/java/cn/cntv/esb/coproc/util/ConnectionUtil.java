package cn.portal.esb.coproc.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import cn.portal.esb.coproc.util.sub.Result;

public class ConnectionUtil {
	
	public static void main(String[] args) {
		String url = "http://www.gamersky.com";
		//System.out.println(ConnectionUtil.connect(url));
		Result<String> result = tryConnect(url, 3);
		if(result.isOk()){
			System.out.println("[NgxCacheLocalCluster] Lvs response: "+result.getResultVal());
		}else{
			System.out.println("[NgxCacheLocalCluster] Lvs exception: "+result.getMsg());
		}
	}
	
	public static String connect(String url){
		return connect(url, "UTF-8");
	}
	
	public static String connect(String url, String charset){
		HttpURLConnection conn = null;
		StringBuffer content = new StringBuffer();
		try{
			URL _url = new URL(url);
			conn = (HttpURLConnection)_url.openConnection();
			conn.setRequestMethod("GET");
			int response_code = conn.getResponseCode();
            if (response_code == HttpURLConnection.HTTP_OK) {
            	InputStream in = conn.getInputStream();
            	BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
            	String line = null;
            	while ((line = reader.readLine()) != null) {
            		content.append(line);
            	}
            	return content.toString();
            }else{
            	throw new RuntimeException("wrong response code:"+response_code);
            }
		}catch (Exception e) {
			throw new RuntimeException(e);
		}finally{
			if(conn!=null){
				conn.disconnect();
			}
		}
	}
	
	public static Result<String> tryConnect(String url,int maxTryTimes){
		int tryTimes = maxTryTimes;
		StringBuffer errorMsg = new StringBuffer();
		while(true){
			if(tryTimes>0){				
				try {
					String resultVal = connect(url);
					return new Result<String>(true, resultVal);
				} catch (Exception e) {
					errorMsg.append("try connection("+tryTimes+") exception:"+e.toString()+",errorMsg:"+e.getMessage());
					errorMsg.append("\n");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
					}
				}
				tryTimes --;
			}else{
				errorMsg.append("exceed allowable times:"+maxTryTimes);				
				return new Result<String>(false, null, errorMsg.toString());
			}
		}
	}
	

	
}
