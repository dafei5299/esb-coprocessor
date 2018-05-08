package cn.portal.esb.coproc.ngx;

import java.io.Closeable;

public interface NgxCache extends Closeable {

	String get(String key);

	void set(String key, String value);

	void del(String key);

}
