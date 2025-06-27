package gz.radar.objects;

import android.content.res.Resources;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.View.OnHoverListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;
import gz.radar.AndroidApkField;
import gz.util.XView;

public class ViewInfo extends ObjectInfo {

	private int viewId;

	private String viewIdName = "";

	private String viewText;

	private boolean isVisible;

	public ViewInfo(View view) throws IllegalAccessException {
		super(view);
		this.viewId = view.getId();
		if (viewId != View.NO_ID) {
			try {
				this.viewIdName = view.getResources().getResourceEntryName(viewId);
			} catch (Resources.NotFoundException e) {
			}
		}
		XView xView = new XView(view);
		this.viewText = xView.getViewText();
		this.isVisible = xView.isVisible();
		View.OnClickListener onClickListener = xView.getOnClickListener();
		if (onClickListener != null) {
			String objectId = ObjectsStore.storeObject(onClickListener);
			addAndroidApkField(new AndroidApkField("mOnClickListener", onClickListener.getClass(), false, -1,
					onClickListener, objectId));
		}
		View.OnLongClickListener onLongClickListener = xView.getOnLongClickListener();
		if (onLongClickListener != null) {
			String objectId = ObjectsStore.storeObject(onLongClickListener);
			addAndroidApkField(new AndroidApkField("mOnLongClickListener", onLongClickListener.getClass(), false, -1,
					onLongClickListener, objectId));
		}
		View.OnTouchListener mOnTouchListener = xView.getOnTouchListener();
		if (mOnTouchListener != null) {
			String objectId = ObjectsStore.storeObject(mOnTouchListener);
			addAndroidApkField(new AndroidApkField("mOnTouchListener", mOnTouchListener.getClass(), false, -1,
					mOnTouchListener, objectId));
		}
		View.OnFocusChangeListener mOnFocusChangeListener = xView.getOnFocusChangeListener();
		if (mOnFocusChangeListener != null) {
			String objectId = ObjectsStore.storeObject(mOnFocusChangeListener);
			addAndroidApkField(new AndroidApkField("mOnFocusChangeListener", mOnFocusChangeListener.getClass(), false,
					-1, mOnFocusChangeListener, objectId));
		}
		TextView.OnEditorActionListener mOnEditorActionListener = xView.getOnEditorActionListener();
		if (mOnEditorActionListener != null) {
			String objectId = ObjectsStore.storeObject(mOnEditorActionListener);
			addAndroidApkField(new AndroidApkField("mOnEditorActionListener", mOnEditorActionListener.getClass(), false,
					-1, mOnFocusChangeListener, objectId));
		}

		Object mOnScrollChangeListener = xView.getOnScrollChangeListener();
		if (mOnScrollChangeListener != null) {
			String objectId = ObjectsStore.storeObject(mOnScrollChangeListener);
			addAndroidApkField(new AndroidApkField("mOnScrollChangeListener", mOnScrollChangeListener.getClass(), false,
					-1, mOnScrollChangeListener, objectId));
		}

		Object adapter = xView.getAdapter();
		if (adapter != null) {
			String objectId = ObjectsStore.storeObject(adapter);
			addAndroidApkField(new AndroidApkField("mAdapter", adapter.getClass(), false, -1, adapter, objectId));
		}

		OnScrollListener onScrollListener = xView.getOnScrollListener();
		if (onScrollListener != null) {
			String objectId = ObjectsStore.storeObject(onScrollListener);
			addAndroidApkField(new AndroidApkField("mOnScrollListener", onScrollListener.getClass(), false, -1,
					onScrollListener, objectId));
		}

		OnHoverListener mOnHoverListener = xView.getOnHoverListener();
		if (mOnHoverListener != null) {
			String objectId = ObjectsStore.storeObject(mOnHoverListener);
			addAndroidApkField(new AndroidApkField("mOnHoverListener", mOnHoverListener.getClass(), false, -1,
					mOnHoverListener, objectId));
		}

		OnDragListener mOnDragListener = xView.getOnDragListener();
		if (mOnDragListener != null) {
			String objectId = ObjectsStore.storeObject(mOnDragListener);
			addAndroidApkField(new AndroidApkField("mOnDragListener", mOnDragListener.getClass(), false, -1,
					mOnDragListener, objectId));
		}

		OnItemClickListener mOnItemClickListener = xView.getOnItemClickListener();
		if (mOnItemClickListener != null) {
			String objectId = ObjectsStore.storeObject(mOnItemClickListener);
			addAndroidApkField(new AndroidApkField("mOnItemClickListener", mOnItemClickListener.getClass(), false, -1,
					mOnItemClickListener, objectId));
		}

		OnItemLongClickListener mOnItemLongClickListener = xView.getOnItemLongClickListener();
		if (mOnItemLongClickListener != null) {
			String objectId = ObjectsStore.storeObject(mOnItemLongClickListener);
			addAndroidApkField(new AndroidApkField("mOnItemLongClickListener", mOnItemLongClickListener.getClass(),
					false, -1, mOnItemLongClickListener, objectId));
		}

		OnItemSelectedListener mOnItemSelectedListener = xView.getOnItemSelectedListener();
		if (mOnItemSelectedListener != null) {
			String objectId = ObjectsStore.storeObject(mOnItemSelectedListener);
			addAndroidApkField(new AndroidApkField("mOnItemSelectedListener", mOnItemSelectedListener.getClass(), false,
					-1, mOnItemSelectedListener, objectId));
		}

	}

	public String getViewId() {
		return viewIdName;
	}

	public String getViewIdName() {
		return viewIdName;
	}

	public String getViewText() {
		return viewText;
	}

	public boolean isVisible() {
		return isVisible;
	}

	@Override
	public String toString() {
		StringBuilder report = new StringBuilder();
		report.append("------------------View--------------------").append("\n");
		report.append("View Id: " + getViewId()).append("\n");
		report.append("View IdName: " + getViewIdName()).append("\n");
		report.append("View Text: " + getViewText()).append("\n");
		report.append("View Visible: " + isVisible()).append("\n");
		report.append("View Class: " + getName()).append("\n");
		report.append("View SuperClass: " + getSuperClazz()).append("\n");
		report.append("View ImplementInterfaces: " + getImplementInterfaces()).append("\n");
		View view = (View) obj;
		String bounds = view.getLeft() + "," + view.getTop() + "," + view.getRight() + ","
				+ view.getBottom();
		report.append("View Bounds: " +  bounds).append("\n");
		AndroidApkField[] androidApkFields = getAndroidApkFields();
		report.append("View Fields: " + androidApkFields.length).append("\n");
		for (int j = 0; j < androidApkFields.length; j++) {
			report.append("\t" + androidApkFields[j].toLine()).append("\n");
		}
		String[] methods = methods();
		report.append("View Methods: " + methods.length).append("\n");
		for (int j = 0; j < methods.length; j++) {
			report.append("\t" + methods[j]).append("\n");
		}
		return report.toString();
	}

}
