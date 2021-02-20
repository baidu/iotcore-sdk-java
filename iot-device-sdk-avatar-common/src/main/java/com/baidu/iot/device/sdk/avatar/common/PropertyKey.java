/*
 * Copyright (c) 2020 Baidu, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.iot.device.sdk.avatar.common;

import com.google.common.collect.Lists;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Author zhangxiao18
 * Date 2020/9/24
 */
@EqualsAndHashCode
public class PropertyKey {

    // a.b.c -> [a, b, c]
    private final String[] keyEntries;

    private final String keyPrettyString;

    public PropertyKey(String key) {
        keyPrettyString = key;
        if (key.isEmpty()) {
            keyEntries = new String[0];
            return;
        }
        keyEntries = key.split("\\.");
        if (keyEntries.length > 5) {
            throw new RuntimeException("Key is too long");
        }
    }

    private PropertyKey(String[] keyEntries) {
        this.keyEntries = keyEntries;
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < keyEntries.length; i++) {
            key.append(keyEntries[i]);
            if (i != keyEntries.length - 1) {
                key.append(".");
            }
        }
        keyPrettyString = key.toString();
    }

    @Override
    public String toString() {
        return keyPrettyString;
    }

    public List<String> getEntries() {
        return Lists.newArrayList(keyEntries);
    }

    public PropertyKey removePrefixEntry(int count) {
        if (keyEntries.length <= count) {
            return new PropertyKey("");
        }
        return new PropertyKey(Arrays.copyOfRange(keyEntries, count, keyEntries.length));
    }

    // do not contains self.
    public Set<PropertyKey> findChildren(Set<PropertyKey> candidates) {
        Set<PropertyKey> result = new HashSet<>(candidates);
        for (int i = 0; i < keyEntries.length; i ++) {
            Iterator<PropertyKey> iterator = result.iterator();
            while (iterator.hasNext()) {
                PropertyKey candidate = iterator.next();
                if (candidate.keyEntries.length <= i
                        || candidate.keyEntries.length == keyEntries.length
                        || !candidate.keyEntries[i].equals(keyEntries[i])) {
                    iterator.remove();
                }
            }
        }
        return result;
    }

}
