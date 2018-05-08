package cn.portal.esb.coproc.mapper.main;

import java.util.Set;

import cn.portal.esb.coproc.model.SystemModel;

public interface SystemDbMapper {

	SystemModel findByID(long id);

	Set<Long> findIDs();

}
