package cn.portal.esb.coproc.mapper.main;

import java.util.Set;

import cn.portal.esb.coproc.model.GroupModel;

public interface GroupDbMapper {

	GroupModel findByID(long id);

	Set<Long> findIDs();

}
