/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spider;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * 给定一个起始URL，然后读取网页HTML内容，按URL regex匹配其中的URL，加入URL queue，继续爬行。
 * 对每个HTML内容，都将执行Handler，以完成采集等任务。
 * 
 * 实现采用多个Worker（多线程）。
 * 
 * @author tclh123
 * 
 */
public class Spider implements Runnable {
    
    private URL startURL;
    private LinkedList<URL> urlQueue;
    private Set<String> processedURLs;
    
    private ISpiderHandler handler;
    
    private int timeout;
    private boolean followOtherDomains;
    private int threadsNumber;
    
    private String userAgent;
    private String urlPattern;
    private boolean verbose;    // 显示冗余信息
    
    private boolean regexParser;

    public Spider() {
        this(null);
    }

    public Spider(URL startURL) {
        this.startURL = startURL;
//        this.urlPattern = urlPattern;
        
        urlQueue = new LinkedList<URL>();
        processedURLs = new HashSet<String>();
        
        timeout = 5000; // default timeout is 5 seconds
        threadsNumber = 5;
        
        regexParser = false;
    }
    public Spider(URL startURL, boolean regexParser) {
        this.startURL = startURL;
//        this.urlPattern = urlPattern;
        
        urlQueue = new LinkedList<URL>();
        processedURLs = new HashSet<String>();
        
        timeout = 5000; // default timeout is 5 seconds
        threadsNumber = 5;
        
        this.regexParser = regexParser;
    }

    /**
     * Gets URL to start retreival from
     * @return start URL
     */
    public URL getStartURL() {
        return startURL;
    }

    /**
     * Sets URL to start retreival from
     * @param startURL
     */
    public void setStartURL(URL startURL) {
        this.startURL = startURL;
    }

    /**
     * @return Handler attached to this spider
     */
    public ISpiderHandler getHandler() {
        return handler;
    }

    /**
     * Attach handler to this spider
     * @param handler
     */
    public void setHandler(ISpiderHandler handler) {
        this.handler = handler;
    }

    /**
     * Connection timeout in milliseconds
     * @return timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Sets connection timeout in milliseconds
     * @param timeout
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Whether to follow other domains
     * @return followOtherDomains
     */
    public boolean isFollowOtherDomains() {
        return followOtherDomains;
    }

    /**
     * Set whether to follow other domains
     * @param followOtherDomains
     */
    public void setFollowOtherDomains(boolean followOtherDomains) {
        this.followOtherDomains = followOtherDomains;
    }

    /**
     * Gets number of concurrent worker threads
     * @return
     */
    public int getThreadsNumber() {
        return threadsNumber;
    }

    /**
     * Sets number of concurrent worker threads
     * @param threadsNumber
     */
    public void setThreadsNumber(int threadsNumber) {
        this.threadsNumber = threadsNumber;
    }


    /**
     * Returns user agent string, that will be used in User-Agent header
     * @return
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Sets user agent string, that will be used in User-Agent header
     * @param userAgent
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Sets pattern that only URL matches it will be retrieved
     * @param pattern
     */
    public void setURLPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    /**
     * Returns pattern that only URL matches it will be retrieved
     * @return pattern
     */
    public String getURLPattern() {
        return urlPattern;
    }

    /**
     * If set to true verbose information will be printed to user
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Returns whether verbose information will be printed to user
     * @return verbose
     */
    public boolean isVerbose() {
        return verbose;
    }


    /**
     * Add this URL to the queue of URLs to be processed
     * @param url
     */
    private void queueURL(URL url) {
        if (!followOtherDomains && !url.getHost().equals(getStartURL().getHost())) {
            if (verbose) {
                System.out.format("Refusing to put URL %s into queue - this URL is from other domain\n", url.toString());
            }
            return;
        }
        String urlStr = url.toString();

        synchronized (urlQueue) {   // Java同步机制，urlQueue 必须先获得对象锁
            if (!processedURLs.contains(urlStr)) {
                if (verbose) {
                    System.out.format("Putting URL %s into queue\n", urlStr);
                }
                urlQueue.add(url);
                processedURLs.add(urlStr);

                urlQueue.notifyAll();   // 唤醒所有
            }
        }
    }
    
    /**
     * Starts retrieval of Web pages
     * @see Runnable#run()
     */
    public void run() {
        queueURL(getStartURL());

        Semaphore waitForQueueSemaphore = new Semaphore(threadsNumber); // 信号（同步）量

        if (verbose) {
            System.out.format("Starting %d working threads\n", threadsNumber);
        }

        Worker[] workers = new Worker[threadsNumber];
        for (int i = 0; i < threadsNumber; ++i) {
            workers[i] = new Worker(waitForQueueSemaphore);
            new Thread(workers[i], "Spider Worker #" + i).start();
        }

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            // Check whether all workers are waiting for the queue,
            // that usually means, that there are no links any more:
            if (waitForQueueSemaphore.availablePermits() == 0) {
                if (verbose) {
                    System.out.println("Stopping working threads");
                }
                for (int i = 0; i < threadsNumber; ++i) {
                    workers[i].stop();
                }
                // Free them:
                synchronized (urlQueue) {
                    urlQueue.notifyAll();
                }
                break;
            }
        } while (true);
    }
    
    class Worker implements Runnable {

        private boolean isStopped;
        private Semaphore waitForQueueSemaphore;    // 所有 worker share 一个 Semaphore

        public Worker(Semaphore waitForQueueSemaphore) {
            this.waitForQueueSemaphore = waitForQueueSemaphore;
        }

        public void stop() {
            isStopped = true;
        }
        
        
        public void run() {
            ISpiderHandler handler = getHandler();
            if (handler == null) {
                handler = new DefaultSpiderHandler();
            }

            while (!isStopped) {
                URL url;
                synchronized (urlQueue) {
                    if (urlQueue.isEmpty()) {
                        try {
                            waitForQueueSemaphore.acquire();
                            urlQueue.wait();
                        } catch (InterruptedException e) {
                        } finally {
                            waitForQueueSemaphore.release();
                        }
                        continue;
                    }
                    url = urlQueue.removeFirst();   // 取出 队首 URL
                }
                
                if (urlPattern != null && !Pattern.compile(urlPattern).matcher(url.toString()).matches()) {
                    if (verbose) {
                            System.out.format("Refusing to load URL %s - it doesn't match pattern '%s'\n", url.toString(), urlPattern);
                    }
                    continue;
                }

                // We only work with HTTP protocol:
                if (!"http".equals(url.getProtocol())) {
                    if (verbose) {
                            System.out.format("Refusing to load URL %s - protocol is not HTTP\n", url.toString());
                    }
                    continue;
                }

                try {
                    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
                    urlConnection.setConnectTimeout(timeout);

                    if (userAgent != null) {
                        urlConnection.setRequestProperty("User-Agent", userAgent);
                    } else {
                        urlConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Maxthon;)");
                    }
                    
                    int responseCode = urlConnection.getResponseCode();
                    
                    urlConnection.getHeaderFields();
                    // We process only HTML pages:
                    String contentType = urlConnection.getContentType();
                    if (contentType == null || !contentType.startsWith("text/html")) {
                        if (verbose) {
                            System.out.format("Stopping processing URL %s - unknown content type (%s)\n", url.toString(), contentType);
                        }
                        continue;
                    }
                    
                    HTMLPageProcessor htmlPageProcessor = null;
                    if(!regexParser) {
                        htmlPageProcessor = new HTMLPageProcessor(urlConnection);
                    } else {
                        htmlPageProcessor = new HTMLPageProcessor(urlConnection, urlPattern);
                    }
                    htmlPageProcessor.process();

                    if (htmlPageProcessor.shouldFollow()) {
                        Collection<URL> links = htmlPageProcessor.getLinks();
                        Iterator<URL> i = links.iterator();
                        while (i.hasNext()) {
                            queueURL(i.next());     // 加入 urls
                        }
                    }
                    if (htmlPageProcessor.shouldIndex()) {
                        handler.handleContent(url, urlConnection.getLastModified(), htmlPageProcessor.getContents());
                    }
                } catch (FileNotFoundException e) {
                    if (verbose) {
                            System.out.println("Resource doesn't exist: " + url);
                    }
                } catch(SocketException e) {
                    // java.net.SocketException: Connection reset
                    // 有可能因为访问过于频繁，导致连接被重置
                    
                    System.out.println("Thread Sleep 30s, cause " + e.toString());
                    // 没有被访问，重新放回去
                    String urlStr = url.toString();
                    synchronized (urlQueue) {   // Java同步机制，urlQueue 必须先获得对象锁
                        if (processedURLs.contains(urlStr)) {   // 不能用 queueURL，因为processedURLs已经包含url
                            if (verbose) {
                                System.out.format("RE Putting URL %s into queue\n", urlStr);
                            }
                            urlQueue.add(url);
                            urlQueue.notifyAll();   // 唤醒所有
                        }
                    }
                    
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException ex) {
                    }
                } catch (IOException e) {
                    
                    //      java.net.SocketTimeoutException: connect timed out  ??
                    
                    e.printStackTrace();
                }
            }
        }
    }
}
