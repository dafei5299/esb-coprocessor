package cn.portal.esb.coproc.mapper.stat;

import org.apache.ibatis.annotations.Param;

import cn.portal.esb.coproc.model.StatisticsModel;

public interface StatisticsDbMapper {

	void save(StatisticsModel stat);

	boolean exists(String table);

	void merge(@Param("unit") int unit, @Param("dayTable") String dayTable,
			@Param("yearTable") String yearTable);

	void drop(String table);

}
