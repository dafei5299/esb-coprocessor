package cn.portal.esb.coproc.zk;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cn.portal.esb.coproc.zk.ZkElection.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class ZkTest {

	@Value("${zk.endpoints}")
	private String endpoints;
	@Value("${zk.timeout}")
	private int timeout;
	@Autowired
	private ZkClient zk;
	@Autowired
	private ZkClient zk1;
	@Autowired
	private ZkClient zk2;

	@Before
	public void before() throws Exception {
		zk.ensureAbsence("/ticket");
		zk.ensureAbsence("/master");
		zk1 = new ZkClient(endpoints, timeout);
		zk2 = new ZkClient(endpoints, timeout);
	}

	@After
	public void after() throws Exception {
		zk1.close();
		zk2.close();
	}

	@Test
	public void ticket() throws Exception {
		zk.ensurePresence("/ticket");
		Assert.assertEquals(0, zk.takeTicket("/ticket", "id1").getSerial());
		Assert.assertEquals(1, zk1.takeTicket("/ticket", "id2").getSerial());
		Assert.assertEquals(2, zk2.takeTicket("/ticket", "id3").getSerial());
		Assert.assertEquals(3, zk.listTickets("/ticket").size());
		zk1.close();
		Assert.assertEquals(2, zk.listTickets("/ticket").size());
	}

	@Test
	public void election1() {
		ZkElection el = new ZkElection(zk, "/master", "user_1");
		el.start();
		Assert.assertEquals(State.LEADER, el.state());
		el.stop();
		Assert.assertEquals(State.PASSIVE, el.state());
	}

	@Test
	public void election2() throws InterruptedException {
		ZkElection e1 = newThread("user_1");
		Thread.sleep(100);
		ZkElection e2 = newThread("user_2");
		Thread.sleep(100);
		ZkElection e3 = newThread("user_3");
		Thread.sleep(1000);
		Assert.assertEquals(State.LEADER, e1.state());
		Assert.assertEquals(State.FOLLOWER, e2.state());
		Assert.assertEquals(State.FOLLOWER, e3.state());
		System.out.println("-----");
		e1.stop();
		Thread.sleep(1000);
		Assert.assertEquals(State.PASSIVE, e1.state());
		Assert.assertEquals(State.LEADER, e2.state());
		Assert.assertEquals(State.FOLLOWER, e3.state());
		System.out.println("-----");
		e2.stop();
		Thread.sleep(1000);
		Assert.assertEquals(State.PASSIVE, e1.state());
		Assert.assertEquals(State.PASSIVE, e2.state());
		Assert.assertEquals(State.LEADER, e3.state());
		System.out.println("-----");
		e1.start();
		Assert.assertEquals(State.FOLLOWER, e1.state());
		Assert.assertEquals(State.PASSIVE, e2.state());
		Assert.assertEquals(State.LEADER, e3.state());
		System.out.println("-----");
		e1.stop();
		e3.stop();
		Thread.sleep(1000);
		Assert.assertEquals(State.PASSIVE, e1.state());
		Assert.assertEquals(State.PASSIVE, e2.state());
		Assert.assertEquals(State.PASSIVE, e3.state());
	}

	private ZkElection newThread(String id) {
		final ZkElection e = new ZkElection(zk, "/master", id);
		new Thread() {
			public void run() {
				e.start();
			}
		}.start();
		return e;
	}

	@Test
	public void election3() throws Exception {
		ZkElection e1 = newThread(zk1, "user_1");
		Thread.sleep(100);
		ZkElection e2 = newThread(zk2, "user_2");
		Thread.sleep(100);
		ZkElection e3 = newThread(zk, "user_3");
		Thread.sleep(1000);
		Assert.assertEquals(State.LEADER, e1.state());
		Assert.assertEquals(State.FOLLOWER, e2.state());
		Assert.assertEquals(State.FOLLOWER, e3.state());
		System.out.println("-----");
		zk1.close();
		Thread.sleep(1000);
		Assert.assertEquals(State.FORBIDDEN, e1.state());
		Assert.assertEquals(State.LEADER, e2.state());
		Assert.assertEquals(State.FOLLOWER, e3.state());
		System.out.println("-----");
		zk2.close();
		Thread.sleep(1000);
		Assert.assertEquals(State.FORBIDDEN, e1.state());
		Assert.assertEquals(State.FORBIDDEN, e2.state());
		Assert.assertEquals(State.LEADER, e3.state());
		System.out.println("-----");
		zk.close();
		Thread.sleep(1000);
		Assert.assertEquals(State.FORBIDDEN, e1.state());
		Assert.assertEquals(State.FORBIDDEN, e2.state());
		Assert.assertEquals(State.FORBIDDEN, e3.state());
	}

	private ZkElection newThread(ZkClient zk, String id) {
		final ZkElection e = new ZkElection(zk, "/master", id);
		new Thread() {
			public void run() {
				e.start();
			}
		}.start();
		return e;
	}

}
