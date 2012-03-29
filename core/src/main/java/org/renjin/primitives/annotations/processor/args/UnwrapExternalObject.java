package org.renjin.primitives.annotations.processor.args;

import org.renjin.primitives.annotations.processor.JvmMethod.Argument;
import org.renjin.sexp.ExternalExp;


public class UnwrapExternalObject extends ArgConverterStrategy {

  public UnwrapExternalObject(Argument formal) {
    super(formal);
  }

  public static boolean accept(Argument formal) {
    return !formal.getClazz().isPrimitive();
  }

  @Override
  public String conversionExpression(String argumentExpression) {
    return "WrapperRuntime.<" + formal.getClazz().getName() + ">unwrapExternal(" + argumentExpression + ")";
  }

  @Override
  public String getTestExpr(String argLocal) {
    return argLocal + " instanceof " + ExternalExp.class.getSimpleName() + " && " +
        "((" + ExternalExp.class.getSimpleName() + ")" + argLocal + ").getValue() instanceof " + formal.getClazz().getName();
  }

}
