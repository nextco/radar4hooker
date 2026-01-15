package gz.radar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import gz.util.Logger;

public class AndroidUI2 {

	private static Logger logger = new Logger(AndroidUI2.class);

	public static List<View> collectImportantViews(View root) {
		List<View> result = new ArrayList<>();
		traverse(root, result);
		return result;
	}

	private static void traverse(View v, List<View> out) {
		if (v == null)
			return;
		if (v.getVisibility() != View.VISIBLE)
			return;
		// ===== 重要控件判定 =====
		if (v instanceof Button || v instanceof TextView || v instanceof ImageView || v instanceof ImageButton
				|| v instanceof CheckBox || v instanceof SeekBar || isSwitch(v) || isViewPager(v) || isRecyclerView(v)
				|| isWebView(v)) {

			out.add(v);
		}

		// ===== 继续递归子 View =====
		if (v instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) v;
			for (int i = 0; i < group.getChildCount(); i++) {
				traverse(group.getChildAt(i), out);
			}
		}
	}

	private static boolean isSwitch(View v) {
		try {
			if (v instanceof android.widget.Switch)
				return true;
			return Class.forName("androidx.appcompat.widget.SwitchCompat").isInstance(v);
		} catch (Throwable ignore) {
		}
		return false;
	}

	private static boolean isViewPager(View v) {
		try {
			return Class.forName("androidx.viewpager.widget.ViewPager").isInstance(v)
					|| Class.forName("androidx.viewpager2.widget.ViewPager2").isInstance(v);
		} catch (Throwable ignore) {
		}
		return false;
	}

	private static boolean isRecyclerView(View v) {
		try {
			return Class.forName("androidx.recyclerview.widget.RecyclerView").isInstance(v);
		} catch (Throwable ignore) {
		}
		return false;
	}

	private static boolean isWebView(View v) {
		if (v instanceof android.webkit.WebView)
			return true;

		// 兼容某些 App 的自定义 WebView（抖音/微信等）
		try {
			if (v.getClass().getName().toLowerCase().contains("webview")) {
				return true;
			}
		} catch (Throwable ignore) {
		}
		return false;
	}

	public static void tryDismissByDialogFragment() throws Exception {
		Activity top = Android.getTopActivity();
		if (top == null)
			return;
		top.runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (dismissAndroidXDialogFragment(top))
					return;
				dismissPlatformDialogFragment(top);
			}
		});
	}
	
	static Field findField(Class<?> clz, String name) throws NoSuchFieldException {
	    Class<?> c = clz;
	    while (c != null) {
	        try {
	            Field f = c.getDeclaredField(name);
	            f.setAccessible(true);
	            return f;
	        } catch (NoSuchFieldException ignored) {
	            c = c.getSuperclass();
	        }
	    }
	    throw new NoSuchFieldException("No field " + name + " in " + clz);
	}

	static Method findMethod(Class<?> clz, String name, Class<?>... params) throws NoSuchMethodException {
	    Class<?> c = clz;
	    while (c != null) {
	        try {
	            Method m = c.getDeclaredMethod(name, params);
	            m.setAccessible(true);
	            return m;
	        } catch (NoSuchMethodException ignored) {
	            c = c.getSuperclass();
	        }
	    }
	    throw new NoSuchMethodException("No method " + name + " in " + clz);
	}

	@SuppressWarnings("unchecked")
	static List<Object> tryGetFragmentsByFields(Object fm) {
	    try {
	        // 1) 新版本：fm.mFragmentStore.mAdded
	        Field storeF = null;
	        try { storeF = findField(fm.getClass(), "mFragmentStore"); } catch (Throwable ignored) {}

	        if (storeF != null) {
	            Object store = storeF.get(fm);
	            if (store != null) {
	                try {
	                    Field addedF = findField(store.getClass(), "mAdded");
	                    Object added = addedF.get(store);
	                    if (added instanceof List) return (List<Object>) added;
	                } catch (Throwable ignored) {}
	            }
	        }

	        // 2) 老版本：fm.mAdded
	        try {
	            Field addedF2 = findField(fm.getClass(), "mAdded");
	            Object added2 = addedF2.get(fm);
	            if (added2 instanceof List) return (List<Object>) added2;
	        } catch (Throwable ignored) {}

	    } catch (Throwable ignored) {}

	    return null;
	}
	
	@SuppressWarnings("unchecked")
	static List<Object> tryGetFragmentsByMethod(Object fm) {
	    try {
	        Method m = findMethod(fm.getClass(), "getFragments");
	        Object r = m.invoke(fm);
	        return (List<Object>) r;
	    } catch (Throwable ignored) {
	        return null;
	    }
	}
	
	// AndroidX：dismiss 顶层 DialogFragment
	public static boolean dismissAndroidXDialogFragment(Activity a) {
	    try {
	        Class<?> faClz = Class.forName("androidx.fragment.app.FragmentActivity");
	        if (!faClz.isInstance(a)) return false;
	        Object fm = faClz.getMethod("getSupportFragmentManager").invoke(a);
	        if (fm == null) return false;
	        List<Object> fragments = tryGetFragmentsByMethod(fm);
	        if (fragments == null) fragments = tryGetFragmentsByFields(fm);
	        if (fragments == null || fragments.isEmpty()) return false;
	        Class<?> dfClz = Class.forName("androidx.fragment.app.DialogFragment");
	        // 从后往前找：更接近“顶层”
	        for (int i = fragments.size() - 1; i >= 0; i--) {
	            Object f = fragments.get(i);
	            if (f == null) continue;
	            if (dfClz.isInstance(f)) {
	                // 优先 dismissAllowingStateLoss，避免状态保存异常
	                try {
	                    f.getClass().getMethod("dismissAllowingStateLoss").invoke(f);
	                } catch (NoSuchMethodException e) {
	                    // 极少数版本只有 dismiss()
	                    f.getClass().getMethod("dismiss").invoke(f);
	                }
	                return true;
	            }
	        }
	    } catch (Throwable e) {
	        logger.warn(e);
	    }
	    return false;
	}



	// 原生 DialogFragment
	public static boolean dismissPlatformDialogFragment(Activity a) {
		try {
			Object fm = a.getFragmentManager();
			java.util.List<?> fragments = (java.util.List<?>) fm.getClass().getMethod("getFragments").invoke(fm);
			if (fragments == null)
				return false;

			Class<?> dfClz = Class.forName("android.app.DialogFragment");
			for (int i = fragments.size() - 1; i >= 0; i--) {
				Object f = fragments.get(i);
				if (f != null && dfClz.isInstance(f)) {
					android.app.DialogFragment df = null;
					dfClz.getMethod("dismissAllowingStateLoss").invoke(f);
					return true;
				}
			}
		} catch (Throwable e) {
			logger.warn(e);
		}
		return false;
	}

}
