package ti.modules.titanium.ui.widget.tabgroup;

import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.TabGroupProxy;
import ti.modules.titanium.ui.TabProxy;
import android.app.Activity;

public abstract class TiUIAbstractTabGroup extends TiUIView {

	public TiUIAbstractTabGroup(TabGroupProxy proxy, Activity activity) {
		super(proxy);
	}

	/**
	 * Add the provided tab to this group.
	 */
	public abstract void addTab(TabProxy tabProxy);

	/**
	 * Remove the tab from this group.
	 *
	 * @param tab the tab to remove from the group
	 */
	public abstract void removeTab(TabProxy tabProxy);

	/**
	 * Changes the selected tab of the group.
	 *
	 * @param tab the tab that will become selected
	 */
	public abstract void selectTab(TabProxy tabProxy);

	/**
	 * Returns the currently selected tab.
	 */
	public abstract TabProxy getSelectedTab();

}
