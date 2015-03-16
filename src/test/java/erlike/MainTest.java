package erlike;

import org.junit.*;
import static erlike.Library.*;

@Ignore
public class MainTest {
    public static class TestProc extends Proc {
        private final String saying;
        public TestProc(String saying) {
            this.saying = saying;
        }

        @Override
        protected void main() throws Exception {
            System.out.println("Here I am! " + self());

            for (int i = 0; i < 2; i++)
                receive(new PartialConsumer()
                    .match(String.class, s ->
                        System.out.println("I got: " + s + "\n" + this.saying))
                    .otherwise(x ->
                        System.out.println("I don't get it...")
                ));

            System.out.println("I've done my job!");
        }
    }

    @Test
    public void readmeExample() throws InterruptedException {
        Node test = new Node("test");

        final Pid p1 = test.spawn(() -> {
            System.out.println("first process running as " + self());
            receive(msg ->
                    System.out.println("I got: " + msg));
        });

        test.spawn(pid -> {
            System.out.println("second process running as " + self());
            Thread.sleep(500);
            pid.send("Hello from " + self());
        }, p1);
    }
}
