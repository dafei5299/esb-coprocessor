package cn.portal.esb.coproc.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@SuppressWarnings("serial")
@JsonIgnoreProperties({ "system" })
public class SourceAuthModel implements Serializable {

	private long id;
	private long systemID;
	private SystemModel system;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getSystemID() {
		return systemID;
	}

	public void setSystemID(long systemID) {
		this.systemID = systemID;
	}

	public SystemModel getSystem() {
		return system;
	}

	public void setSystem(SystemModel system) {
		this.system = system;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + (int) (systemID ^ (systemID >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SourceAuthModel other = (SourceAuthModel) obj;
		if (id != other.id)
			return false;
		if (systemID != other.systemID)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SourceAuth [id=" + id + ", systemID=" + systemID + "]";
	}

}
