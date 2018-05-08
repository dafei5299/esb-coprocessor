package cn.portal.esb.coproc.ssh;

public class LogOperate {
	
	private RemoteSSH remotessh;
	/**打包和删除日志文件的shell脚本(带路径)*/
	private String ShellScript;
	
	public LogOperate(String pattern,String IP,String user,String password,String publickeypath,String shellscript){
		this.ShellScript = shellscript;
		if(pattern.equals("password")){
			this.remotessh = new RemoteSSHByPassword(IP,user,password);
		}else if(pattern.equals("publickey")){
			this.remotessh = new RemoteSSHByPublicKey(IP,user,publickeypath);
		}
	}
			
	public void DelLogFile(String day,String filename){
	    
		String cmd = this.ShellScript+" del "+day+" "+filename;
		this.remotessh.execCommands(cmd);
	}
	
	public void TarLogFile(String day){
		
		String cmd = this.ShellScript+" tar "+day;
		this.remotessh.execCommands(cmd);
	}
}
