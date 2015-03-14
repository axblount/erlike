package erlike;

import org.junit.*;
import static erlike.BIFs.*;

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
    public void main() throws InterruptedException {
        Node node = new Node("test");

        Pid t = node.spawn(TestProc.class, "Sounds good!");
        Pid what = node.spawn(() -> {
            System.out.println("Anonymous baby!");
            receive(o -> System.out.println("got: " + o));
            exit();
            System.out.println("This shouldn't print!");
        });
        t.send("Hello there.");
        t.send(12345);
        what.send("A SECRET");

        node.joinAll();
    }
}
