package cn.portal.esb.coproc.ssh;

import java.util.ArrayList;

import ch.ethz.ssh2.Connection;

public abstract class RemoteSSH {
	
	protected Connection connection;
	public abstract void execCommands(String shellcmds);
	public abstract ArrayList<String> execCommandsWithResult(String shellcmds);
}
