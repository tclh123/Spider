/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * grab 9000+ apis from http://www.programmableweb.com
 * 
 * @author tclh123
 */
public class ExampleSpiderHandler implements ISpiderHandler {
    
    @Override
    public void handleContent(URL url, long lastModified, byte[] content) {
        
        if(url.toString().contains("directory")) return;    // 目录页不用抓
        
        // fileName
        String fileName = url.getHost() + url.getPath();
        if (url.getQuery() != null) {
                fileName += "?" + url.getQuery().replace('/', '_');
        }

        File file = new File(fileName + ".txt");

        // dir
        File dir = file.getParentFile();
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
                
                String strContent = new String(content);
                
//                //TODO: 1、改成一次正则，这样效率太低。
//                //      2、有一种页面（www.programmableweb.com/api/ez-texting，有图片介绍）会失败，HTML格式不同，做兼容。
//                
//                Pattern pattern = Pattern.compile("<h2 class=\"shadow\">(.*?): Highlights</h2>");
//                Matcher matcher = pattern.matcher(strContent);
//                matcher.find();
//                String apiName = matcher.group(1);
//                
//                pattern = Pattern.compile("<dt>Summary</dt><dd>(.*?)</dd>");
//                matcher = pattern.matcher(strContent);
//                matcher.find();
//                String apiSummary = matcher.group(1);                
//
//                pattern = Pattern.compile("<dt>Category</dt><dd><a href=\"(.*?)\">(.*?)</a></dd>");
//                matcher = pattern.matcher(strContent);
//                matcher.find();
//                String apiCategory = matcher.group(2);         
//                
//                pattern = Pattern.compile("<dt>Tags</dt><dd>(.*?)</dd>");
//                matcher = pattern.matcher(strContent);
//                matcher.find();
//                String apiTags = matcher.group(1); 
//                
//                pattern = Pattern.compile("<dt>Protocols</dt><dd>(.*?)</dd>");
//                matcher = pattern.matcher(strContent);
//                matcher.find();
//                String apiProtocols = matcher.group(1);           
//                
//                pattern = Pattern.compile("<dt>Data Formats</dt><dd>(.*?)</dd>");
//                matcher = pattern.matcher(strContent);
//                matcher.find();
//                String apiDataFormats = matcher.group(1);                           
//                                
//                pattern = Pattern.compile("<dt>API home</dt><dd>(.*?)</dd>");
//                matcher = pattern.matcher(strContent);
//                matcher.find();
//                String apiHome = matcher.group(1);    
                
//                StringBuilder sb = new StringBuilder();
//                sb.append("apiName : ").append(apiName).append('\n');
//                sb.append("apiSummary : ").append(apiSummary).append('\n');
//                sb.append("apiCategory : ").append(apiCategory).append('\n');
//                sb.append("apiTags : ").append(apiTags).append('\n');
//                sb.append("apiProtocols : ").append(apiProtocols).append('\n');
//                sb.append("apiDataFormats : ").append(apiDataFormats).append('\n');
//                sb.append("apiHome : ").append(apiHome).append('\n');
                
                Pattern pattern = Pattern.compile("<h1>(.*?)</h1>");    
                Matcher matcher = pattern.matcher(strContent);
                matcher.find();
                String apiName = matcher.group(1);
                
                pattern = Pattern.compile("Highlights</h2>(.*?)</div>", Pattern.DOTALL);    // Pattern.DOTALL 单行模式
                matcher = pattern.matcher(strContent);
                matcher.find();
                String apiContents = matcher.group(1);
                
                FileOutputStream os = new FileOutputStream(file);
                StringBuilder sb = new StringBuilder();
                sb.append("apiName : ").append(apiName).append('\n');
                sb.append("apiContents : ").append(apiContents).append('\n');
                
                os.write(sb.toString().getBytes());
                
                os.close();
        } catch (IOException e) {
                e.printStackTrace();
        }        
    }
}
/*
<div class="span-16">
        <h2 class="shadow"> The Global Proteome Machine: Highlights</h2>
        <dl class="inline dt90"><dt>Summary</dt><dd>Proteome data for biomedical research</dd></dl>
        <dl class="inline dt90"><dt>Category</dt><dd><a href="/apis/directory/1?apicat=Science">Science</a></dd></dl>
        <dl class="inline dt90"><dt>Tags</dt><dd><a href="/apitag/database" rel="tag">database</a> <a href="/apitag/science" rel="tag">science</a> </dd></dl>
        <dl class="inline dt90"><dt>Protocols</dt><dd><a href="/apis/directory/1?protocol=REST" rel="nofollow">REST</a></dd></dl>
        <dl class="inline dt90"><dt>Data Formats</dt><dd><a href="/apis/directory/1?format=JSON" rel="nofollow">JSON</a></dd></dl>
        <dl class="inline dt90"><dt>API home</dt><dd><a href="http://wiki.thegpm.org/wiki/GPMDB_REST" onmousedown="clka(9249);" target="_blank">http://wiki.thegpm.org/wiki/GPMDB_REST</a>&nbsp;<img src="/images/newwind.gif" alt=""></dd></dl>
</div>
 
 * 
 * <dl class="inline dt90"><dt>Protocols</dt><dd><a href="/apis/directory/1?protocol=REST" rel="nofollow">REST</a>, <a href="/apis/directory/1?protocol=SOAP" rel="nofollow">SOAP</a>, <a href="/apis/directory/1?protocol=XML-RPC" rel="nofollow">XML-RPC</a></dd></dl>
 
 <dl class="inline dt90"><dt>API home</dt><dd><a href="http://wiki.thegpm.org/wiki/GPMDB_REST" onmousedown="clka(9249);" target="_blank">http://wiki.thegpm.org/wiki/GPMDB_REST</a>&nbsp;<img src="/images/newwind.gif" alt=""></dd></dl>
 
 * 
 * 
 * 
 * 
 * 
 <h1>SciVerse Framework and Content  API</h1>
 
 <div class="span-8 last padT15">
    <h2>Highlights</h2>
    <dl class="tabular dt90"><dt>Summary</dt><dd class="w220">Science Reference Research Platform OpenSocial </dd></dl>
    <dl class="tabular dt90"><dt>Category</dt><dd class="w220"><a href="/apis/directory/1?apicat=Reference">Reference</a></dd></dl>
    <dl class="tabular dt90"><dt>Tags</dt><dd class="w220"><a href="/apitag/reference" rel="tag">reference</a> <a href="/apitag/research" rel="tag">research</a> <a href="/apitag/science" rel="tag">science</a> <a href="/apitag/widget" rel="tag">widget</a> <a href="/apitag/opensocial" rel="tag">opensocial</a> </dd></dl>
    <dl class="tabular dt90"><dt>Protocols</dt><dd class="w220"><a href="/apis/directory/1?protocol=REST" rel="nofollow">REST</a></dd></dl>
    <dl class="tabular dt90"><dt>Data Formats</dt><dd class="w220"><a href="/apis/directory/1?format=XML" rel="nofollow">XML</a>, <a href="/apis/directory/1?format=JSON" rel="nofollow">JSON</a>, <a href="/apis/directory/1?format=Atom" rel="nofollow">Atom</a></dd></dl>
    <dl class="tabular dt90"><dt>API home</dt><dd class="w220"><a href="http://www.developers.elsevier.com/cms/index" onmousedown="clka(2709);" target="_blank">http://www.developers.elsevier.com/ ...</a>&nbsp;<img src="/images/newwind.gif" alt=""></dd></dl>
</div>
 
 */