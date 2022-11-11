package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final int maxDepth;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            @MaxDepth int maxDepth,
            @IgnoredUrls List<Pattern> ignoredUrls,
            PageParserFactory parserFactory) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.maxDepth = maxDepth;
        this.ignoredUrls = ignoredUrls;
        this.parserFactory = parserFactory;

    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);
        ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
        for (String url : startingUrls) {
            pool.invoke(new CrawlInternalTask(url, deadline, maxDepth, counts, visitedUrls));

        }

        if (counts.isEmpty()) {
            return new CrawlResult.Builder()
                    .setWordCounts(counts)
                    .setUrlsVisited(visitedUrls.size())
                    .build();
        }

        return new CrawlResult.Builder()
                .setWordCounts(WordCounts.sort(counts, popularWordCount))
                .setUrlsVisited(visitedUrls.size())
                .build();
    }

    @Override
    public int getMaxParallelism() {
        System.out.println(Runtime.getRuntime().availableProcessors());
        return Runtime.getRuntime().availableProcessors();
    }

    public class CrawlInternalTask extends RecursiveTask<Boolean> {
        private static Instant deadline;
        private static int maxDepth;
        private static ConcurrentHashMap<String, Integer> counts;
        private static ConcurrentSkipListSet<String> visitedUrls;
        private final String url;
        private final ReentrantLock lock = new ReentrantLock();



        CrawlInternalTask(String url, Instant deadline, int maxDepth, ConcurrentHashMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls) {
            System.out.println("new task getting created");
            this.url = url;
            CrawlInternalTask.deadline = deadline;
            CrawlInternalTask.maxDepth = maxDepth;
            CrawlInternalTask.counts = counts;
            CrawlInternalTask.visitedUrls = visitedUrls;
        }

        @Override
        protected Boolean compute() {
            if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
                System.out.println("returning compute" + false);
                return false;
            }
            for (Pattern pattern : ignoredUrls) {
                if (pattern.matcher(url).matches()) {
                    System.out.println("returning ignored urls" + false);

                    return false;
                }
            }
            System.out.println("max depth " + maxDepth + "url" + url);

            /*if (visitedUrls.contains(url)) {
                System.out.println("returning visited urls" + false);

                return false;
            }
            visitedUrls.add(url);*/
            try {
                lock.lock();
                if (visitedUrls.contains(url)) {
                    return false;
                }
                visitedUrls.add(url);
            } finally {
                lock.unlock();
            }
            PageParser.Result result = parserFactory.get(url).parse();
            result.getWordCounts().entrySet().parallelStream().forEach(e -> {
                if (counts.containsKey(e.getKey())) {
                    counts.put(e.getKey(), e.getValue() + counts.get(e.getKey()));
                } else {
                    counts.put(e.getKey(), e.getValue());
                }
            });
            List<CrawlInternalTask> subtasks =
                    result.getLinks().stream().map(link -> new CrawlInternalTask(link, deadline, maxDepth - 1, counts, visitedUrls))
                            .collect(Collectors.toList());
            invokeAll(subtasks);


            return true;
        }


    }


}
