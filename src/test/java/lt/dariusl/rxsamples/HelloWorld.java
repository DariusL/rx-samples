package lt.dariusl.rxsamples;

import io.reactivex.Observable;
import org.junit.jupiter.api.Test;

class HelloWorld {

    @Test
    void testHelloWorld() {
        Observable<String> wasup = Observable
                .just("Hello, world!");
        wasup.subscribe(System.out::println);
    }
}
