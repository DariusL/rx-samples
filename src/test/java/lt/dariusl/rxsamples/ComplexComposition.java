package lt.dariusl.rxsamples;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

class ComplexComposition {

    private Observable<String> events() {
        return Observable
                .just(new Object())
                .repeat()
                .concatMap(event -> Observable.just(event).delay(1, TimeUnit.SECONDS, Schedulers.trampoline()))
                .map(something -> Util.time())
                .doOnNext(instant -> System.out.printf("Emiting an event at %s\n", instant));
    }

    class SomeApi {
        Random random = new Random();

        Observable<Integer> randomInt() {
            return Observable.defer(() -> Observable.just(random.nextInt()));
        }
    }

    @Test
    void testCallOnEvent() throws InterruptedException {
        SomeApi api = new SomeApi();
        events()
                .take(10)
                .flatMap(event -> api.randomInt())
                .subscribe(
                        integer -> System.out.printf("Received onNext %d\n", integer),
                        Throwable::printStackTrace,
                        () -> System.out.println("Completed")
                );
    }

    @Test
    void testCallOnEventWithTimestamp() throws InterruptedException {
        SomeApi api = new SomeApi();
        events()
                .take(10)
                .flatMap(event -> api
                        .randomInt()
                        .map(integer -> String.format("Integer %d at %s", integer, event)))
                .subscribe(
                        string -> System.out.printf("Received onNext %s\n", string),
                        Throwable::printStackTrace,
                        () -> System.out.println("Completed")
                );
    }
}
