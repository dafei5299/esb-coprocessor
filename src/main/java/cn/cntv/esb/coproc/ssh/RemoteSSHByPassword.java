package cn.portal.esb.coproc.ssh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class RemoteSSHByPassword extends RemoteSSH{

	/**私有成员变量*/
	private String IPAddress;
	private String userName;
	private String password;
	
	/**
	 * 构造函数重载
	 * @param IPAddress
	 * @param userName
	 * @param charset
	 */
	public RemoteSSHByPassword(String IPAddress,String userName,String password){
		this.IPAddress = IPAddress;
		this.userName = userName;
		this.password = password;
	}
	
	/**
	 * 利用用户名和密码登录远程主机 
	 * @param boolean
	 */
	@SuppressWarnings("finally")
	public boolean loginWithUserNameAndPassword(){
		boolean loginResult = false;
		try{
			connection = new Connection(this.IPAddress); 
			connection.connect();
			loginResult = connection.authenticateWithPassword(this.userName, this.password);
		}catch(IOException ioexception){
			ioexception.printStackTrace();
		}finally{
			return loginResult;
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
			if(this.loginWithUserNameAndPassword()){
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
			if(this.loginWithUserNameAndPassword()){
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
