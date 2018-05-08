package cn.portal.esb.coproc.thread;

import java.io.IOException;

import cn.portal.esb.coproc.redis.RedisCluster;

public class Inittest {

	public static void main(String[] args) throws IOException {
		String[] jsonarray = {
				"{\"Api\":\"add\",\"RedirectTime\":1368270020383,\"RequestTime\":1368270020383,\"RecordTime\":1368270020385,\"Status\":200,\"ResponseTime\":1368270020385,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":1000,\"System\":\"adddd\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a0\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270020384,\"RequestTime\":1368270020384,\"RecordTime\":1368270020386,\"Status\":200,\"ResponseTime\":1368270020386,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":2000,\"System\":\"adddd\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a0\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270020385,\"RequestTime\":1368270020385,\"RecordTime\":1368270020387,\"Status\":200,\"ResponseTime\":1368270020387,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":3000,\"System\":\"message\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a2\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270021383,\"RequestTime\":1368270021383,\"RecordTime\":1368270020385,\"Status\":200,\"ResponseTime\":1368270020385,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":1000,\"System\":\"adddd\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a0\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270021384,\"RequestTime\":1368270021384,\"RecordTime\":1368270020386,\"Status\":200,\"ResponseTime\":1368270020386,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":2000,\"System\":\"adddd\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a0\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270021385,\"RequestTime\":1368270021385,\"RecordTime\":1368270020387,\"Status\":200,\"ResponseTime\":1368270020387,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":3000,\"System\":\"message\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a2\",\"Group\":\"relation\"}",
				"{\"Api\":\"add\",\"RedirectTime\":1368270022383,\"RequestTime\":1368270022383,\"RecordTime\":1368270020385,\"Status\":200,\"ResponseTime\":1368270020385,\"TargetIP\":\"192.168.56.102\",\"TargetPort\":80,\"Bandwidth\":1000,\"System\":\"adddd\",\"Appkey\":\"4fa8bb4b6275e8801df986259a5b57a0\",\"Group\":\"relation\"}" };
		RedisCluster redis = new RedisCluster();
		String redisKey = "portal_esb:statistics_queue";
		for (String str : jsonarray) {
			redis.set(redisKey, str);
		}
		redis.destroy();
	}

}
