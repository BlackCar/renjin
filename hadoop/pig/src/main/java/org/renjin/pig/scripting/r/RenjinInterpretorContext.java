/*
 * R : A Computer Language for Statistical Data Analysis
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1997--2008  The R Development Core Team
 * Copyright (C) 2003, 2004  The R Foundation
 * Copyright (C) 2010 bedatadriven
 * Copyright (C) 2012 The Renjin project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.renjin.pig.scripting.r;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import org.apache.pig.impl.PigContext;
import org.renjin.eval.Context;
import org.renjin.script.RenjinScriptEngine;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.sexp.Environment;
import org.renjin.sexp.Null;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.Symbol;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.io.Closeables.closeQuietly;
import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.newReaderSupplier;
import static org.apache.pig.scripting.ScriptEngine.getJarPath;

/**
 * Creates renjin in a fashion suitable for running the engine internal to pig, used by both the ScriptEngine
 * bridge and the related UDF function(s)
 */
class RenjinInterpretorContext {

    //private static final Logger log = LoggerFactory.getLogger(RenjinInterpretorContext.class);
    private static Object log = new Object();
    private static volatile RenjinInterpretorContext instance;

    private final RenjinScriptEngine renjinEngine;
    private final Set<String> loadedPaths = newHashSet();

    private RenjinInterpretorContext() {
        RenjinScriptEngine engine;

        // Try the "official" way first - works except for instances where META-INF service infrastructure is borken
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = (RenjinScriptEngine) manager.getEngineByName("Renjin");

        if (engine == null) {
            //log.warn("Renjin engine is not correctly registered in java services infrastructure");
            // Hack if the jar is not registered correctly - that makes the java service infrastructure not work
            RenjinScriptEngineFactory factory = new RenjinScriptEngineFactory();
            engine = factory.getScriptEngine();
        }

        checkNotNull(engine, "The Renjin R engine could not be created");
        this.renjinEngine = engine;
    }

    public static Invocable getInvocable(String path) throws IOException {
        load(path);
        return getInterpretor().renjinEngine;
    }

    static void registerJars(PigContext pigContext) {
        try {
            pigContext.scriptFiles.add(getJarPath(RenjinScriptEngine.class));
            pigContext.scriptFiles.add(getJarPath(RenjinInterpretorContext.class));
        } catch (FileNotFoundException fnf) {
            //log.warn("Renjin jar not found !", fnf);
        }
    }

    static RenjinInterpretorContext getInterpretor() {
        if (instance == null) {
            synchronized (log) {
                if (instance == null) {
                    instance = new RenjinInterpretorContext();
                }
            }
        }
        return instance;
    }

    static void load(String source) throws IOException {
        RenjinInterpretorContext context = getInterpretor();

        if (context.loadedPaths.contains(source)) {
            return;
        }

        synchronized (context) {
            Reader reader = null;
            try {
                reader = getScript(source);
                SEXP result = (SEXP) context.renjinEngine.eval(reader);
                Preconditions.checkArgument(!(result instanceof Null));
                context.loadedPaths.add(source);
            } catch (ScriptException se) {
                throw new IOException(se);
            } finally {
                closeQuietly(reader);
            }
        }
    }

    private static Reader getScript(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            return Files.newReader(file, UTF_8);
        } else {
            URL url = getResource(path);
            return newReaderSupplier(url, UTF_8).getInput();
        }
    }

    static Iterable<String> getFunctionPrintNames() {
        final Context topLevelContext = getInterpretor().renjinEngine.getTopLevelContext();
        final Environment environment = topLevelContext.getEnvironment();
        Collection<Symbol> syms = environment.getSymbolNames();
        return filter(transform(syms, new Function<Symbol, String>() {
            @Override
            public String apply(Symbol input) {
                return environment.getFrame().getFunction(topLevelContext, input) != null ? input.getPrintName() : null;
            }
        }), notNull());
    }
    
    static org.renjin.sexp.Function getFunctionForPrintName(String printName) {
        Context topLevelContext = getInterpretor().renjinEngine.getTopLevelContext();
        Environment environment = topLevelContext.getEnvironment();
        Collection<Symbol> symbols = environment.getSymbolNames();

        Symbol symbol = null;
        // Hunt the symbol
        for (Symbol sym : symbols) {
            if (sym.getPrintName().equals(printName)) {
                symbol = sym;
                break;
            }
        }
        checkNotNull(symbol);
        return environment.findFunctionOrThrow(topLevelContext, symbol);
    }
}
