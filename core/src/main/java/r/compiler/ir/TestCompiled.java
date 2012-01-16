package r.compiler.ir;

import r.compiler.CompiledBody;
import r.lang.Context;
import r.lang.DoubleVector;
import r.lang.Environment;
import r.lang.Null;
import r.lang.SEXP;
import r.lang.Vector;

public class TestCompiled implements CompiledBody {

  @Override
  public SEXP eval(Context context, Environment rho) {
    
    return Null.INSTANCE;
   
  }

}
