package cn.portal.esb.coproc.thread;

import java.util.Iterator;
import java.util.Map;

public class firststatisticstest {

	public static void main(String[] args) {
		String[] jsonarray = {
				"{\"Api\":\"add\",\"RedirectTime\":1368270020.383,\"RequestTime\":1368270020.383,\"RecordTime\":1368270020.385,\"Status\":200,\"ResponseTime\":1368270020.385,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":1000,\"System\":\"friends1\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a0\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270020.384,\"RequestTime\":1368270020.384,\"RecordTime\":1368270020.386,\"Status\":200,\"ResponseTime\":1368270020.386,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":2000,\"System\":\"friends1\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a1\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270020.385,\"RequestTime\":1368270020.385,\"RecordTime\":1368270020.387,\"Status\":200,\"ResponseTime\":1368270020.387,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":3000,\"System\":\"friends2\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a2\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270021.383,\"RequestTime\":1368270021.383,\"RecordTime\":1368270020.385,\"Status\":200,\"ResponseTime\":1368270020.385,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":1000,\"System\":\"friends1\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a0\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270021.384,\"RequestTime\":1368270021.384 \"RecordTime\":1368270020.386,\"Status\":200,\"ResponseTime\":1368270020.386,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":2000,\"System\":\"friends1\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a1\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270021.385,\"RequestTime\":1368270021.385,\"RecordTime\":1368270020.387,\"Status\":200,\"ResponseTime\":1368270020.387,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":3000,\"System\":\"friends2\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a2\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270022.383,\"RequestTime\":1368270022.383,\"RecordTime\":1368270020.385,\"Status\":200,\"ResponseTime\":1368270020.385,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":1000,\"System\":\"friends1\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a0\",\"Group\":\"relation\"}" };
		FirstStatistics statistics = new FirstStatistics();
		for (String str : jsonarray) {
			JSONConvertToBean jsonConvertToBean = new JSONConvertToBean(str);
			statistics.jsonConvertToBean = jsonConvertToBean;
			statistics.DoStatistics();
		}
		Iterator<Map.Entry<String, IndicatorBean>> iterator = statistics.hashmap
				.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, IndicatorBean> entry = (Map.Entry<String, IndicatorBean>) iterator
					.next();
			Object key = entry.getKey();
			IndicatorBean val = (IndicatorBean) entry.getValue();
			System.out.println(key + " # " + val.getCount() + " # "
					+ val.getBandwidth());
		}
	}

}
