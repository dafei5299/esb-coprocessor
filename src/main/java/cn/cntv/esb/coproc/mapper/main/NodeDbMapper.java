package cn.portal.esb.coproc.mapper.main;

import java.util.Set;

import cn.portal.esb.coproc.model.NodeModel;

public interface NodeDbMapper {

	NodeModel findByID(long id);

	Set<Long> findIDs();

}
