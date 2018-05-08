package cn.portal.esb.coproc.zk;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.portal.esb.coproc.zk.ConnectedAware.Type;

import com.google.common.base.Charsets;

public class ZkClient implements Closeable {

	private static final Logger log = LoggerFactory.getLogger(ZkClient.class);
	private String endpoints;
	private int timeout;
	private volatile ZooKeeper zk;
	private Set<ConnectedAware> awares = new HashSet<>();

	public ZkClient(String endpoints, int timeout) throws ZkException {
		this.endpoints = endpoints;
		this.timeout = timeout;
		connect();
	}

	@Override
	public void close() throws IOException {
		try {
			if (zk != null)
				zk.close();
		} catch (InterruptedException _) {
			// ignore
		}
		for (ConnectedAware aware : awares)
			aware.disconnect(Type.SHUTDOWN);
	}

	public void addAware(ConnectedAware aware) {
		awares.add(aware);
	}

	private void connect() throws ZkException {
		try {
			if (zk != null)
				zk.close();
		} catch (InterruptedException _) {
			// ignore
		}
		try {
			final CountDownLatch assignLatch = new CountDownLatch(1);
			final CountDownLatch connectLatch = new CountDownLatch(1);
			zk = new ZooKeeper(endpoints, timeout, new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					try {
						log.info("zk {} event {}", endpoints, event.getState());
						assignLatch.await();
						switch (event.getState()) {
						case SyncConnected: // 已连接
							connectLatch.countDown();
							for (ConnectedAware aware : awares)
								aware.reconnect();
							break;
						case Expired: // 会话过期，手动重连
							for (ConnectedAware aware : awares)
								aware.disconnect(Type.EXPIRED);
							connect();
							break;
						case Disconnected: // 断开连接，自动重连
							for (ConnectedAware aware : awares)
								aware.disconnect(Type.DISCONNECTED);
							break;
						default:
							log.warn("zk {} unexpected event {}", endpoints,
									event.getState());
							break;
						}
					} catch (InterruptedException e) {
						throw new ZkException(e);
					}
				}
			});
			assignLatch.countDown();
			log.info("attempting to connect to zk {}", endpoints);
			connectLatch.await();
		} catch (IOException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	private String norm(String path) {
		if (path == null || path.isEmpty())
			return "/";
		if (!path.startsWith("/"))
			path = "/" + path;
		if (path.endsWith("/"))
			return path.substring(0, path.length() - 1);
		return path;
	}

	public boolean isConnected() {
		if (zk != null)
			return zk.getState().isConnected();
		return false;
	}

	public ZkClient ensurePresence(String path) throws ZkException {
		path = norm(path);
		try {
			if (!"/".equals(path) && zk.exists(path, false) == null) {
				ensurePresence(path.substring(0, path.lastIndexOf("/")));
				zk.create(path, new byte[0], Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
				log.info("create znode {}", path);
			}
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
		return this;
	}

	public ZkClient ensureAbsence(String path) throws ZkException {
		path = norm(path);
		try {
			if (zk.exists(path, false) != null) {
				for (String child : zk.getChildren(path, false))
					ensureAbsence(path + "/" + child);
				zk.delete(path, -1);
				log.info("delete znode {}", path);
			}
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
		return this;
	}

	public String data(String path) throws ZkException {
		path = norm(path);
		try {
			if (zk.exists(path, false) == null)
				return null;
			return new String(zk.getData(path, false, null), Charsets.UTF_8);
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	public void touch(String path) throws ZkException {
		path = norm(path);
		try {
			if (zk.exists(path, false) == null)
				return;
			Stat stat = new Stat();
			byte[] origin = zk.getData(path, false, stat);
			zk.setData(path, origin, stat.getVersion());
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	public boolean save(String path, String data) throws ZkException {
		path = norm(path);
		try {
			if (zk.exists(path, false) == null) {
				zk.create(path, data.getBytes(Charsets.UTF_8),
						Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				return true;
			}
			zk.setData(path, data.getBytes(Charsets.UTF_8), -1);
			return false;
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	public boolean remove(String path) throws ZkException {
		path = norm(path);
		try {
			if (zk.exists(path, false) == null)
				return false;
			zk.delete(path, -1);
			return true;
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	public boolean exists(String path, Watcher watcher) throws ZkException {
		path = norm(path);
		try {
			return zk.exists(path, watcher) != null;
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	public void removeChildren(String path) throws ZkException {
		path = norm(path);
		try {
			if (zk.exists(path, false) == null)
				return;
			for (String child : zk.getChildren(path, false))
				zk.delete(path + "/" + child, -1);
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	public List<String> children(String path) throws ZkException {
		path = norm(path);
		try {
			List<String> result = new ArrayList<>();
			for (String child : zk.getChildren(path, false))
				result.add(new String(zk.getData(path + "/" + child, false,
						null), Charsets.UTF_8));
			return result;
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	// 注册观察children变化事件，首次获取及每次变更，都会调用回调函数
	public void watchChildren(final String path, final ChildrenUpdater updater)
			throws ZkException {
		try {
			updater.onChange(zk.getChildren(norm(path), new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					if (event.getType() == EventType.NodeChildrenChanged)
						watchChildren(path, updater);
				}
			}));
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	// 注册观察节点数据变化事件，首次获取及每次变更，都会调用回调函数
	public void watchNodeData(final String path, final NodeDataUpdater updater)
			throws ZkException {
		try {
			updater.onChange(new String(zk.getData(norm(path), new Watcher() {
				@Override
				public void process(WatchedEvent event) {
					if (event.getType() == EventType.NodeDataChanged)
						watchNodeData(path, updater);
				}
			}, null), Charsets.UTF_8));
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	// 抢定时任务的令牌
	public long licence(String path, long initial, int increment)
			throws ZkException {
		path = norm(path);
		try {
			// 不存在，直接获取令牌
			if (zk.exists(path, false) == null) {
				try {
					zk.create(path, String.valueOf(initial).getBytes(),
							Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					return initial;
				} catch (KeeperException.NodeExistsException e) {
					// ignore
				}
			}
			while (true) {
				try {
					Stat stat = new Stat();
					// 取当前值
					long origin = Long.parseLong(new String(zk.getData(path,
							false, stat)));
					// 递增到下一个周期
					long licence = origin + increment;
					// 写入zk，stat中记录了取值时的版本号，更新时会验证版本号没有变化
					// 此即乐观锁实现
					zk.setData(path, String.valueOf(licence).getBytes(),
							stat.getVersion());
					return licence;
				} catch (KeeperException.BadVersionException e) {
					// 版本号不正确则忽略错误，重新循环
					// ignore
				}
			}
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	public ZkTicket takeTicket(String path, String data) throws ZkException {
		path = norm(path);
		try {
			// Leader选举的竞选动作，节点为EPHEMERAL_SEQUENTIAL类型
			String node = zk.create(path + "/", data.getBytes(Charsets.UTF_8),
					Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
			int serial = Integer
					.parseInt(node.substring(node.lastIndexOf("/") + 1));
			return new ZkTicket(node, data, serial, zk.getSessionId());
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

	public List<ZkTicket> listTickets(String path) throws ZkException {
		path = norm(path);
		try {
			List<ZkTicket> tickets = new ArrayList<>();
			for (String child : zk.getChildren(path, false)) {
				String node = path + "/" + child;
				int serial = Integer.parseInt(child);
				Stat stat = new Stat();
				String data = new String(zk.getData(node, false, stat),
						Charsets.UTF_8);
				long owner = stat.getEphemeralOwner();
				tickets.add(new ZkTicket(node, data, serial, owner));
			}
			Collections.sort(tickets);
			return tickets;
		} catch (KeeperException | InterruptedException e) {
			throw new ZkException(e);
		}
	}

}
