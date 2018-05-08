package cn.portal.esb.coproc.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class RemoteSSHByPublicKey extends RemoteSSH{

	/**私有成员变量*/
	private String IPAddress;
	private String userName;
	private String keyFile;
	
	/**
	 * 构造函数重载
	 * @param IPAddress
	 * @param userName
	 * @param charset
	 */
	public RemoteSSHByPublicKey(String IPAddress,String userName,String keyFile){
		this.IPAddress = IPAddress;
		this.userName = userName;
		this.keyFile = keyFile;
	}
	
	/**
	 * 利用ssh的publickey登陆远程主机
	 * @return boolean
	 * @throws IOException
	 */
	@SuppressWarnings("finally")
	public boolean loginWithPublicKey(){
		boolean loginresult = false;
		try{
			File keyFile = new File(this.keyFile);
			connection =  new Connection(this.IPAddress);
			connection.connect();
			loginresult = connection.authenticateWithPublicKey(this.userName,keyFile,"");
		}catch(IOException ioexception){
			ioexception.printStackTrace();
		}finally{
			return loginresult;
		}
	}
	
	/**
	 * 执行Shell命令 
	 * @param shellcmds 命令行序列
	 * @return String
	 */
	@SuppressWarnings("resource")
	public void execCommands(String shellcmds){
		InputStream inputStream=null;
		String result = "";
		try{
			if(this.loginWithPublicKey()){
				Session session = connection.openSession();
				session.execCommand(shellcmds);
				inputStream = new StreamGobbler(session.getStdout());
				BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(inputStream));
				while((result=bufferedreader.readLine()) != null){
					System.out.println(result);
				}
				session.close();
			}
		}catch(IOException ioexception){
			ioexception.printStackTrace();
		}finally{
			connection.close();
		}
	}
	
	@SuppressWarnings({ "finally", "resource" })
	public ArrayList<String> execCommandsWithResult(String shellcmds){
		InputStream inputStream=null;
		String result = "";
		ArrayList<String> resultList = new ArrayList<String>();
		try{
			if(this.loginWithPublicKey()){
				Session session = connection.openSession();
				session.execCommand(shellcmds);
				inputStream = new StreamGobbler(session.getStdout());
				BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(inputStream));
				while((result=bufferedreader.readLine()) != null){
					resultList.add(result);
				}
				session.close();
			}
		}catch(IOException ioexception){
			ioexception.printStackTrace();
		}finally{
			connection.close();
			return resultList;
		}
	}
	
}
