package org.renjin.maven.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.renjin.eval.Context;
import org.renjin.parser.RParser;
import org.renjin.sexp.Closure;
import org.renjin.sexp.ExpressionVector;
import org.renjin.sexp.FunctionCall;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.Symbol;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * Discovers and runs a set of R test scripts, usually from
 * src/test/R
 * 
 */
public class TestRunner {

  private String namespace;
  private TestReporter reporter;
  private List<String> defaultPackages;

  public boolean run(File sourceDirectory, File reportsDirectory, String namespaceName, List<String> defaultPackages) throws Exception {
    
    this.defaultPackages = defaultPackages;
    if(this.defaultPackages == null) {
      this.defaultPackages = Lists.newArrayList();
    }
    
    reporter = new TestReporter(reportsDirectory);
    reporter.start();
    
    this.namespace = namespaceName;
    
    if(sourceDirectory.isDirectory() && sourceDirectory.listFiles() != null) {
      for(File sourceFile : sourceDirectory.listFiles()) {
        if(sourceFile.getName().toUpperCase().endsWith(".R")) {
          try {
            reporter.startFile(sourceFile);
            executeTestFile(sourceFile);
            reporter.fileComplete();
          } catch (IOException e) {
            System.out.println("FAILURE: " + sourceFile.getName());
          }
        }
      }
    }
    
    return reporter.allTestsSucceeded();
  }

  private Context createContext() throws IOException  {

    Context ctx = Context.newTopLevelContext();
    ctx.init();      
    return ctx;
  }

  private void loadLibrary(Context ctx, String namespaceName) {
    try {
      ctx.evaluate(FunctionCall.newCall(Symbol.get("library"), Symbol.get(namespaceName)));
    } catch(Exception e) {
      System.err.println("Could not load this project's namespace (it may not have one)");
      //e.printStackTrace();
    }
  }
  

  private boolean isZeroArgFunction(SEXP value) {
    if(value instanceof Closure) {
      Closure testFunction = (Closure)value;
      if(testFunction.getFormals().length() == 0) {
        return true;
      }
    } 
    return false;
  }

  private void executeTestFile(File sourceFile) throws IOException {
    ExpressionVector source = RParser.parseSource(Files.newReaderSupplier(sourceFile, Charsets.UTF_8));
    Context ctx = createContext();
    FileObject workingDir = ctx.resolveFile(sourceFile.getParent());
    System.err.println("Working directory = " + workingDir);
    ctx.getSession().setWorkingDirectory(workingDir);
//    ctx.getSession().setStdOut(reporter.getStdOut());
//    ctx.getSession().setStdErr(reporter.getStdOut());
    
    if(defaultPackages.isEmpty()) {
      System.err.println("No default packages specified");
    }
    
    for(String pkg : defaultPackages) {
      System.err.println("Loading default package " + pkg);
      loadLibrary(ctx, pkg);
    }
    
    try {
      reporter.startFunction("root");
      ctx.evaluate(source);
      reporter.functionSucceeded();
    } catch(Exception e) {
      reporter.functionThrew(e);
    }
    
    for(Symbol name : ctx.getGlobalEnvironment().getSymbolNames()) {
      if(name.getPrintName().startsWith("test.")) {
        SEXP value = ctx.getGlobalEnvironment().getVariable(name);
        if(isZeroArgFunction(value)) {
          executeTestFunction(ctx, name);
        }
      }
    }
  }

  private void executeTestFunction(Context context, Symbol name) {
    try {
      reporter.startFunction(name.getPrintName());
      context.evaluate(FunctionCall.newCall(name));
      reporter.functionSucceeded();
    } catch(Exception e) {
      reporter.functionThrew(e);
    }
  }
}