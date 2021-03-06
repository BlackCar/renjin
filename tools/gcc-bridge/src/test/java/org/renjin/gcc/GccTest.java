package org.renjin.gcc;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.renjin.gcc.gimple.GimpleFunction;
import org.renjin.gcc.gimple.GimpleParser;
import org.renjin.gcc.runtime.CharPtr;
import org.renjin.gcc.runtime.DoublePtr;

import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static java.lang.Double.NaN;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class GccTest {

	@Test
	public void simpleTest() throws Exception {

    Class clazz = compile("area.c", "Area");
		
		// try to load class
		Method method = clazz.getMethod("circle_area", double.class);

    Double value = (Double) method.invoke(null, 2d);

    assertThat(value, closeTo(12.56, 0.01));
	}

  @Test
  public void pointers() throws Exception {
    Class clazz = compile("pointers.c", "Pointers");

    Method method = clazz.getMethod("sum_array", DoublePtr.class, int.class);
    Double result = (Double)method.invoke(null, new DoublePtr(15, 20, 300), 3);
    
    assertThat(result, equalTo(335d));

    Method fillMethod = clazz.getMethod("fill_array", DoublePtr.class, int.class);
    DoublePtr ptr = new DoublePtr(new double[5]);
    fillMethod.invoke(null, ptr, ptr.array.length);
    
    System.out.println(Arrays.toString(ptr.array));
  }

  @Test
  public void functionPointers() throws Exception {
    Class clazz = compile("funptr.c", "FunPtr");
    Method method = clazz.getMethod("sum_array", DoublePtr.class, int.class);
    Double result = (Double)method.invoke(null, new DoublePtr(1,4,16), 3);
    System.out.println(result);
  }
  
  @Test
  public void calls() throws Exception {
    
    Class clazz = compile("calls.c", "Calls");
    Method sqrtMethod = clazz.getMethod("testsqrt", double.class);
    assertThat((Double)sqrtMethod.invoke(null, 4d), equalTo(3d));

  }

  @Test
  public void boolToInt() throws Exception {
    Class clazz = compile("bool2int.c", "BoolInt");

    Method method = clazz.getMethod("test", int.class);
    int result = (Integer)method.invoke(null, 0);

    assertThat(result, equalTo(5));

  }

  @Test
  public void distBinary() throws Exception {
    Class clazz = compile("distbinary.c", "DistBinary");
  }
  
  
  @Test
  public void chars() throws Exception { 
    Class clazz = compile("chars.c", "Chars");
    
    Method method = clazz.getMethod("circle_name");
    CharPtr ptr = (CharPtr) method.invoke(null);
    
    assertThat(ptr.asString(), equalTo("Hello world"));
    
    method = clazz.getMethod("test_first_char");
    Integer result = (Integer) method.invoke(null);
    
    assertThat(result, equalTo((int)'h'));
    
    method = clazz.getMethod("unmarshall");
    result = (Integer) method.invoke(null);
    
    assertThat(result, equalTo((int)'e'));
  }
  
  
  
  @Test
  public void negate() throws Exception {
    Class clazz = compile("negate.c", "Negate");
    
    Method method = clazz.getMethod("negate", double.class);
    assertThat((Double)method.invoke(null, 1.5), equalTo(-1.5));
    assertThat((Double)method.invoke(null, -1.5), equalTo(1.5));

  }

  @Test
  public void fpComparison() throws Exception {
    Class clazz = compile("fpcmp.c", "FpCmp");

    assertThat(call(clazz, "lessThan", -2.4, -2.3), equalTo(1));
    assertThat(call(clazz, "lessThan", -2.4, -2.4), equalTo(0));
    assertThat(call(clazz, "lessThan", 1.1, 1.2), equalTo(1));
    assertThat(call(clazz, "lessThan", 1.5, 1.2), equalTo(0));
    assertThat(call(clazz, "lessThan", NaN, NaN), equalTo(0));
    assertThat(call(clazz, "lessThan", NaN, 42), equalTo(0));
    
    assertThat(call(clazz, "flessThan", -2.4f, -2.3f), equalTo(1));
    assertThat(call(clazz, "flessThan", -2.4f, -2.4f), equalTo(0));

    assertThat(call(clazz, "lessThanEqual", -2.4, -2.3), equalTo(1));
    assertThat(call(clazz, "lessThanEqual", -2.4, -2.4), equalTo(1));
    assertThat(call(clazz, "lessThanEqual", 1.1, 1.2), equalTo(1));
    assertThat(call(clazz, "lessThanEqual", 1.5, 1.2), equalTo(0));
    assertThat(call(clazz, "lessThanEqual", NaN, NaN), equalTo(0));
    assertThat(call(clazz, "lessThanEqual", NaN, 42), equalTo(0));


    assertThat(call(clazz, "greaterThan", -2.4, -2.3), equalTo(0));
    assertThat(call(clazz, "greaterThan", -2.4, -2.4), equalTo(0));
    assertThat(call(clazz, "greaterThan", 1.1, 1.2), equalTo(0));
    assertThat(call(clazz, "greaterThan", 1.5, 1.2), equalTo(1));
    assertThat(call(clazz, "greaterThan", NaN, NaN), equalTo(0));
    assertThat(call(clazz, "greaterThan", NaN, 42), equalTo(0));

    assertThat(call(clazz, "greaterThanEqual", -2.4, -2.3), equalTo(0));
    assertThat(call(clazz, "greaterThanEqual", -2.4, -2.4), equalTo(1));
    assertThat(call(clazz, "greaterThanEqual", 1.1, 1.2), equalTo(0));
    assertThat(call(clazz, "greaterThanEqual", 1.5, 1.2), equalTo(1));
    assertThat(call(clazz, "greaterThanEqual", NaN, NaN), equalTo(0));
    assertThat(call(clazz, "greaterThanEqual", NaN, 42), equalTo(0));
  }

  private Integer call(Class clazz, String methodName, double x) throws Exception {
    Method method = clazz.getMethod(methodName, double.class);
    return (Integer) method.invoke(null, x);
  }

  private int call(Class clazz, String methodName, double x, double y) throws Exception {
    Method method = clazz.getMethod(methodName, double.class, double.class);
    return (Integer) method.invoke(null, x, y);
  }
  
  private int call(Class clazz, String methodName, float x, float y) throws Exception {
    Method method = clazz.getMethod(methodName, float.class, float.class);
    return (Integer) method.invoke(null, x, y);
  }

  @Test
  public void fortranTest() throws Exception {
//    Class clazz = compile(Lists.newArrayList("dqrls.f", "dqrdc2.f"), "Dqrls");


  }


  
  
  private Class<?> compile(String source, String className) throws Exception {
    return compile(Lists.newArrayList(source), className);
  }

  private Class<?> compile(List<String> sources, String className) throws Exception {

    Gcc gcc = new Gcc();
    List<GimpleFunction> functions = Lists.newArrayList();

    for(String sourceName : sources) {
      File source = new File(getClass().getResource(sourceName).getFile());
      String gimple = gcc.compileToGimple(source);
      System.out.println(gimple);

      GimpleParser parser = new GimpleParser(CallingConventions.fromFile(source));
      functions.addAll(parser.parse(new StringReader(gimple)));
    }

    return compileGimple(className, functions);
  }

  private Class<?> compileGimple(String className, List<GimpleFunction> functions) throws Exception {
    
    GimpleCompiler compiler = new GimpleCompiler();
    compiler.setJimpleOutputDirectory(new File("target/test-jimple"));
    compiler.setOutputDirectory(new File("target/test-classes"));
    compiler.setPackageName("org.renjin.gcc");
    compiler.setClassName(className);
    compiler.setVerbose(true);
    compiler.getMethodTable().addReferenceClass(RStubs.class);
    compiler.compile(functions);

    return Class.forName("org.renjin.gcc." + className);
  }

  @Test
  public void structFromGcc4_4_6() throws Exception {
    GimpleParser parser = new GimpleParser(new CallingConvention());
    List<GimpleFunction> fns = parser.parse(new InputStreamReader(getClass().getResourceAsStream("approx.gimple")));

    Class clazz = compileGimple("Gcc4_4_6", fns);
  }

}
