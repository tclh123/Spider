/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spider;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author tclh123
 */
public class Main {
    public static void main(String[] args) {
        Spider spider = null;
        
        try {
            spider = new Spider(new URL("http://www.programmableweb.com/apis/directory/1"), false);
//            spider = new Spider(new URL("http://www.programmableweb.com/api/baba-change"));
            
//            spider.setVerbose(true);
            spider.setThreadsNumber(10);
            spider.setHandler(new ExampleSpiderHandler());
            spider.setURLPattern("http://www.programmableweb.com/apis/directory/\\d+"
                    + "|http://www.programmableweb.com/api/[\\da-z\\.-]+");
        } catch (MalformedURLException ex) {
            System.out.println("fucked");
        }
        
        spider.run();
    }
}
