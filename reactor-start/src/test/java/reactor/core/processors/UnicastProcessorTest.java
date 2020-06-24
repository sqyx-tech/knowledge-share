package reactor.core.processors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.subscriber.AssertSubscriber;
import reactor.test.StepVerifier;
import reactor.util.concurrent.Queues;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

/**
 *  UnicastProcessor： <br>
 *  1. 内置缓存队列 <br>
 *  2. 只能有一个订阅者 <br>
 * @author lianghong
 * @date 2020/6/12
 */
public class UnicastProcessorTest {

    @Test
    @DisplayName("通过发出error拒绝多订阅者")
    public void secondSubscriberRejectedProperly() {
        UnicastProcessor<Integer> unicastProcessor = UnicastProcessor.create();

        AssertSubscriber ts_1 = AssertSubscriber.create();
        AssertSubscriber ts_2 = AssertSubscriber.create();
        unicastProcessor.subscribe(ts_1);
        unicastProcessor.subscribe(ts_2);

        unicastProcessor.onNext(1);
        unicastProcessor.onComplete();

        ts_1.assertValues(1)
                .assertComplete()
                .assertNoError();

        // 第二个订阅者会接收到一个错误信号
        ts_2.assertError(IllegalStateException.class);
        Assertions.assertTrue(unicastProcessor.hasCompleted(), "no completed?");
    }

    @Test
    @DisplayName("多线程生产数据")
    public void multiThreadProducer() {
        UnicastProcessor<Integer> unicastProcessor = UnicastProcessor.create();
        // 可以通过sink直接生产数据
        FluxSink<Integer> fluxSink = unicastProcessor.sink();
        int nThread = 5;
        int perThreadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(nThread);
        for (int i = 0; i < nThread; i++) {
            Runnable runnable = () -> {
                for (int j = 0; j < perThreadCount; j++) {
                    fluxSink.next(j);
                }
            };
            executorService.submit(runnable);
        }
        StepVerifier
                .create(unicastProcessor)
                .expectNextCount(nThread * perThreadCount)
                .thenCancel()
                .verify();
        executorService.shutdownNow();
    }

    @Test
    @DisplayName("自定义缓存队列")
    public void customDefineCacheQueue() {
        Queue<Integer> queue = Queues.<Integer>get(100).get();
        UnicastProcessor unicastProcessor = UnicastProcessor.create(queue);
        unicastProcessor.onComplete();
        Assertions.assertTrue(unicastProcessor.hasCompleted(), "not completed?");
    }

    @Test
    @DisplayName("队列溢出自定义处理策略")
    public void overflowQueueTerminate() {
        Consumer<? super Integer> onOverflow = item -> {
            System.out.println("缓存队列已满，无法继续产生数据！");
        };
        LinkedBlockingDeque<Integer> queue = new LinkedBlockingDeque<>(1);
        UnicastProcessor unicastProcessor = UnicastProcessor.create(queue, onOverflow, () -> {});

        StepVerifier
                .create(unicastProcessor, 0L)
                .then(() -> {
                    FluxSink sink = unicastProcessor.sink();
                    for (int i = 0; i < 20; i++) {
                        sink.next(i);
                    }
                    sink.complete();
                })
                .thenRequest(1)
                .expectNext(0)
                .expectError(IllegalStateException.class)
                .verify();
    }
}
