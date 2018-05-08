package cn.portal.esb.coproc.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:applicationContext.xml")
public class ConfigServiceTest {

	@Autowired
	private ConfigService configService;

	@Test
	public void system() {
		Assert.assertEquals(Action.CREATE, configService.system(5));
		Assert.assertEquals(Action.ALTER, configService.system(5));
		Assert.assertEquals(Action.NONE, configService.system(1));
	}

	@Test
	public void node() {
		Assert.assertEquals(Action.CREATE, configService.node(54));
		Assert.assertEquals(Action.ALTER, configService.node(54));
		Assert.assertEquals(Action.NONE, configService.node(1));
	}

	@Test
	public void group() {
		Assert.assertEquals(Action.CREATE, configService.group(4));
		Assert.assertEquals(Action.ALTER, configService.group(4));
		Assert.assertEquals(Action.NONE, configService.group(1));
	}

	@Test
	public void api() {
		Assert.assertEquals(Action.CREATE, configService.api(17));
		Assert.assertEquals(Action.ALTER, configService.api(17));
		Assert.assertEquals(Action.NONE, configService.api(1));
	}

	@Test
	public void source() {
		Assert.assertEquals(Action.CREATE, configService.source(29));
		Assert.assertEquals(Action.ALTER, configService.source(29));
		Assert.assertEquals(Action.NONE, configService.source(1));
	}

}
