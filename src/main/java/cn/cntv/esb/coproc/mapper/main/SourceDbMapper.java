package cn.portal.esb.coproc.mapper.main;

import java.util.List;
import java.util.Set;

import cn.portal.esb.coproc.model.SourceAuthModel;
import cn.portal.esb.coproc.model.SourceModel;

public interface SourceDbMapper {

	List<SourceAuthModel> findAuthsByID(long id);

	SourceModel findByID(long id);

	Set<Long> findIDs();

}
