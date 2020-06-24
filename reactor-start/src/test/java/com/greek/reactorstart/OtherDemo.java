package com.greek.reactorstart;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author lanruqi
 * @date 2020/6/16
 */
public class OtherDemo {


    @Test
    public void take(){
        Flux<Integer> fluxLog = Flux.range(1, 10)
                .take(5)
                .log();

        StepVerifier.create(fluxLog, 6)
                .expectNextCount(5)
                .verifyComplete();
    }
}
