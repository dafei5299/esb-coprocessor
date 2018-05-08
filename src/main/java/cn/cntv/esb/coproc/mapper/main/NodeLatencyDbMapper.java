package cn.portal.esb.coproc.mapper.main;

import java.util.Set;

import org.apache.ibatis.annotations.Param;

import cn.portal.esb.coproc.model.NodeLatencyModel;

public interface NodeLatencyDbMapper {

	NodeLatencyModel findByID(long id);

	Set<Long> findIDs();

	NodeLatencyModel find(@Param("nodeID") long nodeID,
			@Param("dataCenterID") long dataCenterID);

	void insert(NodeLatencyModel latency);

	void update(NodeLatencyModel latency);

}
