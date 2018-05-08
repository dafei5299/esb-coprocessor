package cn.portal.esb.coproc.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.Test;

public class LatencyTest {

	@Test
	public void connect() throws IOException {
		InetSocketAddress endpoint = new InetSocketAddress("192.168.0.1", 80);
		long start = System.nanoTime();
		try (Socket socket = new Socket()) {
			socket.setReuseAddress(true);
			socket.setTcpNoDelay(true);
			socket.setSoLinger(true, 0);
			socket.connect(endpoint, 1000);
		}
		int latency = (int) (System.nanoTime() - start) / 1000;
		System.out.println(latency);
		System.out.println(weight(latency));
	}

	private int weight(int latency) {
		if (latency < 0)
			return 0;
		if (latency < 500)
			return 100;
		if (latency > 50000)
			return 1;
		return 50000 / latency;
	}

}
