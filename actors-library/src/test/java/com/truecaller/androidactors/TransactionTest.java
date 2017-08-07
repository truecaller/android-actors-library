/*
 * Copyright (C) 2017 True Software Scandinavia AB
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
 */

package com.truecaller.androidactors;

import com.truecaller.androidactors.ActorService.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class TransactionTest {

    @Mock
    private Message mMessage;

    @Mock
    private Object mImpl;

    @Mock
    private FailureHandler mFailureHandler;

    @Before
    public void startUp() {
        Transaction.clearPool();
    }

    @Test
    public void reusingTest() {
        Transaction transaction = Transaction.obtain(mImpl, mMessage, mFailureHandler);
        transaction.recycle();
        Assert.assertSame(transaction, Transaction.obtain(mImpl, mMessage, mFailureHandler));
    }

    @Test
    public void chainTest() {
        Transaction transactions[] = new Transaction[] {
            Transaction.obtain(mImpl, mMessage, mFailureHandler),
            Transaction.obtain(mImpl, mMessage, mFailureHandler),
            Transaction.obtain(mImpl, mMessage, mFailureHandler)
        };

        transactions[2].recycle();
        transactions[1].recycle();
        transactions[0].recycle();

        Assert.assertSame(transactions[1], transactions[0].next);
        Assert.assertSame(transactions[2], transactions[1].next);
        Assert.assertNull(transactions[2].next);
    }

    @Test
    public void poolLimitTest() {
        Transaction transactions[] = new Transaction[Transaction.MAX_POOL_SIZE + 2];
        for (int index = 0; index < transactions.length; ++index) {
            transactions[index] = Transaction.obtain(mImpl, mMessage, mFailureHandler);
        }

        for (int index = transactions.length - 1; index >= 0; --index) {
            transactions[index].recycle();
        }

        for (int index = transactions.length - 1; index >= transactions.length - Transaction.MAX_POOL_SIZE + 1; --index) {
            Assert.assertSame("Item at position " + (index - 1) + " links to invalid object", transactions[index], transactions[index - 1].next);
        }

        for (int index = transactions.length - Transaction.MAX_POOL_SIZE - 1; index >= 0; --index) {
            Assert.assertNull("Item at position " + index + " links to object", transactions[index].next);
        }
    }
}
