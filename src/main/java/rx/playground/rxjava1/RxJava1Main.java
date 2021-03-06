package rx.playground.rxjava1;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.playground.*;
import rx.playground.Error;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RxJava1Main {

    public static void main() throws InterruptedException {
        Observer<String> stringObserver = PrintObserver.create();
        Observable.just("hogehoge", "mogemoge")
                .subscribe(stringObserver);

        Observable.range(0, 4)
                .take(2) // 0,1 が2回
                .repeat(2)
//                .take(2) // 0,1 だけ
                .subscribe(PrintObserver.create());

        Observable.interval(1, TimeUnit.SECONDS)
                .take(5)
                .subscribe(PrintObserver.create());

        Observable.range(0, 3)
                .publish()
                .defer(() -> Observable.range(2, 3))
                .subscribe(PrintObserver.create());

        Observable
                .create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        System.out.println("integer call");
                        subscriber.onNext(1);
                        subscriber.onNext(2);
                        subscriber.onCompleted();
                    }
                })
                .doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        System.out.println("doOnNext : " + integer);
                    }
                })
                .flatMap(new Func1<Integer, Observable<?>>() { // return merge(map(func));
                    @Override
                    public Observable<?> call(Integer integer) {
                        return Observable.create(new Observable.OnSubscribe<String>() {
                            @Override
                            public void call(Subscriber<? super String> subscriber) {
                                System.out.println("string flatMap call");
                                subscriber.onNext("toString : " + integer.toString());
                                subscriber.onCompleted();
                            }
                        });
                    }
                })
                .subscribe(PrintObserver.create());

        Observable.merge(Observable.just(100), Observable.range(0, 3), Observable.just(200))
                .subscribe(PrintObserver.create());

        Observable.range(0, 10)
                .skip(5)
                .take(2)
//                .limit(2) take(count)を内部で読んでるだけなので同じ
                .subscribe(PrintObserver.create());

        Observable.range(0, 5)
                .doOnNext(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        // 0 - 4の値は来る
                        System.out.println(integer);
                    }
                })
                .skip(2)
                .subscribe(PrintObserver.create());  // 2 - 4の値が来る

        Observable<Integer> integerObservable = Observable
                .create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        IntStream.range(0, 5).forEach(v -> {
                            System.out.println(v);
                            subscriber.onNext(v);
                        });
                        System.out.println("Completed");
                        subscriber.onCompleted();
                    }
                })
                .cache();
        integerObservable.subscribe(PrintObserver.create());
        integerObservable.subscribe(PrintObserver.create());

        Observable<Integer> integerObservable1 = Observable.range(0, 10);
        Observable<Integer> integerObservable2 = Observable.range(10, 10);

        Observable
                .combineLatest(integerObservable1, integerObservable2, new Func2<Integer, Integer, String>() {
                    @Override
                    public String call(Integer integer, Integer integer2) {
                        System.out.println(integer);
                        System.out.println(integer2);
                        return Integer.toString(integer + integer2);
                    }
                })
                .subscribe(PrintObserver.create());

        Observable<String> stringObservable = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext("test");
            }
        });

        Observable.merge(stringObservable.doOnNext(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        System.out.println("doOnNext 1");
                    }
                }),

                stringObservable.doOnNext(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        System.out.println("doOnNext 2");
                    }
                }))
                .subscribe(PrintObserver.create());

        PublishSubject<String> stringPublishSubject = PublishSubject.create();
        Observable<String> stringObservable1 = stringPublishSubject.asObservable();
        stringObservable1
                .flatMap(new Func1<String, Observable<String>>() {
                    @Override
                    public Observable<String> call(String s) {
                        return Observable.just(s)
                                .onErrorResumeNext(throwable -> Observable.empty());
//                                .onErrorReturn(throwable -> "catch error");
                    }
                })
                .subscribe(PrintObserver.create());

        stringPublishSubject.onNext("test");
        stringPublishSubject.onError(new Exception());
//        stringPublishSubject.onCompleted();
        stringPublishSubject.onNext("test");// 無効.onCompleted or onErrorを読んだ後なので、onNextは呼ばれない


        Observable
                .create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        subscriber.onNext("test");
                        subscriber.onNext("test");// onNextがちゃんと呼ばれる
                    }
                })
                .flatMap(new Func1<String, Observable<?>>() {
                    @Override
                    public Observable<?> call(String s) {
                        return Observable
                                .create(new Observable.OnSubscribe<String>() {
                                    @Override
                                    public void call(Subscriber<? super String> subscriber) {
                                        subscriber.onNext(s);
                                        subscriber.onError(new Exception());
                                    }
                                })
                                // 上のObservableのonErrorやCompleteが発火すると困るのでエラーハンドリングして呼ばれないようにする
                                .onErrorReturn(throwable -> "catch error");
//                                .onErrorResumeNext(throwable -> Observable.empty());
                    }
                })
                .subscribe(PrintObserver.create());

        Error.run();
        SingleSample.run();
        SubjectSample.run();
        SubscribeOnTraining.run();
        Repeat.run();
        Do.run();

        Observable.range(0, 3)
                .repeat(2)
                .subscribe(PrintObserver.create());

        Observable.range(0, 3)
                .collect(() -> new ArrayList<>(), (Action2<List<Integer>, Integer>) (integers, integer) -> integers.add(integer))
                .subscribe(PrintObserver.create());

        Observable.merge(
                Observable.just(1).flatMap(new Func1<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(Integer integer) {
                        return Observable.just(integer).subscribeOn(Schedulers.newThread());
                    }
                }),
                Observable.just(2).subscribeOn(Schedulers.newThread()))
                .collect(() -> new ArrayList<>(), (Action2<List<Integer>, Integer>) (integers, integer) -> {
                            System.out.println("Thread : " + Thread.currentThread().getName());
                            System.out.println("integer : " + integer);
                            integers.add(integer);
                        }
                )
                .flatMap((Func1<List<Integer>, Observable<Integer>>) integers -> Observable.just(integers.get(1)))
                .subscribe(PrintObserver.create());


        Observable.range(0, 8)
                .buffer(4)
                .subscribe(PrintObserver.create());

        Observable.range(0, 10)
                .flatMap(new Func1<Integer, Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call(Integer integer) {
                        System.out.println("integer : " + integer);
                        return Observable.just(integer);
                    }
                })
                .subscribe(PrintObserver.create());

        Variable<String> stringVariable = new Variable<>("test");

        Subscription subscription = stringVariable.asObservable().subscribe(PrintObserver.create());
        stringVariable.set("hogehoge");
        subscription.unsubscribe();
        stringVariable.asObservable().subscribe(PrintObserver.create());
        System.out.println(stringVariable.get());

        Thread.sleep(1000);

        Observable<Integer> a = Observable.range(0, 1);
        Observable<Integer> b = Observable.range(1, 1);

        Observable<Integer> o1 = Observable.just(1, 3, 5);
        Observable<Integer> o2 = Observable.just(2, 4, 6);

        Observable
                .zip(o1, o2, (d1, d2) -> d1 + " + " + d2 + " = " + (d1 + d2))
                .subscribe(System.out::println);

        Observable<Integer> integerObservable3 = Observable
                .create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        System.out.println("integerObservable3");
                        subscriber.onNext(1);
                        subscriber.onCompleted();
                    }
                })
                .delay(2, TimeUnit.SECONDS);

        Observable<Integer> integerObservable4 = Observable
                .create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(Subscriber<? super Integer> subscriber) {
                        System.out.println("integerObservable4");
                        subscriber.onNext(2);
                        subscriber.onCompleted();
                    }
                })
                .delay(100, TimeUnit.MILLISECONDS);

        Observable
                .combineLatest(integerObservable3.subscribeOn(Schedulers.newThread()), integerObservable4.subscribeOn(Schedulers.newThread()), new Func2<Integer, Integer, List<Integer>>() {
                    @Override
                    public List<Integer> call(Integer integer, Integer integer2) {
                        System.out.println("" + integer + integer2);
                        List<Integer> integers = new ArrayList<Integer>();
                        integers.add(integer);
                        integers.add(integer2);
                        return integers;
                    }
                })
                .map(integers -> {
                    return integers
                            .stream()
                            .map(integer -> {
                                return get(integer);
                            })
                            .collect(Collectors.toList());
                })
                .flatMap(observables -> {
                    return Observable.concat(observables)
                            .doOnNext(s1 -> System.out.println(s1));
                })
                .subscribeOn(Schedulers.newThread())
                .subscribe(PrintObserver.create("combineLatest"));

        Observable<Integer> aa = a.filter(integer -> {
            System.out.println("aa : " + integer);
            return integer % 2 == 0;
        });

        Observable<Integer> aaa = a.filter(integer -> {
            System.out.println("aaa : " + integer);
            return integer % 2 != 0;
        });

        Observable.merge(aa, aaa)
                .subscribe(PrintObserver.create("merge"));

        Thread.sleep(5000);

//        start();
        start2();

        Thread.sleep(3000);
    }

    static void start() {
        // 直列実行になるonNextが1 - 2 - 3...となる
        Observable.concat(
                Observable.range(1, 5)
                        .map(x -> fatTask(x)))
                .subscribe(x -> System.out.println("start onNext : " + x));
    }

    static void start2() throws InterruptedException {
//        Observable.concat(
//                Observable.range(1, 5)
//                        .map(x -> fatTask(x)))
//                .concatWith(fatTask2(1)) // 1-5まで実行されてから、fatTask2(1)が実行される
//                .subscribe(x -> System.out.println("start2 onNext : " + x));

        // onNextは順番になるが、fatTaskとfatTask2は実行される
//        Observable.concat(
//                Observable.range(1, 5)
//                        .map(x -> fatTask(x)))
//                .flatMap(integer -> fatTask2(integer))
//                .subscribe(x -> System.out.println("start2 onNext : " + x));

        // こうすればすべて直列実行になる
//        fatTask(1) : start.
//        fatTask2(1) : start.
//        start2 onNext : 1
//        fatTask(2) : start.
//        fatTask2(2) : start.
//        start2 onNext : 2
//        fatTask(3) : start.
//        fatTask2(3) : start.
//        start2 onNext : 3
//        fatTask(4) : start.
//        fatTask2(4) : start.
//        start2 onNext : 4
//        fatTask(5) : start.
//        fatTask2(5) : start.
//        start2 onNext : 5
        Observable.concat(
                Observable.range(1, 5)
                        .map(x -> fatTask(x)
                                .flatMap(integer -> fatTask2(integer))))
                .subscribe(x -> System.out.println("start2 onNext : " + x));

        Subscription subscription = Observable
                .create(emitter -> {
                    emitter.onNext("hogehoge");
                    emitter.onCompleted();
                })
                .subscribeOn(Schedulers.io())
                .doOnNext(o -> System.out.println("be delay doOnNext : " + o))
                .delay(1, TimeUnit.SECONDS)
                .doOnNext(o -> System.out.println("delay doOnNext : " + o))
                .subscribe(PrintObserver.create());

        subscription.unsubscribe();

        Thread.sleep(3000L);
    }

    private static final Random rand = new Random();
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);

    // ランダムにスリープした後 x を onNext する
    private static Observable<Integer> fatTask(final int x) {
        return Observable.create(subscriber -> {
            long sleep = (long) (rand.nextDouble() * 1000L);
            System.out.println("fatTask(" + x + ") : start.");

            executor.schedule(() -> {
                subscriber.onNext(x);
                subscriber.onCompleted();
            }, sleep, TimeUnit.MILLISECONDS);
        });
    }

    private static Observable<Integer> fatTask2(final int x) {
        return Observable.create(subscriber -> {
            long sleep = (long) (rand.nextDouble() * 1000L);
            System.out.println("fatTask2(" + x + ") : start.");

            executor.schedule(() -> {
                subscriber.onNext(x);
                subscriber.onCompleted();
            }, sleep, TimeUnit.MILLISECONDS);
        });
    }

    public static Observable<Integer> get(Integer integer) {
        return Observable.just(integer);
    }
}