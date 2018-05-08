package cn.portal.esb.coproc.zk;

import java.util.List;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class ZkElection {

	public static enum State {
		PASSIVE, PROPOSAL, LEADER, FOLLOWER, FORBIDDEN
	}

	private static final Logger log = LoggerFactory.getLogger(ZkElection.class);
	private ZkClient zk;
	private String path;
	private String id;
	private State state;
	private ZkTicket me;

	public ZkElection(ZkClient zk, String path, String id) {
		this.zk = zk;
		this.path = path;
		this.id = id;
		this.state = State.PASSIVE;
		zk.ensurePresence(path);
		zk.addAware(new ConnectedAware() {
			@Override
			public void disconnect(Type type) {
				if (state != State.PASSIVE && state != State.FORBIDDEN)
					emit(State.FORBIDDEN);
				log.warn("zk disconnect {}, {} proposal for {} state {}", type,
						ZkElection.this.id, ZkElection.this.path, state);
			}

			@Override
			public void reconnect() {
				Assert.state(state == State.PASSIVE || state == State.FORBIDDEN);
				if (state == State.FORBIDDEN)
					start();
			}
		});
	}

	public synchronized void start() {
		Assert.state(state == State.PASSIVE || state == State.FORBIDDEN);
		revoke();
		try {
			me = zk.takeTicket(path, id); // 先领个号
			emit(State.PROPOSAL);
			log.info("{} proposal for {}", id, path);
			determine();
		} catch (ZkException e) {
			failure(e);
		}
	}

	public synchronized void stop() {
		Assert.state(state == State.LEADER || state == State.FOLLOWER);
		revoke();
		emit(State.PASSIVE);
	}

	public State state() {
		return state;
	}

	public String path() {
		return me != null ? me.getNode() : path;
	}

	private synchronized void determine() throws ZkException {
		Assert.state(state == State.PROPOSAL);
		List<ZkTicket> peers = zk.listTickets(path);
		int rank = peers.indexOf(me); // 看看我排第几个
		log.info("{} proposal for {}, rank {} of {} peers", id, path, rank,
				peers.size());
		if (rank == 0)
			leader(); // 排第一，则成为主
		else
			follower(peers.get(rank - 1)); // 排之后，则成为从，并紧盯前面一个人
	}

	private synchronized void failure(ZkException ex) {
		Assert.state(state == State.PASSIVE || state == State.FORBIDDEN);
		log.warn("{} proposal for {} failure", id, path, ex);
		revoke();
		emit(State.FORBIDDEN);
	}

	private void leader() {
		Assert.state(state == State.PROPOSAL);
		emit(State.LEADER);
		log.info("{} become leader for {}", id, path);
	}

	private void follower(ZkTicket rival) throws ZkException {
		Assert.state(state == State.PROPOSAL);
		boolean exists = zk.exists(rival.getNode(), new Watcher() {
			@Override
			public void process(WatchedEvent event) {
				// 注册观察事件，紧盯前面的人死没死
				if (event.getType() == EventType.NodeDeleted
						&& state == State.FOLLOWER) {
					try {
						emit(State.PROPOSAL);
						log.info("rival dead, {} re-proposal for {}", id, path);
						// 死了就上
						determine();
					} catch (ZkException e) {
						failure(e);
					}
				}
			}
		});
		if (exists) {
			emit(State.FOLLOWER);
			log.info("{} become follower for {}", id, path);
		} else {
			log.info("rival dead, {} re-proposal for {}", id, path);
			determine();
		}
	}

	private void emit(State state) {
		this.state = state;
	}

	private void revoke() {
		try {
			for (ZkTicket ticket : zk.listTickets(path))
				if (ticket.getData().equals(id))
					zk.remove(ticket.getNode());
		} catch (ZkException _) {
			// ignore
		}
	}

}
