package cn.portal.esb.coproc.zk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class ZkClientTest {

	@Autowired
	private ZkClient zk;

	@Test
	public void ensureAbsence() {
		zk.ensureAbsence("/systems");
		zk.ensureAbsence("/groups");
		zk.ensureAbsence("/apis");
		zk.ensureAbsence("/nodes");
		zk.ensureAbsence("/sources");
	}

	@Test
	public void licence() throws InterruptedException {
		for (int i = 0; i < 4; i++) {
			new Thread("Thread-" + i) {
				public void run() {
					while (true) {
						long now = System.currentTimeMillis();
						long licence = zk.licence("/test", now / 1000 * 1000,
								1000);
						if (licence > now) {
							try {
								Thread.sleep(licence - now);
							} catch (InterruptedException e) {
								// ignore
							}
						}
						System.out.println(Thread.currentThread().getName()
								+ ": " + licence);
					}
				}
			}.start();
		}
		Thread.sleep(30000);
	}

}
