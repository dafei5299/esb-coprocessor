package cn.portal.esb.coproc.service.stat;

import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.redis.BrpopCallback;
import cn.portal.esb.coproc.redis.RedisCluster;
import cn.portal.esb.coproc.service.ConfigData;
import cn.portal.esb.coproc.util.MapperUtils;

@Service
public class ExtractTask implements BrpopCallback {

	private static final Logger log = LoggerFactory
			.getLogger(ExtractTask.class);
	@Autowired
	private RedisCluster redis;
	@Autowired
	private ConfigData configData;

	@PostConstruct
	public void init() {
		redis.brpop("portal_esb:statistics_queue", this);
	}

	@Override
	public void handle(String data) {
		// 从 portal_esb:statistics_queue 中提取消息 （List结构）
		// 初步加工后
		// 放入 esb:<system>:timeline 中 （Sorted Set结构，时间戳为score）
		RawData rd = MapperUtils.fromJson(data, RawData.class);
		if (rd == null)
			return;
		log.debug("extract json from queue: {}", data);
		PreparedData pd = new PreparedData();
		pd.setSystem(rd.getSystem());
		pd.setGroup(rd.getGroup());
		pd.setApi(rd.getApi());
		if (rd.getVersion() == 0) {
			int defaultVersion = configData.api(rd.getSystem(), rd.getGroup(),
					rd.getApi()).getVersion();
			rd.setVersion(defaultVersion);
		}
		pd.setVersion(rd.getVersion());
		pd.setSource(rd.getSource());
		pd.setTarget(rd.getTargetAddr() + ":" + rd.getTargetPort());
		pd.setSuccess(rd.getStatusCode() < 500 && rd.getStatusCode() > 0);
		pd.setContentLength(rd.getContentLength());
		pd.setRecordTime(rd.getRecordTime());
		pd.setProcessTime((int) (rd.getForwardRespTime() - rd
				.getForwardReqTime()));
		pd.setUuid(UUID.randomUUID().toString()); // 生成UUID，避免zset覆盖
		redis.zadd("esb:" + pd.getSystem() + ":timeline", pd.getRecordTime(),
				MapperUtils.toJson(pd));
	}

}
