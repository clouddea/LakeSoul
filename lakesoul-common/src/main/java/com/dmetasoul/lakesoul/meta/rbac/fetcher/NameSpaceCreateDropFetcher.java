/*
 * Copyright [2022] [DMetaSoul Team]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.dmetasoul.lakesoul.meta.rbac.fetcher;

import org.aspectj.lang.JoinPoint;

import java.util.LinkedList;
import java.util.List;

/**
 * Fetch namespace name string from the first argument
 */
public class NameSpaceCreateDropFetcher implements AuthZFetcher{
    @Override
    public List<String> getObject(JoinPoint point) {
        String object = (String) point.getArgs()[0];
        LinkedList<String> objects = new LinkedList<>();
        objects.add(object);
        return objects;
    }
}


