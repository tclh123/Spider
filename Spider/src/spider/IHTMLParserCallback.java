package spider;

import java.util.Map;

public interface IHTMLParserCallback {

	public void handleTag(String tag, Map<String, String> attributes);
}
