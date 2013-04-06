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
import java.io.IOException;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.renjin.sexp.Closure;
import org.renjin.sexp.Function;
import org.renjin.sexp.PairList;
import org.renjin.sexp.PrimitiveFunction;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.Symbols;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.pig.impl.util.Utils.getSchemaFromString;

/**
 * Function that is evaluated internal to pig to actually invoke R
 * @author Greg Bowyer
 */
public class RenjinPigFunction extends EvalFunc<Object> {

    private static final Schema DEFAULT_SCHEMA = new Schema(new Schema.FieldSchema(null, DataType.BYTEARRAY));

    private final String symbolName;
    private final Invocable engine;
    private final int arity;
    private final boolean varArg;

    private final Schema outputSchema;

    public RenjinPigFunction(String fileName, String symbolName) throws IOException{
        this.symbolName = symbolName;
        this.engine = RenjinInterpretorContext.getInvocable(fileName);

        Function fn = RenjinInterpretorContext.getFunctionForPrintName(symbolName);

        String rawOutputSchema = fn.getAttributes().get("pig.outputSchema").toString().replace("\"", "");
        outputSchema = (isNullOrEmpty(rawOutputSchema)) ? DEFAULT_SCHEMA : getSchemaFromString(rawOutputSchema);

        boolean hasVarArg = false;
        int foundArity = 0;

        if (fn instanceof Closure) {
            PairList formals = ((Closure)fn).getFormals();

            for (PairList.Node node : formals.nodes()) {
                if (Symbols.ELLIPSES.equals(node.getTag())) {
                    hasVarArg = true;
                }
                foundArity++;
            }
        } else if (fn instanceof PrimitiveFunction) {
            // There is no readily apparent way to save the user from themselves here
            hasVarArg = true;
        } else {
            throw new IllegalArgumentException("Unknown function type: " + fn.getClass().getCanonicalName());
        }

        this.varArg = hasVarArg;
        this.arity = foundArity;
    }

    @Override
    public Object exec(Tuple input) throws IOException {
        try {
            SEXP sexp;
            if (arity > 0 || varArg) {
                Object[] args = new Object[this.varArg ? input.size() : arity];

                for (int i = 0; i < args.length; i++) {
                    args[i] = input.get(i);
                }

                sexp = (SEXP) this.engine.invokeFunction(symbolName, args);
            } else {
                sexp = (SEXP) this.engine.invokeFunction(symbolName);
            }
            return convertToPig(sexp);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public Object convertToPig(SEXP sexp) {
        if (this.outputSchema == null) {
            return sexp.isNumeric() ? sexp.asReal() : sexp.toString();

        } else {
            if (this.outputSchema.size() > 1) {

            }
            return sexp.isNumeric() ? sexp.asReal() : sexp.toString();
        }
    }

    @Override
    public Schema outputSchema(Schema input) {
        return outputSchema;
    }
}
