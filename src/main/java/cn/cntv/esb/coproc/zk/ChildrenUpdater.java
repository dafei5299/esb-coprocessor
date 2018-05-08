package cn.portal.esb.coproc.zk;

import java.util.List;

public interface ChildrenUpdater {

	void onChange(List<String> children);

}
