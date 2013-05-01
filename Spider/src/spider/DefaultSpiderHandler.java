package spider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

public class DefaultSpiderHandler implements ISpiderHandler {
	
        @Override
	public void handleContent(URL url, long lastModified, byte[] content) {
		
                // fileName
		String fileName = url.getHost() + url.getPath();
		if (url.getQuery() != null) {
			fileName += "?" + url.getQuery().replace('/', '_');
		}
		
		File file = new File(fileName);

                // dir
		File dir = file.getParentFile();
//                System.out.println("dir: " + dir);
		if (dir != null) {
			if (dir.exists()) {
				if (!dir.isDirectory()) {
					File bakFile = new File(dir.getAbsoluteFile() + ".bak");
					dir.renameTo(bakFile);
					dir.mkdirs();
					bakFile.renameTo(new File(dir, "index.html"));
				}
			} else {
				dir.mkdirs();
			}
		}

                // 若更新，则保留两者
		if (file.exists()) {
			if (lastModified != 0 && file.lastModified() > lastModified) {
				System.out.println("Skipping: " + file);
				return;
			}
			int index = 1;
			File newfile;
			do {
				newfile = new File(file.getParentFile(), file.getName() + "." + index++);
			} while (newfile.exists());

			file = newfile;
		}
                
                // write
		try {
			System.out.println("Saved: " + file);
			FileOutputStream os = new FileOutputStream(file);
			os.write(content);
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}