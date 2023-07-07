package com.tencent.devops.common.util;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * List转String工具类
 *
 * @author harryzhu
 * @version V4.0
 * @date 2019/2/22
 */
public class List2StrUtil
{
    /**
     * List转分号分隔String
     *
     * @param list
     * @return
     */
    public static String toString(Collection<String> list, String spliter)
    {
        StringBuffer resultBuf = new StringBuffer();
        if (CollectionUtils.isNotEmpty(list))
        {
            for (String item : list)
            {
                resultBuf.append(item).append(spliter);
            }
        }
        String resultStr = resultBuf.toString();
        if (StringUtils.isNotEmpty(resultStr))
        {
            return resultStr.substring(0, resultStr.length() - 1);
        }
        return "";
    }

    /**
     * String转List
     *
     * @param source
     * @return
     */
    public static List<String> fromString(String source, String splitter) {
        List<String> resultList = new ArrayList<>();

        if (StringUtils.isNotEmpty(source)) {
            for (String owner : source.split(splitter)) {
                resultList.add(owner);
            }
        }

        return resultList;
    }

    /**
     * 过滤重复的元素
     *
     * @param source
     * @param spliter
     * @return
     */
    public static String filterDup(String source, String spliter)
    {
        //IP地址去重
        if (StringUtils.isNotEmpty(source))
        {
            Set<String> bindIpSet = new HashSet<>();
            for (String bindIp : source.split(spliter))
            {
                bindIpSet.add(bindIp);
            }
            if (CollectionUtils.isNotEmpty(bindIpSet))
            {
                StringBuffer filteredBindIpStr = new StringBuffer();
                for (String bindIp : bindIpSet)
                {
                    filteredBindIpStr.append(bindIp).append(spliter);
                }
                source = filteredBindIpStr.toString();
                if (source.endsWith(spliter))
                {
                    source = source.substring(0, source.lastIndexOf(spliter));
                }
            }
        }
        return source;
    }


    /**
     * 优化removAll方法
     *
     * @param source      原始list集合
     * @param destination 需筛除掉的list集合
     * @param <T>
     * @return list
     */
    public static <T> List<T> listRemoveAllAscension(List<T> source, List<T> destination) {
        List<T> result = new LinkedList<T>();
        Set<T> destinationSet = new HashSet<T>(destination);
        for (T t : source) {
            if (!destinationSet.contains(t)) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * 获取两个集合的交集
     *
     * @param largerArryList 较大的集合
     * @param smallerArryList 较小的集合
     * @param <T> 泛型
     * @return list
     */
    public static <T> List<T> intersectionList(List<T> largerArryList, List<T> smallerArryList) {

        List<T> resultList;

        // 比较大的集合使用linkedlist
        LinkedList<T> largerList = new LinkedList<T>(largerArryList);

        // 比较小的集合使用hashset
        HashSet<T> smallerList = new HashSet<T>(smallerArryList);

        // 采用Iterator迭代器进行数据的操作
        Iterator<T> iter = largerList.iterator();
        while (iter.hasNext()) {
            if (!smallerList.contains(iter.next())) {
                iter.remove();
            }
        }
        return new ArrayList<>(largerList);
    }
}
