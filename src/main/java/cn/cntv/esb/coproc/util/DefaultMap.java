package cn.portal.esb.coproc.util;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({ "serial", "unchecked" })
public class DefaultMap<K, V> extends HashMap<K, V> {

	public interface Callable<V> {
		V call();
	}

	private Callable<V> defaultValue;

	public DefaultMap(Callable<V> defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public V get(Object key) {
		if (!containsKey(key))
			put((K) key, defaultValue.call());
		return super.get(key);
	}

	public static Map<String, Integer> si() {
		return new DefaultMap<>(new Callable<Integer>() {
			public Integer call() {
				return 0;
			}
		});
	}

	public static Map<Integer, Integer> ii() {
		return new DefaultMap<>(new Callable<Integer>() {
			public Integer call() {
				return 0;
			}
		});
	}

	public static Map<String, Map<Integer, Integer>> sii() {
		return new DefaultMap<>(new Callable<Map<Integer, Integer>>() {
			public Map<Integer, Integer> call() {
				return ii();
			}
		});
	}

	public static Map<String, Map<String, Map<Integer, Integer>>> ssii() {
		return new DefaultMap<>(
				new Callable<Map<String, Map<Integer, Integer>>>() {
					@Override
					public Map<String, Map<Integer, Integer>> call() {
						return sii();
					}
				});
	}

}
