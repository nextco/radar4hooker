package gz.httpserver.controller;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import android.view.View;

public final class UISharedViewStore {

	private static final char[] ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
	private static final int ID_LENGTH = 8;
	private static final long VIEW_TTL_MS = 2 * 60 * 1000L;
	private static final long CLEANUP_INTERVAL_MS = 60 * 1000L;
	private static final Map<String, ViewEntry> VIEW_CACHE = new ConcurrentHashMap<String, ViewEntry>();
	private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "hooker-ui-view-store-cleaner");
			thread.setDaemon(true);
			return thread;
		}
	});

	static {
		CLEANUP_EXECUTOR.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				cleanupExpiredEntries();
			}
		}, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
	}

	private UISharedViewStore() {
	}

	public static String cache(View view) {
		if (view == null) {
			return null;
		}
		String id = "hooker_" + generateViewId();
		VIEW_CACHE.put(id, new ViewEntry(view));
		return id;
	}

	public static View get(String id) {
		if (id == null) {
			return null;
		}
		ViewEntry entry = VIEW_CACHE.get(id);
		if (entry == null) {
			return null;
		}
		if (entry.isExpired()) {
			VIEW_CACHE.remove(id);
			return null;
		}
		View view = entry.getView();
		if (view == null) {
			VIEW_CACHE.remove(id);
			return null;
		}
		return view;
	}

	private static void cleanupExpiredEntries() {
		for (Map.Entry<String, ViewEntry> item : VIEW_CACHE.entrySet()) {
			ViewEntry entry = item.getValue();
			if (entry == null || entry.isExpired() || entry.getView() == null) {
				VIEW_CACHE.remove(item.getKey());
			}
		}
	}

	private static String generateViewId() {
		char[] buffer = new char[ID_LENGTH];
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = ID_CHARS[random.nextInt(ID_CHARS.length)];
		}
		return new String(buffer);
	}

	private static final class ViewEntry {
		private final WeakReference<View> reference;
		private final long expiresAt;

		private ViewEntry(View view) {
			this.reference = new WeakReference<View>(view);
			this.expiresAt = System.currentTimeMillis() + VIEW_TTL_MS;
		}

		private View getView() {
			return reference.get();
		}

		private boolean isExpired() {
			return System.currentTimeMillis() > expiresAt;
		}
	}
}
