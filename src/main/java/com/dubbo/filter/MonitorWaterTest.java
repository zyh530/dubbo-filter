package com.dubbo.filter;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author: wudong
 * @create: 2022-02-15 11:22
 **/
public class MonitorWaterTest {

    public static void main(String[] args) throws InterruptedException {
        MonitorWaterLine monitorWaterLine = new MonitorWaterLine(60);


        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(5);

        for (int i = 0; i < 5; i++) {
            Thread.sleep(ThreadLocalRandom.current().nextInt(5000));
            scheduledThreadPoolExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    WaterLineStat stat = monitorWaterLine.stat();
                    int tp90 = stat.TP90();
                    int tp99 = stat.TP99();
                    // System.out.println(Thread.currentThread() + ",time:" + start +
                    //         ",TP情况统计:耗时:" + (System.currentTimeMillis() - start) + ",TP90:" + tp90 + ",TP99:" + tp99);
                }
            },5,5, TimeUnit.SECONDS);
        }

        //模拟20个线程记录请求耗时
        ThreadUtil.concurrencyRun(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                TimeRecord timeRecord = new TimeRecord();
                for (int i = 0; i < 300; i++) {
                    int time = ThreadLocalRandom.current().nextInt(100);
                    Thread.sleep(time);
                    long start = System.nanoTime()/1000;
                    monitorWaterLine.record(time);
                    long end = System.nanoTime()/1000;
                    timeRecord.addUseTime(end - start);
                }
                System.out.println("记录耗时情况统计:" + timeRecord);
                return null;
            }
        }, 100);


    }


    private static class TimeRecord {
        private int times;
        private long totalTime;

        public void addUseTime(long totalTime) {
            times++;
            this.totalTime += totalTime;
        }

        @Override
        public String toString() {
            return "总次数:" + times + ",总耗时:" + totalTime + ",平均耗时:" + totalTime / times;
        }
    }

    private static class ThreadUtil {
        public static <T> void concurrencyRun(final Callable<T> callable,int num) throws InterruptedException {
            System.out.println("start:"+System.currentTimeMillis());
            final CountDownLatch totalcount = new CountDownLatch(num);
            final CountDownLatch stop = new CountDownLatch(num);
            final AtomicBoolean bo = new AtomicBoolean(true);
            for(int n=0;n<num;n++){
                Thread a = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        totalcount.countDown();
                        while (bo.get()) {}
                        try {
                            callable.call();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        stop.countDown();
                    }
                });
                a.start();
            }
            totalcount.await();
            bo.compareAndSet(true, false);
            stop.await();
            System.out.println("end:"+System.currentTimeMillis());
        }
    }
}
