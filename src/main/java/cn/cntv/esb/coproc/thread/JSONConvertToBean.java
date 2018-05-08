package cn.portal.esb.coproc.thread;

import cn.portal.esb.coproc.util.MapperUtils;

/**
 * JSONConvertToBean类功能：转化Redis中JSON数据为Object Author：liweichao
 */
public class JSONConvertToBean {

	/** 成员变量，表示要进行转化的json字符串 */
	private String jsonstr;

	/** 成员变量，类RedisToMySQLBean的实例，依赖关系 */
	public RedisToMySQLBean redisToMySQLBean;

	/**
	 * 构造函数重载
	 * 
	 * @param jsonStr
	 */
	public JSONConvertToBean(String jsonStr) {
		this.jsonstr = jsonStr;
		this.redisToMySQLBean = MapperUtils.fromJson(jsonStr,
				RedisToMySQLBean.class);
	}

	/**
	 * 将当前的redisToMySQLBean内的数据转换为带score的json数据为了适应Sorted Set数据结构
	 * 
	 * @return String
	 */
	public String BeanWithScore() {
		/** 给当前json前面加入RequestTime的值作为其score */
		String score = Long.toString(this.redisToMySQLBean.getRequestTime());
		return score + "#" + this.jsonstr;
	}

}
