package cn.portal.esb.coproc.ngx;

import org.junit.Assert;
import org.junit.Test;

public class NgxCacheTest {

	@Test
	public void set_get_del() throws Exception {
		try (NgxCache ngx = new NgxCacheCluster(
				"dev01.esb.portal.cn:8080, 192.168.1.11:8080", 100, 100, 3, 100)) {
			ngx.set("foo", "bar");
			Thread.sleep(500);

			String resp1 = ngx.get("foo");
			Assert.assertEquals("bar", resp1);

			ngx.del("foo");
			Thread.sleep(500);

			String resp2 = ngx.get("foo");
			Assert.assertEquals(null, resp2);
		}
	}

}
