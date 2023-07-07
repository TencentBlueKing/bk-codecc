package com.tencent.devops.common.util;

import org.jetbrains.annotations.NotNull;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MapUtil {

    /**
     * 按固定K个数分割Map
     *
     * @param chunkMap 需分割的Map
     * @param chunkNum 一份Map最大存放K,V数
     * @return list
     */
    @NotNull
    public static <K, V> List<Map<K, V>> mapChunk(Map<K, V> chunkMap, int chunkNum) {
        if (chunkMap == null || chunkNum <= 0) {
            List<Map<K, V>> list = new ArrayList<>();
            list.add(chunkMap);
            return list;
        }
        Set<K> keySet = chunkMap.keySet();
        Iterator<K> iterator = keySet.iterator();
        int i = 1;
        List<Map<K, V>> total = new ArrayList<>();
        Map<K, V> itemMap = new HashMap<>();
        while (iterator.hasNext()) {
            K next = iterator.next();
            itemMap.put(next, chunkMap.get(next));
            if (i == chunkNum) {
                total.add(itemMap);
                itemMap = new HashMap<>();
                i = 0;
            }
            i++;
        }
        if (!CollectionUtils.isEmpty(itemMap)) {
            total.add(itemMap);
        }
        return total;
    }
}
