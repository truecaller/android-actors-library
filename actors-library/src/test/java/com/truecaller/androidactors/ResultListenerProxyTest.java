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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ResultListenerProxyTest {
    @Mock
    private Object mResult;

    @Mock
    private MessageSender mSender;

    @Mock
    private ResultListener<Object> mListener;

    @Mock
    private TestResultListener mListenerWithExceptionTemplate;

    @Captor
    private ArgumentCaptor<Message<ResultListener<Object>, Void>> mMessageCaptor;

    @Mock
    private ActorInvokeException mException;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onResult_invoke_withResult() throws Exception {
        ResultListenerProxy proxy = new ResultListenerProxy(mSender);
        proxy.onResult(mResult);

        Mockito.verify(mSender).deliver(mMessageCaptor.capture());

        Message<ResultListener<Object>, Void> message = mMessageCaptor.getValue();
        Assert.assertNotNull(message);

        message.invoke(mListener);

        Mockito.verify(mListener).onResult(mResult);
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    public void onResult_setExceptionTemplate_withTemplateProvider() throws Exception {
        ResultListenerProxy proxy = new ResultListenerProxy(mSender);
        proxy.onResult(mResult);

        Mockito.verify(mSender).deliver(mMessageCaptor.capture());

        Message<ResultListener<Object>, Void> message = mMessageCaptor.getValue();
        Assert.assertNotNull(message);

        Mockito.doReturn(mException).when(mListenerWithExceptionTemplate).exception();

        message.invoke(mListenerWithExceptionTemplate);
        Mockito.verify(mListenerWithExceptionTemplate).onResult(mResult);

        Assert.assertSame(mException, message.exception());
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void toString_returnFromResult_resultIsMessage() {
        Message result = Mockito.mock(Message.class);
        Mockito.doReturn("STRING FROM RESULT").when(result).toString();

        ResultListenerProxy proxy = new ResultListenerProxy(mSender);
        proxy.onResult(result);

        Mockito.verify(mSender).deliver(mMessageCaptor.capture());

        Message<ResultListener<Object>, Void> message = mMessageCaptor.getValue();
        Assert.assertNotNull(message);

        Assert.assertEquals("STRING FROM RESULT", message.toString());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void toString_returnDefault_resultIsNotMessage() {
        Mockito.doReturn("A").when(mResult).toString();

        ResultListenerProxy proxy = new ResultListenerProxy(mSender);
        proxy.onResult(mResult);

        Mockito.verify(mSender).deliver(mMessageCaptor.capture());

        Message<ResultListener<Object>, Void> message = mMessageCaptor.getValue();
        Assert.assertNotNull(message);

        Assert.assertEquals(".onResult(A)", message.toString());
    }

    /**
     * Mockito can't mock two interfaces in the same time.
     * This interface is a small workaround for this problem
     */
    private interface TestResultListener extends ResultListener<Object>, ExceptionTemplateProvider {
    }
}
