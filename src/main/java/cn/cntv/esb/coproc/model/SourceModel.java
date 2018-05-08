package cn.portal.esb.coproc.model;

import java.io.Serializable;
import java.util.List;

import com.google.common.hash.Hashing;

@SuppressWarnings("serial")
public class SourceModel implements Serializable {

	private long id;
	private String key;
	private String md5;
	private String name;
	private boolean enable;
	private List<SourceAuthModel> auths;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
		// 将key进行md5散列
		md5 = Hashing.md5().hashBytes(key.getBytes()).toString();
	}

	public String getMd5() {
		return md5;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public List<SourceAuthModel> getAuths() {
		return auths;
	}

	public void setAuths(List<SourceAuthModel> auths) {
		this.auths = auths;
	}

	public boolean deepEquals(SourceModel other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (id != other.id)
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (enable != other.enable)
			return false;
		if (auths == null) {
			if (other.auths != null)
				return false;
		} else if (!auths.equals(other.auths))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		SourceModel other = (SourceModel) obj;
		if (id != other.id)
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Source [id=" + id + ", key=" + key + ", md5=" + md5 + ", name="
				+ name + ", enable=" + enable + ", auths=" + auths + "]";
	}

}
