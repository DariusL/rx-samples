package lt.dariusl.rxsamples.btexample;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


public class Measurement {
    private final Subject<Object> connectionAttempts = PublishSubject.create();
    private final BehaviorSubject<Object> terminationSignal = BehaviorSubject.create();
    private final Observable<DeviceEvent> eventStream;
    private final Observable<DataPoint> cache;
    private final long streamTimeoutMillis;
    private final Function<DataPoint, DataPoint> dataAdjustment;

    Measurement(
            final Connector connector,
            final long deviceRetryDelayMillis,
            final long streamTimeoutMillis,
            Scheduler mainScheduler,
            final Scheduler retryScheduler,
            Function<DataPoint, DataPoint> dataAdjustment,
            final BluetoothState bluetoothState,
            final String deviceAddress) {
        this.streamTimeoutMillis = streamTimeoutMillis;
        this.dataAdjustment = dataAdjustment;

        eventStream = connectionAttempts
                .take(1)
                .switchMap(o -> bluetoothState
                        .observeBluetoothEnabled()
                        .takeUntil(terminationSignal)
                        .switchMap(enabled -> {
                            if(enabled) {
                                return Observable
                                        .defer(() -> Observable.just(connector.getDevice(deviceAddress)))
                                        .flatMap(Measurement.this::setupDevice)
                                        .startWith(new ConnectionEvent(ConnectionState.CONNECTING))
                                        .retryWhen(errors -> errors.flatMap(throwable -> terminationSignal.hasValue() ?
                                                Observable.error(throwable) :
                                                Observable.timer(deviceRetryDelayMillis, TimeUnit.MILLISECONDS, retryScheduler)))
                                        .onErrorResumeNext(Observable.empty())
                                        .concatWith(Observable.just(new ConnectionEvent(ConnectionState.DISCONNECTED)));
                            } else {
                                return Observable.just(new ConnectionEvent(ConnectionState.DISCONNECTED));
                            }
                        }))
                .observeOn(mainScheduler)
                .share();
        cache = observeDataStream().cache();
        cache.subscribe(); // the cache should complete along with the main stream
    }

    public void connect() {
        connectionAttempts.onNext(null);
    }

    public Observable<DataPoint> stop() {
        terminationSignal.onNext(new Object());
        return observeDataStream();
    }

    public Observable<ConnectionState> observeConnection() {
        return eventStream
                .filter(Measurement.typeFilter(ConnectionEvent.class))
                .map(deviceEvent -> ((ConnectionEvent) deviceEvent).state)
                .distinctUntilChanged();
    }

    public Observable<DataPoint> observeDataStream() {
        return eventStream
                .filter(Measurement.typeFilter(DataEvent.class))
                .map(deviceEvent -> ((DataEvent) deviceEvent).point);
    }

    private Observable<DeviceEvent> setupDevice(final Device device) {
        return Observable
                .using(
                        () -> device,
                        this::createStream,
                        Device::release,
                        true);
    }

    private Observable<DeviceEvent> createStream(final Device device) {
        return Observable
                .concat(
                        device.start().cast(DeviceEvent.class),
                        Observable.just(new ConnectionEvent(ConnectionState.CONNECTED)),
                        createDataStream(device).timeout(streamTimeoutMillis, TimeUnit.MILLISECONDS, Schedulers.computation()))
                .takeUntil(terminationSignal)
                .concatWith(stopDevice(device));
    }

    private Observable<DataEvent> stopDevice(Device device){
        return device
                .stop()
                .cast(DataEvent.class)
                .timeout(streamTimeoutMillis, TimeUnit.MILLISECONDS, Schedulers.computation());
    }

    private Observable<DeviceEvent> createDataStream(Device device) {
        return device
                .readStream()
                .map(dataAdjustment)
                .map(DataEvent.WRAP)
                .concatWith(Observable.error(new IOException("Stream stopped prematurely!")));
    }

    public Observable<DataPoint> collectData() {
        stop();
        return cache;
    }

    private interface DeviceEvent {
    }

    private static class ConnectionEvent implements DeviceEvent {
        private final ConnectionState state;

        private ConnectionEvent(ConnectionState state) {
            this.state = state;
        }

    }

    private static class DataEvent implements DeviceEvent {
        private final DataPoint point;

        private DataEvent(DataPoint point) {
            this.point = point;
        }

        static final Function<DataPoint, DeviceEvent> WRAP = DataEvent::new;
    }

    private static <T> Predicate<? super T> typeFilter(final Class<? extends T> cls) {
        return cls::isInstance;
    }
}
