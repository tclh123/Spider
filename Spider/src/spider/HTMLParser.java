package spider;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class HTMLParser {

	public void parse(InputStream inputStream, IHTMLParserCallback cb) throws IOException {
		int ch;
		OUTER: while ((ch = inputStream.read()) != -1) {
			if (ch == '<') {
				do {
					ch = inputStream.read();
					if (ch == -1)
						break OUTER;
				} while (Character.isWhitespace(ch));

				if (ch == '/') {
					do {
						ch = inputStream.read();
						if (ch == -1)
							break OUTER;
					} while (ch != '>');
					continue OUTER;
				}

				// Read the tag name:
				StringBuilder buf = new StringBuilder();
				while (Character.isLetter(ch)) {
					buf.append((char) ch);
					ch = inputStream.read();
					if (ch == -1)
						break OUTER;
				}
				String tag = buf.toString();

				while (Character.isWhitespace(ch)) {
					ch = inputStream.read();
					if (ch == -1)
						break OUTER;
				}

				Map<String, String> attributes = new HashMap<String, String>();

				// Has attributes:
				while (Character.isLetter(ch)) {

					// Read attribute name:
					buf.setLength(0);
					while (Character.isLetter(ch)) {
						buf.append((char) ch);
						ch = inputStream.read();
						if (ch == -1)
							break OUTER;
					}
					String attrName = buf.toString();

					while (Character.isWhitespace(ch)) {
						ch = inputStream.read();
						if (ch == -1)
							break OUTER;
					}

					String attrValue = null;
					if (ch == '=') {
						do {
							ch = inputStream.read();
							if (ch == -1)
								break OUTER;
						} while (Character.isWhitespace(ch));

						int quoteChar = -1;
						if (ch == '\'' || ch == '"') {
							quoteChar = ch;
							ch = inputStream.read();
							if (ch == -1)
								break OUTER;
						}

						if (quoteChar != -1) {
							// Read attribute value (until next quote character):
							buf.setLength(0);
							while (ch != quoteChar) {
								buf.append((char) ch);
								ch = inputStream.read();
								if (ch == -1)
									break OUTER;
							}
							ch = inputStream.read();
							if (ch == -1)
								break OUTER;
						} else {
							// Read attribute value (until we meet whitespace or end of tag):
							buf.setLength(0);
							while (!Character.isWhitespace(ch) && ch != '>') {
								buf.append((char) ch);
								ch = inputStream.read();
								if (ch == -1)
									break OUTER;
							}
						}
						attrValue = buf.toString();

						while (Character.isWhitespace(ch)) {
							ch = inputStream.read();
							if (ch == -1)
								break OUTER;
						}
					}

					attributes.put(attrName, attrValue);
				}

				cb.handleTag(tag, attributes);
			}
		}
	}
}
