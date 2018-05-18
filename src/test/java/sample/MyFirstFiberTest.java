package sample;

import org.junit.Test;

public class MyFirstFiberTest {

    @Test
    public void test() throws Exception {
        MyFirstFiber.doRun();
        System.out.println("YAHOO");
    }


}
