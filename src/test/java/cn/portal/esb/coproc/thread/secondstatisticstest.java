package cn.portal.esb.coproc.thread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class secondstatisticstest {

	public static void main(String[] args) {
		String[] teststr = {
				"4fa8bb4b6275e8801df986259a5b57a0*friends1#2#3000",
				"4fa8bb4b6275e8801df986259a5b57a2*friends2#1#3000",
				"4fa8bb4b6275e8801df986259a5b57a0*friends1#2#3000",
				"4fa8bb4b6275e8801df986259a5b57a2*friends2#1#3000",
				"4fa8bb4b6275e8801df986259a5b57a0*friends1#1#1000" };
		List<String> testList = new ArrayList<String>();
		for (String str : teststr) {
			testList.add(str);
		}
		SecondStatistics secondStatistic = new SecondStatistics();
		secondStatistic.DoStatisticsByTimeSpan(testList);
		Iterator<Map.Entry<String, IndicatorBean>> iterator = secondStatistic.hashmap
				.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, IndicatorBean> entry = (Map.Entry<String, IndicatorBean>) iterator
					.next();
			Object hashkey = entry.getKey();
			IndicatorBean indicator = (IndicatorBean) entry.getValue();
			System.out.println(hashkey.toString() + "&&" + indicator.getCount()
					+ "&&" + indicator.getBandwidth());
		}
	}

}
