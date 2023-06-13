/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.arsc.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class FilterIterator<T> implements Iterator<T>, Predicate<T> {
    private final Iterator<? extends T> iterator;
    private T mNext;
    private final Predicate<T> mSecondTester;
    public FilterIterator(Iterator<? extends T> iterator, Predicate<T> secondTester){
        this.iterator = iterator;
        this.mSecondTester = secondTester;
    }
    public FilterIterator(Iterator<? extends T> iterator){
        this(iterator, null);
    }

    @Override
    public boolean test(T item){
        return item != null;
    }

    @Override
    public boolean hasNext() {
        return getNext() != null;
    }
    @Override
    public T next() {
        T item = getNext();
        if(item == null){
            throw new NoSuchElementException();
        }
        mNext = null;
        return item;
    }
    private T getNext(){
        if(mNext == null) {
            while (iterator.hasNext()) {
                T item = iterator.next();
                if (testAll(item)) {
                    mNext = item;
                    break;
                }
            }
        }
        return mNext;
    }
    private boolean testAll(T item){
        if(item == null || !test(item)){
            return false;
        }
        return mSecondTester == null
                || mSecondTester.test(item);
    }
}
