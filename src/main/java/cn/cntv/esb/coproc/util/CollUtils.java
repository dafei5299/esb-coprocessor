package cn.portal.esb.coproc.util;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public abstract class CollUtils {

	public static Map<String, Object> map() {
		return ImmutableMap.of();
	}

	public static Map<String, Object> map(String k1, Object v1) {
		return ImmutableMap.of(k1, v1);
	}

	public static Map<String, Object> map(String k1, Object v1, String k2,
			Object v2) {
		return ImmutableMap.of(k1, v1, k2, v2);
	}

	public static Map<String, Object> map(String k1, Object v1, String k2,
			Object v2, String k3, Object v3) {
		return ImmutableMap.of(k1, v1, k2, v2, k3, v3);
	}

	public static Map<String, Object> map(String k1, Object v1, String k2,
			Object v2, String k3, Object v3, String k4, Object v4) {
		return ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4);
	}

	public static Map<String, Object> map(String k1, Object v1, String k2,
			Object v2, String k3, Object v3, String k4, Object v4, String k5,
			Object v5) {
		return ImmutableMap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
	}

}
