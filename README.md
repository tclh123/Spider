#Spider

originly forked from [Spider](https://github.com/spektom/spider), and I did some modification for my own purpose.

##Usage

```
Spider spider = new Spider(new URL("http://www.programmableweb.com/apis/directory/1"), false);
spider.setThreadsNumber(10);
spider.setHandler(new ExampleSpiderHandler());
spider.setURLPattern("http://www.programmableweb.com/apis/directory/\\d+"
        + "|http://www.programmableweb.com/api/[\\da-z\\.-]+");
```