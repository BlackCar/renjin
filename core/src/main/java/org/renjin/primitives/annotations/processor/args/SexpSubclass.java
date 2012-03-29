package org.renjin.primitives.annotations.processor.args;

import org.renjin.primitives.annotations.processor.WrapperSourceWriter;
import org.renjin.primitives.annotations.processor.JvmMethod.Argument;
import org.renjin.sexp.SEXP;


/**
 * Handles checked narrowing of types to declared subclass of SEXP
 * @author alex
 *
 */
public class SexpSubclass extends ArgConverterStrategy {

  public SexpSubclass(Argument formal) {
    super(formal);
  }

  public static boolean accept(Argument formal) {
    return SEXP.class.isAssignableFrom(formal.getClazz());
  }

  @Override
  public String conversionExpression(String argumentExpression) {
    return "checkedSubClass(" + argumentExpression + ")";
  }

  @Override
  public String conversionStatement(String tempLocal, String argumentExpression) {
    return "try { " + tempLocal + " = (" + WrapperSourceWriter.toJava(formal.getClazz()) + ")(" + argumentExpression + ");" +
    		" } catch(ClassCastException cce) { throw new ArgumentException(); }";
  }

  @Override
  public String getTestExpr(String argLocal) {
    return argLocal + " instanceof " + WrapperSourceWriter.toJava(formal.getClazz());
  }
  
  

}
