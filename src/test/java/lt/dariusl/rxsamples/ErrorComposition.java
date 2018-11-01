package lt.dariusl.rxsamples;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ErrorComposition {

    public static final int RETRY_ATTEMPTS = 10;

    class ThrowableWithNumber {
        final Throwable throwable;
        final int n;

        ThrowableWithNumber(Throwable throwable, int n) {
            this.throwable = throwable;
            this.n = n;
        }
    }

    class SomeApi {
        Random random = new Random();

        Observable<Integer> randomInt() {
            return Observable.defer(new Callable<ObservableSource<? extends Integer>>() {
                int count = 0;
                @Override
                public ObservableSource<? extends Integer> call() throws Exception {
                    int i = random.nextInt();
                    count++;
                    if (count > 6) {
                        return Observable.just(i);
                    } else {
                        return Observable.error(new Exception("Couldn't generate random integer"));
                    }
                }
            });
        }
    }

    @Test
    void testTryGetRandomInteger() {
        SomeApi api = new SomeApi();

        api.randomInt()
                .subscribe(System.out::println);
    }

    @Test
    void testRetryRandomInteger() {
        SomeApi api = new SomeApi();

        api.randomInt()
                .retry(RETRY_ATTEMPTS)
                .subscribe(System.out::println);
    }

    @Test
    void testRetryRandomIntegerWithLogs() {
        SomeApi api = new SomeApi();

        api.randomInt()
                .doOnSubscribe(disposable -> System.out.println("Susbscribed on " + Util.time()))
                .doOnError(throwable -> System.out.println("error received before retry"))
                .retry(RETRY_ATTEMPTS)
                .doOnError(throwable -> System.out.println("error received after retry"))
                .subscribe(System.out::println);
    }



    @Test
    void testRetryRandomIntegerWithLogsExponentialBackoff() {
        SomeApi api = new SomeApi();

        api.randomInt()
                .doOnSubscribe(disposable -> System.out.println("Susbscribed on " + Util.time()))
                .doOnError(throwable -> System.out.println("error received before retry"))
                .retryWhen(errors ->
                        errors
                                .zipWith(
                                        Observable.range(1, RETRY_ATTEMPTS),
                                        ThrowableWithNumber::new
                                )
                                .flatMap(
                                        throwableWithNumber -> {
                                            if (throwableWithNumber.n < RETRY_ATTEMPTS) {
                                                return Observable.timer(throwableWithNumber.n, TimeUnit.SECONDS, Schedulers.trampoline());
                                            } else {
                                                return Observable.<Long>error(throwableWithNumber.throwable);
                                            }
                                        }
                                ))
                .doOnError(throwable -> System.out.println("error received after retry"))
                .subscribe(System.out::println);
    }
}
