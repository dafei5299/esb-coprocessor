package cn.portal.esb.coproc.service.act;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import cn.portal.esb.coproc.mapper.main.NgxNodeDbMapper;
import cn.portal.esb.coproc.model.NgxNodeModel;
import cn.portal.esb.coproc.service.ConfigService;
import cn.portal.esb.coproc.service.NgxCacheLocalCluster;
import cn.portal.esb.coproc.service.PeriodTask;
import cn.portal.esb.coproc.util.MapperUtils;
import cn.portal.esb.coproc.zk.ZkClient;

@Service
public class DiscoverNgxNodeService extends PeriodTask {

	@Autowired
	private TaskScheduler scheduler;
	@Autowired
	private ZkClient zk;
	@Value("#{dataCenterAware.id()}")
	private long dataCenterID;
	@Value("#{dataCenterAware.coprocessorID()}")
	private long coprocessorID;
	@Autowired
	private ForkJoinPool pool;
	@Autowired
	private NgxCacheLocalCluster ngxLocalCluster;
	@Autowired
	private ConfigService configService;
	@Autowired
	private NgxNodeDbMapper ngxNodeDbMapper;
	@Value("${discover.period}")
	private int period;
	@Value("${discover.timeout.conn}")
	private int timeout; // 毫秒

	@PostConstruct
	public void init() {
		super.init(scheduler, zk);
		super.schedule("/control/discover/ngx/" + dataCenterID, period, 15,
				null, coprocessorID);
		
	}

	@PreDestroy
	public void destroy() {
		super.shutdown();
	}

	@Override
	protected void work(long min, long max, Object payload) {
		if (System.currentTimeMillis() - max > 3 * period)
			return;
		pool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					log.info("All ngxs: {}",ngxLocalCluster.ngxs());
					Map<NgxNodeModel, ForkJoinTask<NgxNodeModel>> ngxs = new HashMap<>();
					for (NgxNodeModel ngx : ngxLocalCluster.ngxs())
						ngxs.put(ngx, new AvailabilityChecker(ngx).fork());
					for (NgxNodeModel ngx : ngxLocalCluster.ngxs())
						apply(ngxs.get(ngx).join());
				} catch (Exception e) {
					log.warn("discover ngx task found error", e);
				}
			}
		});
	}

	@SuppressWarnings("serial")
	private class AvailabilityChecker extends ForkJoinTask<NgxNodeModel> {

		NgxNodeModel ngx;

		AvailabilityChecker(NgxNodeModel ngx) {
			this.ngx = ngx;
		}

		@Override
		public NgxNodeModel getRawResult() {
			return ngx;
		}

		@Override
		protected void setRawResult(NgxNodeModel value) {
			ngx = value;
		}

		@Override
		protected boolean exec() {
			InetSocketAddress endpoint = new InetSocketAddress(ngx.getHost(),
					ngx.getPort());
			try (Socket socket = new Socket()) {
				socket.setReuseAddress(true);
				socket.setTcpNoDelay(true);
				socket.setSoLinger(true, 0);
				socket.connect(endpoint, timeout);
				ngx.setAvailable(true);
			} catch (IOException e) {
				log.debug("ngx node {} connectivity abnormal: {}", endpoint,
						e.getMessage());
				ngx.setAvailable(false);
			}
			
			return true;
		}

	}

	private void apply(NgxNodeModel ngx) {
		String data = zk.data("/ngxs/" + ngx.getId());
		NgxNodeModel old = MapperUtils.fromJson(data, NgxNodeModel.class);
		log.debug("old ngxs: {}",old);
		log.debug("new ngxs: {}",ngx);
		log.debug("deepEquals: {}",old.deepEquals(ngx));
		if(!old.deepEquals(ngx)){
			// 更新zk
			configService.ngx(ngx);
		}
		// 更新db
		ngxNodeDbMapper.save(ngx);
	}
	
	
	public static void main(String[] args) {
		Date date = new Date();
		date.setTime(1380185200000L);
		System.out.println(date);
	}

}
