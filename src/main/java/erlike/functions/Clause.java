package erlike.functions;

/**
 * Created by alex on 11/18/16.
 */
interface Clause extends Lambda.One<Object> {
  boolean matches(Object arg);
}
