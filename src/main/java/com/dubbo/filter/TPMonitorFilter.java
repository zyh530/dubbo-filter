package com.dubbo.filter;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.support.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.dubbo.common.constants.CommonConstants.CONSUMER;
import static org.apache.dubbo.common.constants.CommonConstants.PROVIDER;

/**
 * @author: wudong
 * @create: 2022-02-15 16:51
 **/
@Activate(group = {CONSUMER,PROVIDER})
public class TPMonitorFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(TPMonitorFilter.class);

    private static ConcurrentHashMap<MethodSign, MonitorWaterLine> methodMap = new ConcurrentHashMap<>();

    private static ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);

    static {
        scheduledThreadPoolExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Set<Map.Entry<MethodSign, MonitorWaterLine>> entries = methodMap.entrySet();
                for (Map.Entry<MethodSign, MonitorWaterLine> entry : entries) {
                    MethodSign key = entry.getKey();
                    MonitorWaterLine monitorWaterLine = entry.getValue();
                    if(monitorWaterLine.getDatas().isEmpty()){
                        entries.remove(key);
                        continue;
                    }
                    WaterLineStat stat = monitorWaterLine.stat();
                    int tp0 = stat.TP0();
                    int tp50 = stat.TP50();
                    int tp90 = stat.TP90();
                    int tp99 = stat.TP99();
                    int tp100 = stat.TP100();

                    logger.info("最近一分钟| method={}  | TP50={} | TP90={}  |  TP99={} ",key,tp50,tp90,tp99);

                    // logger.info("最近一分钟|method={}|TP0={}|TP50={}|TP90={}|TP99={}|TP100={}",key,tp0,tp50,tp90,tp99,tp100);
                }
            }
        },5, 5, TimeUnit.SECONDS);
    }



    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        InvokeMode invokeMode = RpcUtils.getInvokeMode(invoker.getUrl(), invocation);

        if (InvokeMode.SYNC != invokeMode) {
            return invoker.invoke(invocation);
        }

        MethodSign methodSign = new MethodSign(invocation);
        MonitorWaterLine methodMonitorWaterLine = methodMap.computeIfAbsent(methodSign, key -> {
            MonitorWaterLine monitorWaterLine = new MonitorWaterLine(60);
            return monitorWaterLine;
        });
        long start = System.currentTimeMillis();
        try {
            return invoker.invoke(invocation);
        } finally {
            methodMonitorWaterLine.record((int)(System.currentTimeMillis() - start));
        }
    }
}
