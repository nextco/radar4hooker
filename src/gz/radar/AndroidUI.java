package gz.radar;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;

import gz.radar.ViewXmlDumper.XmlDumpResult;
import gz.radar.objects.ViewInfo;
import gz.util.X;
import gz.util.XLog;

public class AndroidUI {

    private static final Thread keepScreenOnThread = new Thread(){

        private Set<Class> activityFlags = new HashSet<>();

        @Override
        public void run() {
            while (true) {
                try {
                    final Activity activity = Android.getTopActivity();
                    if (activity != null && !activityFlags.contains(activity.getClass())) {
                        activityFlags.add(activity.getClass());
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Window window = activity.getWindow();
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                View view = activity.getWindow().getDecorView();
                                view.setKeepScreenOn(true);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(3 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public final synchronized static void keepScreenOn() {
       if (keepScreenOnThread.isAlive()) {
            return;
       }
        keepScreenOnThread.start();
    }
    
    public static void swipeToNext(ViewPager vp) {
    	vp.post(new Runnable() {
			
			@Override
			public void run() {
				int cur = vp.getCurrentItem();
		        PagerAdapter adapter = vp.getAdapter();
		        if (adapter == null) return ;

		        int count = adapter.getCount();
		        if (cur + 1 < count) {
		            vp.setCurrentItem(cur + 1, true);
		            return;
		        }
			}
		});
        
    }

    public static void swipeToPrev(ViewPager vp) {
    	vp.post(new Runnable() {
			
			@Override
			public void run() {
				int cur = vp.getCurrentItem();
		        if (cur > 0) {
		            vp.setCurrentItem(cur - 1, true);
		        }
			}
		});
    }
    


    /**
     * 滑动
     * @param x
     * @param y
     * @param stepLength
     */
    public static void hover(final float x,final float y,final int stepLength) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Instrumentation iso= new Instrumentation();
                iso.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_DOWN, x, y, 0));
                iso.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis(),MotionEvent.ACTION_MOVE, x, y, 0));
                iso.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis()+20,MotionEvent.ACTION_MOVE, x, y-30*stepLength, 0));
                iso.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis()+40,MotionEvent.ACTION_MOVE, x, y-60*stepLength, 0));
                iso.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis()+60,MotionEvent.ACTION_MOVE, x, y-90*stepLength, 0));
                iso.sendPointerSync(MotionEvent.obtain(SystemClock.uptimeMillis(),SystemClock.uptimeMillis()+60,MotionEvent.ACTION_UP, x, y-90*stepLength, 0));
            }
        };
        if (Thread.currentThread().getId() <= 2) {
            new Thread(runnable).start();
        }else{
            runnable.run();
        }
    }
    
    public static void showToast(final String text) throws Exception {
    	Android.getTopActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					Toast.makeText(Android.getApplication(), text, Toast.LENGTH_LONG).show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
    }
    
    public static View getRootViewGroup() throws Exception {
    	return Android.getTopActivity().getWindow().getDecorView();
    }
    
    public static View findViewByIdName(String idName) throws Exception {
        Application application = Android.getApplication();
        Resources resources =  Android.getApplication().getResources();
        int id = resources.getIdentifier(idName, "id", application.getPackageName());
        if (id == 0 ) {
            throw new Exception("There is no such id");
        }
        return findViewById(id);
    }

    public static View findViewById(int id) throws Exception {
        Activity activity = Android.getTopActivity();
        View view = activity.findViewById(id);
        if (view != null) {
        	return view;
        }
        List fragments = getFragments();
        if (fragments != null) {
        	for (Object fragment : fragments) {
            	try {
    				View fragmentView = (View) X.invokeObject(fragment, "getView");
    				view = fragmentView.findViewById(id);
    				if (view != null) {
    		        	return view;
    		        }
    			} catch (Exception e) {
    				XLog.appendText(e);
    			}
            }
        }
        return null;
    }

    public static List getFragments() {
    	try {
        	Object fm = X.invokeObject(Android.getTopActivity(), "getSupportFragmentManager");
        	List fragments = (List) X.invokeObject(fm, "getFragments");
        	return fragments;
        }catch(Exception e) {
        }
    	return null;
    }

    public static boolean clickById(int id) throws Exception {
        View view = findViewById(id);
        return performClick(view);
    }

    public static void search() throws Exception {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Instrumentation instrumentation = new Instrumentation();
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_SEARCH);
            }
        };
        if (Thread.currentThread().getId() <= 2) {
            new Thread(runnable).start();
        }else{
            runnable.run();
        }
    }

    public static void searchText(final EditText editText, final String text) throws Exception {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                editText.setText(text);
            }
        };
        Thread currentThread = Thread.currentThread();
        if (currentThread.getId() <= 2) {
            runnable.run();
        }else{
            editText.post(runnable);
        }
        Thread.sleep(500);
        Runnable sendAction = new Runnable() {
            @Override
            public void run() {
            	editText.onEditorAction(EditorInfo.IME_ACTION_SEARCH);
                //editText.performAccessibilityAction(EditorInfo.IME_ACTION_SEARCH, null);
            }
        };
        if (currentThread.getId() <= 2) {
            sendAction.run();
        }else{
            editText.post(sendAction);
        }
    }

    public static void searchText(int editTextId, String text) throws Exception {
        searchText((EditText) findViewById(editTextId), text);
    }

    public static void searchText(String editTextIdName, String text) throws Exception {
        searchText((EditText) findViewByIdName(editTextIdName), text);
    }

    public static < T extends View> List<T> collectViews(View containView, Class<T> tClass) throws Exception {
        List<T> list = new ArrayList<>();
        Class<?> viewClass = containView.getClass();
        if (viewClass.getName().equals(tClass.getName()) || tClass.isAssignableFrom(viewClass)) {
            list.add((T) containView);
            return list;
        }
        if (containView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) containView;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childView = viewGroup.getChildAt(i);
                List<T> listResult = collectViews(childView, tClass);
                if (!listResult.isEmpty()) {
                    list.addAll(listResult);
                }
            }
        }
        return list;
    }

    public static void startActivity(String activityName) throws Exception {
        //contextStartActivity(activityName);
        topActivityStartActivity(activityName);
    }

    public static void contextStartActivity(String activityName) throws Exception {
        Activity activity = Android.getTopActivity();
        if (activity.getClass().getName().equals(activityName)) {
            return;
        }
        Application application = Android.getApplication();
        Class<? extends Activity> activityClass = (Class<? extends Activity>) Class.forName(activityName);
        Intent intent = new Intent(application, activityClass);
        application.startActivity(intent);
    }

    public static void contextStartActivityForNewTask(String activityName) throws Exception {
        Activity activity = Android.getTopActivity();
        if (activity.getClass().getName().equals(activityName)) {
            return;
        }
        Application application = Android.getApplication();
        Class<? extends Activity> activityClass = (Class<? extends Activity>) Class.forName(activityName);
        Intent intent = new Intent(application, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        application.startActivity(intent);
    }

    public static void topActivityStartActivity(String activityName) throws Exception {
        Activity activity = Android.getTopActivity();
        if (activity.getClass().getName().equals(activityName)) {
            return;
        }
        Class<? extends Activity> activityClass = (Class<? extends Activity>) Class.forName(activityName);
        Intent intent = new Intent(activity, activityClass);
        activity.startActivity(intent);
    }
    
    public static void startActivity(String className, Bundle extras, String type) throws Exception {
        Intent intent = new Intent();
        Application ctx = Android.getApplication();
        intent.setClassName(ctx, className);
        if (type != null) {
        	intent.setType(type);
        }
        if (extras != null) {
            intent.putExtras(extras);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }


    public static void finishCurrentActivity() throws Exception {
        final Activity activity = Android.getTopActivity();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                activity.finish();
            }
        };
        activity.runOnUiThread(runnable);
    }

    public static void back() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Instrumentation instrumentation = new Instrumentation();
                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        if (Thread.currentThread().getId() <= 2) {
            new Thread(runnable).start();
        }else{
            runnable.run();
        }
    }

    public static void home() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Instrumentation instrumentation = new Instrumentation();
                    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        if (Thread.currentThread().getId() <= 2) {
            new Thread(runnable).start();
        }else{
            runnable.run();
        }
    }
    
    private static XmlDumpResult getXmlDumpResult() throws Exception {
    	Activity activity = Android.getTopActivity();
        return ViewXmlDumper.viewToXml(activity.getWindow().getDecorView());
    }

    public static String viewTree() throws Exception {
        Document document = getXmlDumpResult().getDocument();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter writer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no"); // 可选，是否省略头
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");              // 可选，是否缩进
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        String xmlString = writer.toString();
        return xmlString;
    }
    
    public static void listImportantViews() throws Exception {
        Document document = getXmlDumpResult().getDocument();
    	XPath xPath = XPathFactory.newInstance().newXPath();
    	//NodeList nodes = (NodeList) xPath.evaluate("//node[@class='xxx']", doc, XPathConstants.NODESET);
    }
    
    public static String getAttributeByKey(String key, Node node) {
        if (node == null || key == null) return null;

        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            Node attr = attributes.getNamedItem(key);
            if (attr != null) {
                return attr.getNodeValue();
            }
        }
        return null; // 没找到
    }

    public static List<View> findViewsByXpath(String xpath) throws Exception {
    	XmlDumpResult xmlDumpResult = getXmlDumpResult();
    	Document document = xmlDumpResult.getDocument();
    	XPath xPath = XPathFactory.newInstance().newXPath();
    	NodeList nodes = (NodeList) xPath.evaluate(xpath, document, XPathConstants.NODESET);
    	List<View> results = new ArrayList<View>();
    	for(int i = 0; nodes != null && i < nodes.getLength(); i ++) {
    		Node node = nodes.item(i);
    		String hashCode = getAttributeByKey("hash_code", node);
    		View view = xmlDumpResult.getView(hashCode);
    		if (view != null) {
    			results.add(view);
    		}
    	}
    	return results;
    }
    
    public static String showViews(String xpath) throws Exception {
    	List<View> views = findViewsByXpath(xpath);
    	StringBuilder info = new StringBuilder();
    	for (View view : views) {
    		info.append(new ViewInfo(view).toString());
    	}
    	return info.toString();
    }

    public static boolean clickByText(String text) throws Exception {
        return clickByText(text, false ,false);
    }

    public static boolean clickByText(String text, boolean mustBeTextEqueal, boolean mustBeVisible) throws Exception {
        Activity activity = Android.getTopActivity();
        if (activity == null) {
            return false;
        }
        View decorView = activity.getWindow().getDecorView();
        View view = findViewByText(decorView, text, mustBeTextEqueal, mustBeVisible);
        return performClick(view);
    }
    
    public static boolean performClick(View view) {
    	 if (view  != null) {
             while (true) {
                 if (view.isClickable()) {
                     final View clickableView = view;
                     Runnable runnable = new Runnable() {
                         @Override
                         public void run() {
                             clickableView.performClick();
                         }
                     };
                     clickableView.post(runnable);
                     return true;
                 }
                 view = (View) view.getParent();
                 if (view == null) {
                     break;
                 }
             }
         }
    	 return false;
    }

    public static <T extends View> T findViewByText(View decorView, String text) {
        return findViewByText(decorView, text, false, false);
    }

    public static <T extends View> T findViewByText(View decorView, String text, boolean mustBeTextEqueal, boolean mustBeVisible) {
        if (mustBeVisible && decorView.getVisibility() != View.VISIBLE) {
            return null;
        }
        if (decorView instanceof TextView && !(decorView instanceof EditText)) {
            TextView textView = ((TextView) decorView);
            String textViewText = textView.getText().toString().trim();
            if (mustBeTextEqueal && textViewText.equals(text)) {
                return (T) textView;
            }else if (textViewText.contains(text)) {
                return (T) textView;
            }
        }else if (decorView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) decorView;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childView = viewGroup.getChildAt(i);
                TextView textView = findViewByText(childView, text);
                if (textView != null) {
                    return (T) textView;
                }
            }
        }
        return null;
    }
    
    public static List<View> findViewsById(View decorView, int id) {
    	List<View> views = new ArrayList<View>();
    	if (decorView.getId() == id) {
        	views.add(decorView);
        	return views;
        }
    	if (decorView instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) decorView;
            int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childView = viewGroup.getChildAt(i);
                views.addAll(findViewsById(childView, id));
            }
        }
    	return views;
    }

}
