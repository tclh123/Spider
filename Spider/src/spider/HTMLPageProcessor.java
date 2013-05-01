package spider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTMLPageProcessor {

	private URLConnection urlConnection;
	private List<URL> links;
	private boolean index;
	private boolean follow;
	private byte[] contents;
        
        private boolean regexParser;
        private String urlPattern;

	/**
	 * Constructs links HTML parser
	 * @param urlConnection Connection to the URL
	 */
	public HTMLPageProcessor(URLConnection urlConnection) {
		this.urlConnection = urlConnection;
		links = new LinkedList<URL>();
		index = true;
		follow = true;
                
                regexParser = false;
	}
        public HTMLPageProcessor(URLConnection urlConnection, String urlPattern) {
		this.urlConnection = urlConnection;
		links = new LinkedList<URL>();
		index = true;
		follow = true;
                
                regexParser = true;
                this.urlPattern = urlPattern;
	}

	/**
	 * Checks robots META tag, whether we should follow links from this page
	 * @see http://www.robotstxt.org/wc/exclusion.html#meta
	 * @return <code>true</code> if spider should follow links from this page
	 */
	public boolean shouldFollow() {
		return follow;
	}

	/**
	 * Checks robots META tag, whether we should index this page
	 * @see http://www.robotstxt.org/wc/exclusion.html#meta
	 * @return <code>true</code> if spider should index this page
	 */
	public boolean shouldIndex() {
		return index;
	}

	/**
	 * Returns collection of link URLs that where found on this page
	 * @return links
	 */
	public Collection<URL> getLinks() {
		return links;
	}

	/**
	 * Returns contents of the HTML page
	 * @return contents
	 */
	public byte[] getContents() {
		return contents;
	}

	/**
	 * Run parsing
	 * @throws IOException
	 */
	public void process() throws IOException {
		InputStream inputStream = urlConnection.getInputStream();

		byte[] buffer = new byte[2048];
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		int bytesRead = inputStream.read(buffer);
		while (bytesRead >= 0) {
			byteOut.write(buffer, 0, bytesRead);    // input -> output
			bytesRead = inputStream.read(buffer);
		}
		contents = byteOut.toByteArray();

                if(!regexParser) {
                    // 先抓取所有URL，之后再跟正则match
                    new HTMLParser().parse(new ByteArrayInputStream(contents), new HTMLParserCallback());   // 利用接口实现回调    
                } else {
                    // 直接用正则匹配URL（有内存泄露？GC无法释放）
                    
                    String strContents = new String(contents);
                    String strPattern[] = urlPattern.split("\\|");
                    for(String s : strPattern) {
                        String domain = "http://"+urlConnection.getURL().getHost();
                        if(s.contains(domain)) {
                            String hrefPattern = s.substring(domain.length());
//                            System.out.println(hrefPattern);
                            Pattern pattern = Pattern.compile("href=\""+hrefPattern+"\"");
                            Matcher matcher = pattern.matcher(strContents);
                            
                            while(matcher.find()) {
                                String href = matcher.group();
                                href = href.substring("href=\"".length(), href.length()-1);
                                URL url = new URL(urlConnection.getURL(), href);
                                links.add(url);
//                                System.out.println(url.toString());                                
                            }                            
                        }
                    }
                }
	}

	class HTMLParserCallback implements IHTMLParserCallback {
		public void handleTag(String tag, Map<String, String> attributes) {
//			if ("meta".equals(tag)) {
//				String name = attributes.get("name");
//				if (name != null && "robots".equals(name)) {
//					String content = attributes.get("content");
//					if (content != null) {
//						String[] contentElements = content.split("[\\s,]+");
//						for (int i = 0; i < contentElements.length; ++i) {
//							if ("noindex".equalsIgnoreCase(contentElements[i])) {
//								index = false;
//							} else if ("nofollow".equalsIgnoreCase(contentElements[i])) {
//								follow = false;
//							}
//						}
//					}
//				}
//			} else 
                        if (follow) {
				String href = attributes.get("href");
				if (href == null && "frame".equals(tag)) {
					href = attributes.get("src");
				}
				if (href != null) {
					int i = href.indexOf('#');
					if (i != -1) {
						href = href.substring(0, i);
					}
					try {
						URL url = new URL(urlConnection.getURL(), href);
						links.add(url);
					} catch (MalformedURLException e) {
					}
				}
			}
		}
	}
}
