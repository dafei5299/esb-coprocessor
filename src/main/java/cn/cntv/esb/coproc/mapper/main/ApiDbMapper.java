package cn.portal.esb.coproc.mapper.main;

import java.util.List;
import java.util.Set;

import cn.portal.esb.coproc.model.ApiModel;
import cn.portal.esb.coproc.model.ApiParamModel;

public interface ApiDbMapper {

	List<ApiParamModel> findParamsByID(long id);

	ApiModel findByID(long id);

	Set<Long> findIDs();

}
