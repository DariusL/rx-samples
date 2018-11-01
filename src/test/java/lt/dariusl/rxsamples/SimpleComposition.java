package lt.dariusl.rxsamples;

import io.reactivex.Observable;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

class SimpleComposition {

    @Test
    void testSimpleRxMap() {
        Observable.just("Egidijus", "Vidas", "Edvinas")
                .map(String::length)
                .subscribe(System.out::println);
    }

    @Test
    void testSimpleStreamMap() {
        Stream.of("Egidijus", "Vidas", "Edvinas")
                .map(String::length)
                .forEach(System.out::println);
    }

    @Test
    void testRxZip() {
        Observable
                .zip(
                        Observable.just("Egidijus", "Vidas", "Edvinas"),
                        Observable.range(1, Integer.MAX_VALUE),
                        (name, number) -> String.format("%d. %s", number, name))
                .subscribe(System.out::println);
    }

    @Test
    void testRxZipWith() {
        Observable.just("Egidijus", "Vidas", "Edvinas")
                .zipWith(
                        Observable.range(1, Integer.MAX_VALUE),
                        (name, number) -> String.format("%d. %s", number, name))
                .subscribe(System.out::println);
    }

    @Test
    void testStreamZip() {
        // wtf java streams neturi zip
    }
}
