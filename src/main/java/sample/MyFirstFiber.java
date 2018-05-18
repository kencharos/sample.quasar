package sample;


import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.SuspendExecution;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 OUTPUT:

 start
 all fiber start, get the result
 ForkJoinPool-default-fiber-pool-worker-0,F3 end.
 ForkJoinPool-default-fiber-pool-worker-0,F2 end.
 ForkJoinPool-default-fiber-pool-worker-0,F1 end.
 1
 2
 3
 */
public class MyFirstFiber {

    public static void main(String...args) throws Exception {
        doRun(args);
    }

    public static void doRun(String...args) throws Exception {
        System.out.println("start");

        List<Fiber<String>> fs = new ArrayList<>();

        fs.add(of("F1",100, () -> "1").start());
        fs.add(of("F2",50, () -> "2").start());
        fs.add(of("F3",10, () -> "3").start());

        System.out.println("all fiber start, get the result");

        for(Fiber<String> f : fs) {
            System.out.println(f.get());
        }
    }

    public static<V> Fiber<V> of(String name, int sleep, Supplier<V> f) {
        return new Fiber<V>() {

            @Override
            protected V run() throws SuspendExecution, InterruptedException {
                Fiber.sleep(sleep);

                System.out.println(Thread.currentThread().getName() + "," + name + " end.");
                return f.get();
            }
        };
    }

}
