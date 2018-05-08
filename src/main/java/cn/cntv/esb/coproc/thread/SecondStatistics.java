package cn.portal.esb.coproc.thread;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * SecondStatistics类功能：对第三个Redis队列中的数据进行二次统计 Author：liweichao
 */
public class SecondStatistics {

	/** 逐条统计的缓冲区 */
	public HashMap<String, IndicatorBean> hashmap;

	/** 构造函数重载 */
	public SecondStatistics() {
		this.hashmap = new HashMap<String, IndicatorBean>();
	}

	/**
	 * 根据指定的时间跨度统计数据
	 * 
	 * @param List
	 *            <String> data
	 */
	public void DoStatisticsByTimeSpan(List<String> timespanlist) {
		Iterator<String> iterator = timespanlist.iterator();
		while (iterator.hasNext()) {
			String[] eachdataarray = iterator.next().split("#");
			IndicatorBean indicatorBean = this.hashmap.get(eachdataarray[0]);
			if (indicatorBean == null) {
				/** 如果HashMap没有查找不到key，那么作为新记录插入 */
				IndicatorBean newIndicatorBean = new IndicatorBean();
				newIndicatorBean.setCount(1);
				newIndicatorBean.setBandwidth(Long.parseLong(eachdataarray[2]));
				this.hashmap.put(eachdataarray[0], newIndicatorBean);
			} else {
				/** 如果HashMap已经存在这项key，修改此key对应的value */
				indicatorBean.setCount(indicatorBean.getCount()
						+ Integer.parseInt(eachdataarray[1]));
				indicatorBean.setBandwidth(indicatorBean.getBandwidth()
						+ Long.parseLong(eachdataarray[2]));
				this.hashmap.put(eachdataarray[0], indicatorBean);
			}
		}
	}

}
