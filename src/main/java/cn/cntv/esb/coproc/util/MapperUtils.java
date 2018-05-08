package cn.portal.esb.coproc.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.SerializationUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public abstract class MapperUtils {

	private static ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
	}

	public static <T> String toJson(T obj) {
		try {
			if (obj == null)
				return null;
			return mapper.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		try {
			if (json == null || json.isEmpty())
				return null;
			return mapper.readValue(json, clazz);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> fromJson(String json) {
		try {
			if (json == null || json.isEmpty())
				return null;
			return mapper.readValue(json, Map.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T copy(T obj) {
		return (T) SerializationUtils.deserialize(SerializationUtils
				.serialize(obj));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Object toMap(final Object obj, final List<String> ignorePaths) {
		if (obj == null)
			return null;
		Class clazz = obj.getClass();
		if (ClassUtils.isPrimitiveOrWrapper(clazz) || clazz.isEnum()
				|| CharSequence.class.isAssignableFrom(clazz)
				|| Number.class.isAssignableFrom(clazz)
				|| Date.class.isAssignableFrom(clazz))
			return obj;
		if (Collection.class.isAssignableFrom(clazz)) {
			Collection col = (Collection) BeanUtils.instantiate(clazz);
			for (Object o : (Collection) obj)
				col.add(toMap(o, ignorePaths));
			return col;
		}
		final Map<String, Object> map = new LinkedHashMap<>();
		ReflectionUtils.doWithFields(clazz, new FieldCallback() {
			@Override
			public void doWith(Field field) throws IllegalArgumentException,
					IllegalAccessException {
				String property = field.getName();
				if (ignorePaths != null)
					for (String ignorePath : ignorePaths)
						if (ignorePath.equals(property))
							return;
				ReflectionUtils.makeAccessible(field);
				Object o = ReflectionUtils.getField(field, obj);
				List<String> ip = new ArrayList<>();
				for (String ignorePath : ignorePaths)
					if (ignorePath.startsWith(property + "."))
						ip.add(ignorePath.substring(ignorePath.indexOf(".") + 1));
				map.put(property, toMap(o, ip));
			}
		});
		return map;
	}

}
