package ti.modules.titanium.xml;

import java.util.List;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.w3c.dom.Node;

@Kroll.proxy
public class XPathNodeListProxy extends KrollProxy
{
	private List nodeList;
	public XPathNodeListProxy(List nodeList)
	{
		super();
		this.nodeList = nodeList;
	}
	
	@Kroll.getProperty @Kroll.method
	public int getLength() {
		return nodeList.size();
	}

	@Kroll.method
	public NodeProxy item(int index) {
		Node node = (Node)nodeList.get(index);
		return NodeProxy.getNodeProxy(node);
	}
}