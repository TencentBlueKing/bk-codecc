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

    /**
     * 判断两个MAP是否相等
     * @param map1
     * @param map2
     * @return
     */
    public static boolean areMapsEqual(Map<Long, List<String>> map1, Map<Long, List<String>> map2) {
        // 检查大小是否相等
        if (map1.size() != map2.size()) {
            return false;
        }

        // 遍历map1中的每个条目
        for (Map.Entry<Long, List<String>> entry : map1.entrySet()) {
            Long key = entry.getKey();
            List<String> value = entry.getValue();

            // 检查map2中是否存在相同的键
            if (!map2.containsKey(key)) {
                return false;
            }

            // 比较两个列表是否相等
            List<String> value2 = map2.get(key);
            if (!value.equals(value2)) {
                return false;
            }
        }
        return true;
    }
}
