package cn.portal.esb.coproc.mapper.main;

import java.util.Set;

import cn.portal.esb.coproc.model.NgxNodeModel;

public interface NgxNodeDbMapper {

	NgxNodeModel findByID(long id);

	void save(NgxNodeModel ngx);

	Set<Long> findIDs();

	int getConcurrencyEstimatePerNgxNode();

}
