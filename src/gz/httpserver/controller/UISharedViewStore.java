package gz.httpserver.controller;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import android.view.View;

public final class UISharedViewStore {

	private static final Map<String, WeakReference<View>> VIEW_CACHE = new ConcurrentHashMap<String, WeakReference<View>>();

	private UISharedViewStore() {
	}

	public static String cache(View view) {
		if (view == null) {
			return null;
		}
		String id = "hooker_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		VIEW_CACHE.put(id, new WeakReference<View>(view));
		return id;
	}

	public static View get(String id) {
		if (id == null) {
			return null;
		}
		WeakReference<View> ref = VIEW_CACHE.get(id);
		return ref == null ? null : ref.get();
	}
}
