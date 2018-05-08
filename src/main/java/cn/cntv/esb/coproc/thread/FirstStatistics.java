package cn.portal.esb.coproc.thread;

import java.util.HashMap;

/**
 * FirstStatistics类功能：利用循环每一条数据进行统计 Author：liweichao
 */
public class FirstStatistics {

	/** 逐条统计的缓冲区 */
	public HashMap<String, IndicatorBean> hashmap;

	/** jsonConvertToBean类的实例，依赖关系 */
	public JSONConvertToBean jsonConvertToBean;

	/**
	 * 构造函数重载
	 */
	public FirstStatistics() {
		this.hashmap = new HashMap<String, IndicatorBean>();
	}

	/**
	 * 统计第一个Redis数据的方法
	 */
	public void DoStatistics() {

		String appkey = this.jsonConvertToBean.redisToMySQLBean.getAppkey();
		String system = this.jsonConvertToBean.redisToMySQLBean.getSystem();
		long bandwidth = this.jsonConvertToBean.redisToMySQLBean.getBandwidth();
		String hashmapkey = appkey + "*" + system;
		IndicatorBean indicatorBean = this.hashmap.get(hashmapkey);
		if (indicatorBean == null) {
			/** 如果HashMap没有查找不到key，那么作为新记录插入 */
			IndicatorBean newIndicatorBean = new IndicatorBean();
			newIndicatorBean.setCount(1);
			newIndicatorBean.setBandwidth(bandwidth);
			this.hashmap.put(hashmapkey, newIndicatorBean);
		} else {
			/** 如果HashMap已经存在这项key，修改此key对应的value */
			indicatorBean.setCount(indicatorBean.getCount() + 1);
			indicatorBean
					.setBandwidth(indicatorBean.getBandwidth() + bandwidth);
			this.hashmap.put(hashmapkey, indicatorBean);
		}
	}

}
