package erlike;

import org.junit.*;
import java.util.function.Consumer;

public class Main {
    public static class TestProc extends Proc {
        public TestProc(Node node, int id) { super(node, id); }

        @Override protected void main() throws Exception {
            System.out.println("Here I am!");

            for (int i = 0; i < 2; i++)
                receive(new PartialConsumer()
                    .match(String.class, s ->
                        System.out.println("I got: " + s))
                    .otherwise(x ->
                        System.out.println("I don't get it...")
                ));

            System.out.println("I've done my job!");
        }
    }

    @Test public void main() throws InterruptedException {
        Node node = new Node("test");

        Pid t = node.spawn(TestProc.class);
        t.send("Hello there.");
        t.send(12345);
       
        node.joinAll();
    }
}
