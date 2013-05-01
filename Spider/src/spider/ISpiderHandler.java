package spider;

import java.net.URL;

public interface ISpiderHandler {

	public void handleContent(URL url, long lastModified, byte[] content);
}
