package com.dubbo.filter;

/**
 * @author: wudong
 * @create: 2022-02-14 09:40
 **/
public interface WaterLineStat {
    int TP0();

    public int TP50();
    public int TP90();
    public int TP99();

    int TP100();

    public long statTimetamps();
}
