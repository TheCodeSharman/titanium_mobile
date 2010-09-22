/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by 
 * Appcelerator. It should not be modified by hand.
 */
package ${config['appid']};

import org.mozilla.javascript.Scriptable;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.kroll.KrollModule;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public final class ${config['classname']}Application extends TiApplication {

	protected HashMap<String, String[]> moduleBindings = new HashMap<String, String[]>();
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		appInfo = new ${config['classname']}AppInfo(this);
		stylesheet = new ApplicationStylesheet();
		
		onAfterCreate();
	}
	
	@Override
	protected void bootModules(TiContext context) {
		//TODO: Make Modules independent of context
		%for module in app_modules:
		// ${module['api_name']} module
			<% bindings_len = len(module['bindings']) %>
		String[] ${module['api_name']}_bindings = new String[] {
			%for j in range(0, bindings_len):
			<% binding = module['bindings'][j] %>\
		"${binding}"\
			%if j < bindings_len:
		,
			%endif
			%endfor
		};
		moduleBindings.put("${module['api_name']}", ${module['api_name']}_bindings);
		${module['class_name']} ${module['api_name']}_module = new ${module['class_name']}(context);
		modules.add(${module['api_name']}_module);
		%endfor
	}
	
	@Override
	public String[] getFilteredBindings(String moduleName) {
		return moduleBindings.get(moduleName);
	}
}
