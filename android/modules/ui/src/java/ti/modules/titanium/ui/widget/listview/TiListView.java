package ti.modules.titanium.ui.widget.listview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.R;
import android.app.Activity;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class TiListView extends TiUIView {

	private ListView listView;
	private TiBaseAdapter adapter;
	private ArrayList<ListSectionProxy> sections;
	private AtomicInteger itemTypeCount;
	private String defaultTemplateBinding;
	private HashMap<String, TiTemplate> templatesByBinding;
	private View headerView;
	private View footerView;
	
	public static final int HEADER_FOOTER_ITEM_TYPE = 2;

	public class TiBaseAdapter extends BaseAdapter {

		Activity context;
		public LayoutInflater inflater;
		public TiBaseAdapter(Activity activity) {
			context = activity;
			inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			int count = 0;
			for (int i = 0; i < sections.size(); i++) {
				ListSectionProxy section = sections.get(i);
				count += section.getItemCount();
			}
			return count;
		}

		@Override
		public Object getItem(int arg0) {
			return arg0;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		public int getViewTypeCount() {
			return 10;
			
		}
		@Override
		public int getItemViewType(int position) {
			Pair<ListSectionProxy, Integer> info = getSectionInfoByEntryIndex(position);
			ListSectionProxy section = info.first;
			int index = info.second;
			if (section.isHeaderView(index) || section.isFooterView(index))
				return HEADER_FOOTER_ITEM_TYPE;
			return section.getTemplateByIndex(index).getType();
			
			
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//Get section info from index
			Pair<ListSectionProxy, Integer> info = getSectionInfoByEntryIndex(position);
			ListSectionProxy section = info.first;
			int index = info.second;
			View content = convertView;

			//Handling section header/footer titles
			if (section.isHeaderView(index) || section.isFooterView(index)) {
				if (content == null) {
					content = inflater.inflate(R.layout.list_header_or_footer, null);
				}
				TextView title = (TextView)content.findViewById(R.id.title);
				title.setText(section.getHeaderOrFooterTitle(index));
				return content;
			}
			
			//Handling templates
			KrollDict data = section.getEntryProperties(index);
			TiTemplate template = section.getTemplateByIndex(index);
			
			if (content != null) {
				content = (TiBaseListViewItem) convertView;
				section.populateViews(data, (TiBaseListViewItem) content, template);
				Log.d("GetView", "reusing View");
			} else {
				Log.d("GetView", "generating View");
				content = section.generateCellContent(index, data, template);
			}

			return content;
		}

	}

	public TiListView(TiViewProxy proxy, Activity activity) {
		super(proxy);
		
		//initializing variables
		sections = new ArrayList<ListSectionProxy>();
		itemTypeCount = new AtomicInteger(3);
		templatesByBinding = new HashMap<String, TiTemplate>();
		
		//initializing listView and adapter
		listView = new ListView(activity);
		adapter = new TiBaseAdapter(activity);
		
		getLayoutParams().autoFillsHeight = true;
		getLayoutParams().autoFillsWidth = true;

		
		setNativeView(listView);
	}
	
	public void setHeaderTitle(String title) {
		TextView textView = (TextView) headerView.findViewById(R.id.title);
		textView.setText(title);
		if (textView.getVisibility() == View.GONE) {
			textView.setVisibility(View.VISIBLE);
		}
	}
	
	public void setFooterTitle(String title) {
		TextView textView = (TextView) footerView.findViewById(R.id.title);
		textView.setText(title);
		if (textView.getVisibility() == View.GONE) {
			textView.setVisibility(View.VISIBLE);
		}
	}
	
	public void processProperties(KrollDict d) {

		if (d.containsKey(TiC.PROPERTY_SECTIONS)) {
			processSections((Object[])d.get(TiC.PROPERTY_SECTIONS));
		}
		
		if (d.containsKey(TiC.PROPERTY_TEMPLATES)) {
			Object templates = d.get(TiC.PROPERTY_TEMPLATES);
			if (templates != null) {
				processTemplates(new KrollDict((HashMap)templates));
			}
		} 
		
		if (d.containsKey(TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE)) {
			defaultTemplateBinding = TiConvert.toString(d, TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE);
		}
		
		if (d.containsKey(TiC.PROPERTY_HEADER_TITLE)) {
			headerView = adapter.inflater.inflate(R.layout.list_header_or_footer, null);
			setHeaderTitle(TiConvert.toString(d, TiC.PROPERTY_HEADER_TITLE));
		}
		
		if (d.containsKey(TiC.PROPERTY_FOOTER_TITLE)) {
			footerView = adapter.inflater.inflate(R.layout.list_header_or_footer, null);
			setFooterTitle(TiConvert.toString(d, TiC.PROPERTY_FOOTER_TITLE));
		}

		//Check to see if headerTitle and footerTitle are specified. If not, we hide the views
		if (headerView == null) {
			headerView = adapter.inflater.inflate(R.layout.list_header_or_footer, null);
			headerView.findViewById(R.id.title).setVisibility(View.GONE);
		}
		
		if (footerView == null) {
			footerView = adapter.inflater.inflate(R.layout.list_header_or_footer, null);
			footerView.findViewById(R.id.title).setVisibility(View.GONE);
		}

		//Have to add header and footer before setting adapter
		listView.addHeaderView(headerView);
		listView.addFooterView(footerView);

		listView.setAdapter(adapter);

		super.processProperties(d);
		
	}
	
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {

		if (key.equals(TiC.PROPERTY_HEADER_TITLE)) {
			setHeaderTitle(TiConvert.toString(newValue));
		}
		
		if (key.equals(TiC.PROPERTY_FOOTER_TITLE)) {
			setFooterTitle(TiConvert.toString(newValue));
		}
	}

	protected void processTemplates(KrollDict templates) {
		for (String key : templates.keySet()) {
			//Here we bind each template with a key so we can use it to look up later
			KrollDict properties = new KrollDict((HashMap)templates.get(key));
			TiTemplate template = new TiTemplate(key, properties);
			//Set type to template, for recycling purposes.
			template.setType(getItemType());
			templatesByBinding.put(key, template);
		}
	}

	protected void processSections(Object[] sections) {
		
		for (int i = 0; i < sections.length; i++) {
			Object obj = sections[i];
			if (obj instanceof ListSectionProxy) {
				ListSectionProxy section = (ListSectionProxy) obj;
				this.sections.add(section);	
				section.setAdapter(adapter);
				section.setListView(this);
				//Attempts to set type for existing templates.
				section.setTemplateType();
			}
		}
	}
	
	protected Pair<ListSectionProxy, Integer> getSectionInfoByEntryIndex(int index) {
		if (index < 0) {
			return null;
		}

		for (int i = 0; i < sections.size(); i++) {
			ListSectionProxy section = sections.get(i);
			int sectionItemCount = section.getItemCount();
			if (index <= sectionItemCount - 1) {
				return new Pair<ListSectionProxy, Integer>(section, index);
			} else {
				index -= sectionItemCount;
			}
		}

		return null;
	}
	
	public int getItemType() {
		return itemTypeCount.getAndIncrement();
	}
	
	public TiTemplate getTemplateByBinding(String binding) {
		return templatesByBinding.get(binding);
	}
	
	public String getDefaultTemplateBinding() {
		return defaultTemplateBinding;
	}
	
}
