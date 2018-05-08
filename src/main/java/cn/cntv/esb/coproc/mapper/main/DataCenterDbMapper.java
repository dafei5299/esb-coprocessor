package cn.portal.esb.coproc.mapper.main;

import org.apache.ibatis.annotations.Param;

import cn.portal.esb.coproc.model.DataCenterModel;

public interface DataCenterDbMapper {

	DataCenterModel findByCoprocessorEndpoint(@Param("host") String host,
			@Param("port") int port);

	String findBackupEndpoint();

}
