package com.dubbo.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * TP10 TP99等水位线监控
 * @author: wudong
 * @create: 2022-02-11 17:58
 **/
public class MonitorWaterLine {
    private static final Logger logger = LoggerFactory.getLogger(MonitorWaterLine.class);

    private ConcurrentLinkedQueue<RecordNode> datas;
    private int statIntervalSeconds;
    private volatile StatInterval preStatInterval;
    private volatile boolean deleting;

    //删除任务固定一个线程
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(1), new RejectedExecutionHandler() {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            logger.debug("拒绝任务");
        }
    });

    public MonitorWaterLine(int statIntervalSeconds) {
        this.statIntervalSeconds = statIntervalSeconds;
        this.datas = new ConcurrentLinkedQueue<>();
    }

    /**
     * @param milliseconds
     */
    public void record(int milliseconds) {
        datas.add(new RecordNode(milliseconds));
        deleteOldRecord();
    }

    public synchronized WaterLineStat stat() {
        StatInterval statInterval = getNowStatInterval();
        StatNode statNode = new StatNode(statInterval);
        this.preStatInterval = statInterval;

        Iterator<RecordNode> iterator = datas.iterator();
        while (iterator.hasNext()) {
            RecordNode next = iterator.next();
            if (next.getLoadSysMillis() < statInterval.getGteSysMillis() || next.getLoadSysMillis() > statInterval.getLteSysMillis()) {
                continue;
            } else {
                statNode.incr(next.data);
            }
        }
        this.preStatInterval = null;
        return statNode;
    }

    private StatInterval getIntersection(StatInterval statIntervalLeft, StatInterval statIntervalRight) {
        if (statIntervalLeft == null || statIntervalRight == null) {
            return null;
        }
        if (statIntervalLeft.getLteSysMillis() <= statIntervalRight.getLteSysMillis()
                && statIntervalLeft.getLteSysMillis() >= statIntervalRight.getGteSysMillis()
                && statIntervalLeft.getGteSysMillis() <= statIntervalRight.getGteSysMillis()) {
            return new StatInterval(statIntervalRight.getGteSysMillis(), statIntervalLeft.getLteSysMillis());
        }
        return null;
    }

    private StatInterval getNowStatInterval() {
        long lteSysMillis = System.currentTimeMillis();
        long gteSysMillis = lteSysMillis - this.statIntervalSeconds * 1000;
        StatInterval statInterval = new StatInterval(gteSysMillis, lteSysMillis);
        return statInterval;
    }

    private void deleteOldRecord() {
        //20%的概率触发删除
        int random = ThreadLocalRandom.current().nextInt(10);
        if (deleting || random < 8) {
            return;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    deleting = true;

                    StatInterval statInterval = MonitorWaterLine.this.preStatInterval;

                    if (statInterval == null) {
                        statInterval = getNowStatInterval();
                    }

                    Iterator<RecordNode> iterator = datas.iterator();
                    while (iterator.hasNext()) {
                        RecordNode next = iterator.next();
                        if (next.getLoadSysMillis() < statInterval.gteSysMillis) {
                            iterator.remove();
                        } else {
                            break;
                        }
                    }
                } finally {
                    deleting = false;
                }
            }
        };
        threadPoolExecutor.execute(runnable);
    }

    public ConcurrentLinkedQueue<RecordNode> getDatas() {
        return datas;
    }

    private static class StatNode implements WaterLineStat {
        private StatInterval statInterval;
        private TreeMap<Integer, Integer> timeCount;
        private TreeMap<Integer, Integer> countTime;

        public StatNode(long gteSysMillis, long lteSysMillis) {
            this.statInterval = new StatInterval(gteSysMillis, lteSysMillis);
            this.timeCount = new TreeMap<>();
        }

        public StatNode(StatInterval statInterval) {
            this.statInterval = statInterval;
            this.timeCount = new TreeMap<>();
        }

        public void incr(int time) {
            Integer integer = timeCount.get(time);
            if (integer == null) {
                integer = 1;
            } else {
                integer++;
            }
            timeCount.put(time, integer);
        }

        public void decr(int time) {
            Integer integer = timeCount.get(time);
            if (integer != null) {
                integer--;
                timeCount.put(time, integer);
            }
        }

        private TreeMap<Integer, Integer> getCountStat() {
            if (countTime != null) {
                return countTime;
            }
            synchronized (this) {
                if (countTime != null) {
                    return countTime;
                }
                TreeMap<Integer, Integer> stat = new TreeMap<>();
                int sum = 0;

                Set<Map.Entry<Integer, Integer>> entries = timeCount.entrySet();
                for (Map.Entry<Integer, Integer> entry : entries) {
                    int time = entry.getKey();
                    int count = entry.getValue();

                    sum += count;
                    stat.put(sum, time);

                }
                countTime = stat;
                return countTime;
            }
        }

        @Override
        public int TP0() {
            return tp(0d);
        }

        @Override
        public int TP50() {
            return tp(0.5d);
        }

        @Override
        public int TP90() {
            return tp(0.9d);
        }

        @Override
        public int TP99() {
            return tp(0.99d);
        }

        @Override
        public int TP100() {
            return tp(1d);
        }

        @Override
        public long statTimetamps() {
            return statInterval.getLteSysMillis();
        }

        private int tp(double tp) {
            TreeMap<Integer, Integer> countStat = getCountStat();
            if (countStat.size() == 0) {
                return 0;
            }
            int total = countStat.lastKey();
            // countStat.ceilingEntry()
            int ceil = (int) Math.ceil(total * tp);
            return countStat.ceilingEntry(ceil).getValue();
        }

        @Override
        public String toString() {
            return "StatNode{" +
                    "statInterval=" + statInterval +
                    ", TP90=" + TP90() +
                    ", TP99=" + TP99() +
                    ", timeCount=" + timeCount +
                    ", countTime=" + countTime +
                    '}';
        }
    }


    private static class StatInterval {
        private long gteSysMillis;
        private long lteSysMillis;

        public StatInterval() {
        }

        public StatInterval(long gteSysMillis, long lteSysMillis) {
            this.gteSysMillis = gteSysMillis;
            this.lteSysMillis = lteSysMillis;
        }

        public long getGteSysMillis() {
            return gteSysMillis;
        }

        public void setGteSysMillis(long gteSysMillis) {
            this.gteSysMillis = gteSysMillis;
        }

        public long getLteSysMillis() {
            return lteSysMillis;
        }

        public void setLteSysMillis(long lteSysMillis) {
            this.lteSysMillis = lteSysMillis;
        }

        @Override
        public String toString() {
            return "StatInterval{" +
                    "gteSysMillis=" + gteSysMillis +
                    ", lteSysMillis=" + lteSysMillis +
                    '}';
        }
    }


    private static class RecordNode {
        private long loadSysMillis;
        private int data;

        public RecordNode(int data) {
            this.loadSysMillis = System.currentTimeMillis();
            this.data = data;
        }

        public long getLoadSysMillis() {
            return loadSysMillis;
        }

        public int getData() {
            return data;
        }
    }
}
