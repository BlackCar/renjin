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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.pig.FuncSpec;
import org.apache.pig.impl.PigContext;
import org.apache.pig.scripting.ScriptEngine;
import org.apache.pig.tools.pigstats.PigStats;

import static java.lang.String.format;

/**
 * Pig ScriptEngine that can be used to run pig, this is largely cribbed from the jython engine,
 * partly because I know python / jython partly because that is a working example
 *
 * @author Greg Bowyer
 */
public class RenjinPigScriptEngine extends ScriptEngine {

    @Override
    public void registerFunctions(String path, String namespace, PigContext pigContext) throws IOException {
        RenjinInterpretorContext.registerJars(pigContext);

        namespace = (namespace == null) ? "" : namespace + NAMESPACE_SEPARATOR;

        RenjinInterpretorContext.load(path);
        for (String name : RenjinInterpretorContext.getFunctionPrintNames()) {
            String spec = format("%s('%s', '%s')", RenjinPigFunction.class.getCanonicalName(), path, name);
            pigContext.registerFunction(namespace + name, new FuncSpec(spec));
        }
        pigContext.addScriptFile(path);
    }

    @Override
    protected Map<String, List<PigStats>> main(PigContext context, String scriptFile) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected Map<String, Object> getParamsFromVariables() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected String getScriptingLang() {
        return "R";
    }

}
