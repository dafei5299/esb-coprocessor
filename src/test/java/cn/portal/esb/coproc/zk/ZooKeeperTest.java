package cn.portal.esb.coproc.zk;

import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class ZooKeeperTest {

	@Value("${zk.endpoints}")
	private String endpoints;

	@Test
	public void zk1() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		ZooKeeper zk = new ZooKeeper(endpoints, 30000, new Watcher() {
			public void process(WatchedEvent event) {
				System.out.println(event);
				if (event.getState() == KeeperState.SyncConnected)
					latch.countDown();
			}
		});
		latch.await();
		System.out.println("session id: " + zk.getSessionId());
		System.out.println("session passwd: "
				+ new BigInteger(zk.getSessionPasswd()));
		zk.getData("/test", true, null);
		Thread.sleep(30000);
	}

	@Test
	public void zk2() throws Exception {
		long id = 161748033187020812L;
		byte[] passwd = new BigInteger(
				"16425302236926366166059355245636239934").toByteArray();
		new ZooKeeper(endpoints, 30000, new Watcher() {
			public void process(WatchedEvent event) {
				System.out.println(event);
			}
		}, id, passwd);
		Thread.sleep(30000);
	}

}
