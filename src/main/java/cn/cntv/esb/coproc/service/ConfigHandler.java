package cn.portal.esb.coproc.service;

public abstract class ConfigHandler<T> {

	public abstract boolean canHandle(Action action, Class<?> clazz);

	public void create(T current) {
		// do nothing
	}

	public void drop(T previous) {
		// do nothing
	}

	public void alter(T previous, T current) {
		// do nothing
	}

}
